package app.anima.core.mind

import app.anima.core.model.FactCandidate
import app.anima.core.model.FactJson
import app.anima.core.model.MindEngine
import app.anima.core.model.MindEvent
import app.anima.core.model.MindFailure
import app.anima.core.model.MindLanguage
import app.anima.core.model.MindModelLocator
import app.anima.core.model.MindModelRegistry
import app.anima.core.model.MindModelSpec
import app.anima.core.model.MindPrompt
import app.anima.core.model.MindPrompts
import app.anima.core.model.MindStatus
import app.anima.core.model.StreamTrimmer
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ADR-020: the LOCAL tier on the LiteRT-LM runtime — the designated
 * tasks-genai successor — as the OPTIONAL second implementation behind the
 * same [MindEngine] interface. Debug builds only, behind the developer flag
 * (default OFF); [GemmaMindEngine] remains the shipped runtime.
 *
 * CPU is the one and only backend, BY LAW, not by default: the Samsung GPU
 * issue cluster (freshness-2026-07 §A: #2292 S24U init crash without
 * fallback, #2114 Xclipse, #2611, #2211) is open, and sampler params are
 * silently ignored on GPU/NPU (#2080). LiteRtLmCpuOnlyTest pins this file to
 * `Backend.CPU` and bans the other backends' spellings outright.
 *
 * Unlike tasks-genai (raw text in → our format layer frames CHATML/PLAIN),
 * LiteRT-LM applies the model's own chat template from the .litertlm file:
 * the system text rides ConversationConfig, the user turn goes in plain.
 * [StreamTrimmer] still runs over the reply as a defensive net (same spec
 * stop-tokens as the Gemma path). Replies are non-streamed for now — the
 * chunk semantics of sendMessageAsync are pinned down by the JVM smoke
 * first (tools/litertlm-smoke); the UI already handles single-chunk Done.
 */
@Singleton
class LiteRtLmMindEngine
    @Inject
    constructor(
        private val locator: MindModelLocator,
    ) : MindEngine {
        private val lock = Mutex()
        private var loaded: Pair<String, Engine>? = null

        override suspend fun status(): MindStatus =
            if (locator.installed.value != null) MindStatus.READY else MindStatus.ASLEEP

        /** Delivery is the Mind screen's job, not the engine's. */
        override suspend fun requestDownload(): Boolean = false

        override suspend fun releaseResources() {
            lock.withLock {
                runCatching { loaded?.second?.close() }
                loaded = null
            }
        }

        override fun reply(prompt: MindPrompt): Flow<MindEvent> =
            flow {
                val engine = engineOrNull()
                if (engine == null) {
                    emit(MindEvent.Failed(MindFailure.LOST_THOUGHT))
                    return@flow
                }
                val spec = currentSpec()
                val trimmer = StreamTrimmer(spec.stopTokens)
                val text =
                    engine
                        .createConversation(
                            ConversationConfig(
                                systemInstruction = Contents.of(prompt.system),
                                samplerConfig = chatSampler(),
                            ),
                        ).use { conversation ->
                            conversation.sendMessage(prompt.user).text()
                        }
                val safe = (trimmer.feed(text) + trimmer.flush()).trimEnd()
                if (safe.isEmpty()) {
                    emit(MindEvent.Failed(MindFailure.LOST_THOUGHT))
                } else {
                    emit(MindEvent.Chunk(safe))
                    emit(MindEvent.Done(safe))
                }
            }.flowOn(Dispatchers.IO)
                .catch { emit(MindEvent.Failed(MindFailure.LOST_THOUGHT)) }

        override suspend fun extractFactCandidates(
            userText: String,
            creatureText: String,
            language: MindLanguage,
        ): List<FactCandidate> =
            runCatching {
                val engine = engineOrNull() ?: return emptyList()
                withContext(Dispatchers.IO) {
                    engine
                        .createConversation(
                            ConversationConfig(samplerConfig = extractionSampler()),
                        ).use { conversation ->
                            FactJson.parseCandidates(
                                conversation
                                    .sendMessage(MindPrompts.extraction(userText, creatureText, language))
                                    .text(),
                            )
                        }
                }
            }.getOrDefault(emptyList())

        /** Registry spec of the installed model; honest fallback otherwise. */
        private fun currentSpec(): MindModelSpec =
            locator.installed.value
                ?.let(MindModelRegistry::specFor)
                ?: MindModelRegistry.GEMMA3_1B

        /** Load (or reuse) the engine for the currently installed model file. */
        private suspend fun engineOrNull(): Engine? =
            lock.withLock {
                val model = locator.installed.value
                if (model == null) {
                    runCatching { loaded?.second?.close() }
                    loaded = null
                    return@withLock null
                }
                loaded?.takeIf { it.first == model.path }?.let { return@withLock it.second }
                runCatching { loaded?.second?.close() }
                loaded = null
                withContext(Dispatchers.IO) {
                    runCatching {
                        Engine(
                            EngineConfig(
                                modelPath = model.path,
                                // ADR-020: CPU only — see the class doc.
                                backend = Backend.CPU(),
                                maxNumTokens = MindModelRegistry.specFor(model).maxTokens,
                            ),
                        ).also { it.initialize() }
                    }.getOrNull()
                }?.also { loaded = model.path to it }
            }

        private fun chatSampler(): SamplerConfig =
            SamplerConfig(
                topK = GemmaMindEngine.CHAT_TOP_K,
                topP = TOP_P,
                temperature = GemmaMindEngine.CHAT_TEMPERATURE.toDouble(),
                seed = SEED,
            )

        private fun extractionSampler(): SamplerConfig =
            SamplerConfig(
                topK = GemmaMindEngine.EXTRACTION_TOP_K,
                topP = TOP_P,
                temperature = GemmaMindEngine.EXTRACTION_TEMPERATURE.toDouble(),
                seed = SEED,
            )

        private companion object {
            /** Same nucleus default the C++ engine documents. */
            const val TOP_P = 0.95

            /** Fixed seed: deterministic debug comparisons against Gemma. */
            const val SEED = 0
        }
    }

/** The text parts of a reply, concatenated; ignores tool/audio content. */
private fun Message.text(): String =
    contents.contents
        .filterIsInstance<Content.Text>()
        .joinToString(separator = "") { it.text }
