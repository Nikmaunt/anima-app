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

/**
 * Everything the mind needs for one reply. Assembled by PromptBuilder.
 * [language] is the ROUTED reply language (Phase 1D) — backends use it only
 * for format framing (e.g. the PLAIN-format "Me:" cue); the prompt text
 * itself is already worded in that language.
 */
data class MindPrompt(
    val system: String,
    val user: String,
    val language: MindLanguage = MindLanguage.EN,
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
     * user's explicit confirmation in UI. [language] is the user's language:
     * extracted fact text must read natively (Phase 1D).
     */
    suspend fun extractFactCandidates(
        userText: String,
        creatureText: String,
        language: MindLanguage = MindLanguage.EN,
    ): List<FactCandidate>
}

/**
 * ADR-011 data-scope guard (audit-v03 F1): a view of the mind that can never
 * select the cloud tier. Surfaces that feed body-adjacent data into prompts —
 * the notification digest is the canonical case — must depend on THIS type,
 * so "the cloud never sees notification data" is enforced by the DI graph's
 * types, not by callers remembering a rule.
 */
interface LocalMindEngine : MindEngine
