package app.anima.core.mind

import app.anima.core.model.FactCandidate
import app.anima.core.model.FactJson
import app.anima.core.model.MindEngine
import app.anima.core.model.MindEvent
import app.anima.core.model.MindFailure
import app.anima.core.model.MindLanguage
import app.anima.core.model.MindPrompt
import app.anima.core.model.MindPrompts
import app.anima.core.model.MindStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini Nano via the ML Kit GenAI Prompt API (ADR-002, tier 1 of ADR-005).
 * Inference runs in the AICore system service — on-device, foreground-only,
 * no network from this app. On unsupported hardware checkStatus reports
 * UNAVAILABLE and this engine honestly says ASLEEP; it never fabricates.
 * Research 2026-07 (research-v2 §A.1): NO Galaxy S24 variant is on the
 * device list, permanently — on the owner's phone the GEMMA tier speaks.
 *
 * Compatibility note: SystemInstruction requires Nano V3+, so the prompt is
 * combined into a single TextPart — works on every Prompt-API device.
 */
@Singleton
class NanoMindEngine
    @Inject
    constructor() : MindEngine {
        private val model: GenerativeModel by lazy { Generation.getClient() }

        override suspend fun status(): MindStatus =
            runCatching {
                when (model.checkStatus()) {
                    FeatureStatus.AVAILABLE -> MindStatus.READY
                    FeatureStatus.DOWNLOADABLE -> MindStatus.DOWNLOADABLE
                    FeatureStatus.DOWNLOADING -> MindStatus.DOWNLOADING
                    else -> MindStatus.ASLEEP
                }
            }.getOrDefault(MindStatus.ASLEEP)

        override suspend fun requestDownload(): Boolean =
            runCatching {
                // The system streams DownloadStatus; completing without throwing
                // means the request was accepted (AICore owns the actual fetch).
                model.download().collect { }
                true
            }.getOrDefault(false)

        override fun reply(prompt: MindPrompt): Flow<MindEvent> =
            flow {
                val request = generateContentRequest(TextPart(MindPrompts.combine(prompt))) {}
                val full = StringBuilder()
                model.generateContentStream(request).collect { response ->
                    val chunk =
                        response.candidates
                            .firstOrNull()
                            ?.text
                            .orEmpty()
                    if (chunk.isNotEmpty()) {
                        full.append(chunk)
                        emit(MindEvent.Chunk(chunk))
                    }
                }
                emit(MindEvent.Done(full.toString()))
            }.catch { t ->
                emit(MindEvent.Failed(mapFailure(t)))
            }

        override suspend fun extractFactCandidates(
            userText: String,
            creatureText: String,
            language: MindLanguage,
        ): List<FactCandidate> =
            runCatching {
                val request =
                    generateContentRequest(TextPart(MindPrompts.extraction(userText, creatureText, language))) {}
                val text =
                    model
                        .generateContent(request)
                        .candidates
                        .firstOrNull()
                        ?.text
                        .orEmpty()
                FactJson.parseCandidates(text)
            }.getOrDefault(emptyList())

        private fun mapFailure(t: Throwable): MindFailure =
            when {
                t is GenAiException && (t.errorCode == BUSY || t.errorCode == BATTERY_QUOTA) -> MindFailure.TIRED
                else -> MindFailure.LOST_THOUGHT
            }

        private companion object {
            const val BUSY = 9
            const val BATTERY_QUOTA = 27
        }
    }
