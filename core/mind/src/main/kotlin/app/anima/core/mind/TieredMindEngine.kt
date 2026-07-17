package app.anima.core.mind

import app.anima.core.model.FactCandidate
import app.anima.core.model.MindEngine
import app.anima.core.model.MindEvent
import app.anima.core.model.MindFailure
import app.anima.core.model.MindInventory
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
 * ADR-005 tier selection: NANO where the Prompt API answers, GEMMA where a
 * local model is installed, honest sleep otherwise. Probed per call — the
 * probe is a cheap binder call / StateFlow read, and staying stateless means
 * installing or deleting a model needs no invalidation choreography.
 */
@Singleton
class TieredMindEngine
    @Inject
    constructor(
        private val nano: NanoMindEngine,
        private val gemma: GemmaMindEngine,
        private val locator: MindModelLocator,
    ) : MindEngine,
        MindInventory {
        private suspend fun active(): Pair<MindTier, MindEngine>? {
            val nanoStatus = nano.status()
            if (nanoStatus != MindStatus.ASLEEP) return MindTier.NANO to nano
            if (locator.installed.value != null) return MindTier.GEMMA to gemma
            return null
        }

        override suspend fun status(): MindStatus =
            when (val backend = active()) {
                null -> MindStatus.ASLEEP
                else -> backend.second.status()
            }

        override suspend fun requestDownload(): Boolean = active()?.second?.requestDownload() ?: false

        override suspend fun releaseResources() = gemma.releaseResources()

        override fun reply(prompt: MindPrompt): Flow<MindEvent> =
            flow {
                when (val backend = active()) {
                    null -> emit(MindEvent.Failed(MindFailure.LOST_THOUGHT))
                    else -> emitAll(backend.second.reply(prompt))
                }
            }

        override suspend fun extractFactCandidates(
            userText: String,
            creatureText: String,
        ): List<FactCandidate> = active()?.second?.extractFactCandidates(userText, creatureText).orEmpty()

        override suspend fun snapshot(): MindSnapshot =
            MindSnapshot(
                activeTier = active()?.first ?: MindTier.NONE,
                nano = nano.status(),
                gemmaModel = locator.installed.value,
            )
    }
