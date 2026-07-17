package app.anima.core.model

import kotlinx.coroutines.flow.Flow

/** Honest availability of the on-device mind. */
enum class MindStatus {
    /** Nano ready; conversations flow. */
    READY,

    /** Supported device, model not fetched yet (system-side download). */
    DOWNLOADABLE,

    /** System is fetching the model. */
    DOWNLOADING,

    /** Unsupported device or AICore missing: the mind sleeps. Never faked. */
    ASLEEP,
}

/** Everything the mind needs for one reply. Assembled by PromptBuilder. */
data class MindPrompt(
    val system: String,
    val user: String,
)

sealed interface MindEvent {
    /** A streamed chunk of the creature's reply. */
    data class Chunk(
        val text: String,
    ) : MindEvent

    /** Terminal: the full reply text. */
    data class Done(
        val fullText: String,
    ) : MindEvent

    /** Terminal: mapped, human-safe failure. */
    data class Failed(
        val reason: MindFailure,
    ) : MindEvent
}

enum class MindFailure {
    /** Per-app quota — "tired of thinking", retry later. */
    TIRED,

    /** Anything else; the reply just didn't come. */
    LOST_THOUGHT,
}

/**
 * The creature's voice, behind an interface. Implementations: Gemini Nano via
 * ML Kit GenAI Prompt API (:core:mind), and a deterministic fake for tests
 * and previews. Inference is strictly on-device and foreground-only.
 */
interface MindEngine {
    suspend fun status(): MindStatus

    /** Triggers the system-side model fetch when status == DOWNLOADABLE. */
    suspend fun requestDownload(): Boolean

    /**
     * Drop expensive backend resources (the GEMMA tier holds ~1 GB while
     * loaded). Safe to call anytime; the next reply reloads lazily.
     */
    suspend fun releaseResources() {}

    /** One streamed reply. Implementations must never fabricate when ASLEEP. */
    fun reply(prompt: MindPrompt): Flow<MindEvent>

    /**
     * Separate structured call: extract fact candidates from a finished
     * exchange. Candidates are suggestions only — persistence requires the
     * user's explicit confirmation in UI.
     */
    suspend fun extractFactCandidates(
        userText: String,
        creatureText: String,
    ): List<FactCandidate>
}
