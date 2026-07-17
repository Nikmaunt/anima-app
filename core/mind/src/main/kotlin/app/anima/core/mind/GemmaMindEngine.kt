package app.anima.core.mind

import android.content.Context
import app.anima.core.model.FactCandidate
import app.anima.core.model.FactJson
import app.anima.core.model.MindEngine
import app.anima.core.model.MindEvent
import app.anima.core.model.MindFailure
import app.anima.core.model.MindModelLocator
import app.anima.core.model.MindPrompt
import app.anima.core.model.MindPrompts
import app.anima.core.model.MindStatus
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tier 2 (ADR-005): Gemma 3 1B through the MediaPipe LLM Inference API,
 * running a local model file owned by :core:model-delivery. Everything is
 * on-device; this module has no network access and never will.
 *
 * The loaded model holds ~0.7–1.5 GB — [releaseResources] drops it (called
 * when the chat leaves the screen); the next reply reloads lazily (seconds,
 * the UI shows "waking up" via the thinking pose).
 */
@Singleton
class GemmaMindEngine
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val locator: MindModelLocator,
    ) : MindEngine {
        private val lock = Mutex()
        private var loaded: Pair<String, LlmInference>? = null

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
            callbackFlow {
                val llm = engineOrNull()
                if (llm == null) {
                    trySend(MindEvent.Failed(MindFailure.LOST_THOUGHT))
                    close()
                    return@callbackFlow
                }
                val session = chatSession(llm)
                val full = StringBuilder()
                session.addQueryChunk(MindPrompts.combine(prompt))
                session.generateResponseAsync { partial, done ->
                    if (partial.isNotEmpty()) {
                        full.append(partial)
                        trySend(MindEvent.Chunk(partial))
                    }
                    if (done) {
                        trySend(MindEvent.Done(full.toString()))
                        close()
                    }
                }
                awaitClose { runCatching { session.close() } }
            }.buffer(Channel.UNLIMITED) // token callbacks must never drop chunks
                .flowOn(Dispatchers.IO)
                .catch { emit(MindEvent.Failed(MindFailure.LOST_THOUGHT)) }

        override suspend fun extractFactCandidates(
            userText: String,
            creatureText: String,
        ): List<FactCandidate> =
            runCatching {
                val llm = engineOrNull() ?: return emptyList()
                withContext(Dispatchers.IO) {
                    val session = extractionSession(llm)
                    try {
                        session.addQueryChunk(MindPrompts.extraction(userText, creatureText))
                        FactJson.parseCandidates(session.generateResponse())
                    } finally {
                        runCatching { session.close() }
                    }
                }
            }.getOrDefault(emptyList())

        /** Load (or reuse) the engine for the currently installed model file. */
        private suspend fun engineOrNull(): LlmInference? =
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
                        val options =
                            LlmInference.LlmInferenceOptions
                                .builder()
                                .setModelPath(model.path)
                                .setMaxTokens(MAX_TOKENS)
                                .build()
                        LlmInference.createFromOptions(context, options)
                    }.getOrNull()
                }?.also { loaded = model.path to it }
            }

        private fun chatSession(llm: LlmInference): LlmInferenceSession =
            LlmInferenceSession.createFromOptions(
                llm,
                LlmInferenceSession.LlmInferenceSessionOptions
                    .builder()
                    .setTopK(CHAT_TOP_K)
                    .setTemperature(CHAT_TEMPERATURE)
                    .build(),
            )

        private fun extractionSession(llm: LlmInference): LlmInferenceSession =
            LlmInferenceSession.createFromOptions(
                llm,
                LlmInferenceSession.LlmInferenceSessionOptions
                    .builder()
                    .setTopK(EXTRACTION_TOP_K)
                    .setTemperature(EXTRACTION_TEMPERATURE)
                    .build(),
            )

        companion object {
            /**
             * ADR-005: maxTokens is input+output combined; the ekv2048-class
             * artifact caps at 2048. PromptBuilder budgets input to leave
             * [REPLY_RESERVE_TOKENS] for the creature's short replies.
             */
            const val MAX_TOKENS = 2048
            const val REPLY_RESERVE_TOKENS = 320
            const val CHAT_TOP_K = 40
            const val CHAT_TEMPERATURE = 0.8f
            const val EXTRACTION_TOP_K = 5
            const val EXTRACTION_TEMPERATURE = 0.2f
        }
    }
