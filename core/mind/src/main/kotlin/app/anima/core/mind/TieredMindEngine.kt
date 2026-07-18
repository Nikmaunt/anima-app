package app.anima.core.mind

import app.anima.core.model.CloudMindBackend
import app.anima.core.model.FactCandidate
import app.anima.core.model.LocalMindEngine
import app.anima.core.model.MindEngine
import app.anima.core.model.MindEvent
import app.anima.core.model.MindFailure
import app.anima.core.model.MindInventory
import app.anima.core.model.MindLanguage
import app.anima.core.model.MindModelLocator
import app.anima.core.model.MindPrompt
import app.anima.core.model.MindSnapshot
import app.anima.core.model.MindStatus
import app.anima.core.model.MindTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ADR-005 tier selection, amended by ADR-011: CLOUD strictly when the user
 * opted in AND the backend reports itself ready (configured + online) —
 * otherwise NANO where the Prompt API answers, GEMMA where a local model is
 * installed, honest sleep last. Probed per call — the probe is a cheap
 * binder call / StateFlow read, and staying stateless means installing or
 * deleting a model (or going offline mid-session) needs no invalidation
 * choreography: offline cloud simply falls through to the local tiers.
 */
@Singleton
class TieredMindEngine
    @Inject
    constructor(
        private val cloud: CloudMindBackend,
        private val nano: NanoMindEngine,
        private val gemma: GemmaMindEngine,
        private val locator: MindModelLocator,
    ) : MindEngine,
        MindInventory {
        private suspend fun active(includeCloud: Boolean = true): Pair<MindTier, MindEngine>? =
            when (
                TierSelection.pick(
                    cloudReady = includeCloud && cloud.status() == MindStatus.READY,
                    nanoAwake = nano.status() != MindStatus.ASLEEP,
                    hasLocalModel = locator.installed.value != null,
                )
            ) {
                MindTier.CLOUD -> MindTier.CLOUD to cloud
                MindTier.NANO -> MindTier.NANO to nano
                MindTier.GEMMA -> MindTier.GEMMA to gemma
                else -> null
            }

        private fun replyVia(
            prompt: MindPrompt,
            includeCloud: Boolean,
        ): Flow<MindEvent> =
            flow {
                when (val backend = active(includeCloud)) {
                    null -> emit(MindEvent.Failed(MindFailure.LOST_THOUGHT))
                    else -> emitAll(backend.second.reply(prompt))
                }
            }

        override suspend fun status(): MindStatus =
            when (val backend = active()) {
                null -> MindStatus.ASLEEP
                else -> backend.second.status()
            }

        override suspend fun requestDownload(): Boolean = active()?.second?.requestDownload() ?: false

        override suspend fun releaseResources() = gemma.releaseResources()

        override fun reply(prompt: MindPrompt): Flow<MindEvent> = replyVia(prompt, includeCloud = true)

        override suspend fun extractFactCandidates(
            userText: String,
            creatureText: String,
            language: MindLanguage,
        ): List<FactCandidate> = active()?.second?.extractFactCandidates(userText, creatureText, language).orEmpty()

        override suspend fun snapshot(): MindSnapshot =
            MindSnapshot(
                activeTier = active()?.first ?: MindTier.NONE,
                nano = nano.status(),
                gemmaModel = locator.installed.value,
                cloud = cloud.currentConfig(),
            )

        /**
         * ADR-011 data-scope guard (audit-v03 F1): the digest and any other
         * body-adjacent summary go through this view, which skips CLOUD even
         * when the user has it enabled and online.
         */
        val localOnly: LocalMindEngine =
            object : LocalMindEngine {
                override suspend fun status(): MindStatus =
                    when (val backend = active(includeCloud = false)) {
                        null -> MindStatus.ASLEEP
                        else -> backend.second.status()
                    }

                override suspend fun requestDownload(): Boolean =
                    active(includeCloud = false)?.second?.requestDownload() ?: false

                override suspend fun releaseResources() = gemma.releaseResources()

                override fun reply(prompt: MindPrompt): Flow<MindEvent> = replyVia(prompt, includeCloud = false)

                override suspend fun extractFactCandidates(
                    userText: String,
                    creatureText: String,
                    language: MindLanguage,
                ): List<FactCandidate> =
                    active(includeCloud = false)
                        ?.second
                        ?.extractFactCandidates(userText, creatureText, language)
                        .orEmpty()
            }
    }

/**
 * The tier ladder as a pure function (unit-tested): CLOUD strictly by
 * opt-in+ready AND only when the surface allows it, NANO where the Prompt
 * API answers, GEMMA where a local model exists, honest sleep last.
 */
internal object TierSelection {
    fun pick(
        cloudReady: Boolean,
        nanoAwake: Boolean,
        hasLocalModel: Boolean,
    ): MindTier =
        when {
            cloudReady -> MindTier.CLOUD
            nanoAwake -> MindTier.NANO
            hasLocalModel -> MindTier.GEMMA
            else -> MindTier.NONE
        }
}
