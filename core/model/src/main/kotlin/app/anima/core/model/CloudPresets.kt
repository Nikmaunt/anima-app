package app.anima.core.model

/** How a provider's key can be checked cheaply (research-v5 §C). */
enum class KeyProbe {
    /** `GET {base}/models` with the Bearer key → 200/401. */
    MODELS,

    /** OpenRouter's `/models` is public; its key endpoint is `GET /key`. */
    OPENROUTER_KEY,

    /**
     * No verified list endpoint through the compat layer (Anthropic) — a
     * 1-token `POST chat/completions` is the honest cheap probe.
     */
    COMPLETIONS_PING,
}

/**
 * A BYOK provider preset (Phase 1E): a name, a base URL and honest defaults.
 * Presets are CONVENIENCE, not endorsement — free tiers are volatile
 * (research-v5 §C), so the model field is a prefill the owner confirms, and
 * the key stays theirs. Everything still flows through the one ADR-011
 * OpenAI-compatible transport; a preset adds zero code paths.
 */
data class CloudPreset(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    /** Prefilled model id — the owner can overwrite it. */
    val defaultModel: String,
    /** Expected key prefix for a soft UI hint (never a hard gate). */
    val keyPrefix: String?,
    /** Whether a no-card free tier existed as of 2026-07 (may change). */
    val freeTier: Boolean,
    val keyProbe: KeyProbe,
) {
    fun keyLooksRight(key: String): Boolean = keyPrefix == null || key.startsWith(keyPrefix)
}

object CloudPresets {
    val OPENROUTER =
        CloudPreset(
            id = "openrouter",
            displayName = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/v1",
            defaultModel = "qwen/qwen3-next-80b-a3b-instruct:free",
            keyPrefix = "sk-or-",
            freeTier = true,
            keyProbe = KeyProbe.OPENROUTER_KEY,
        )

    val GROQ =
        CloudPreset(
            id = "groq",
            displayName = "Groq",
            baseUrl = "https://api.groq.com/openai/v1",
            defaultModel = "openai/gpt-oss-20b",
            keyPrefix = "gsk_",
            freeTier = true,
            keyProbe = KeyProbe.MODELS,
        )

    val GEMINI =
        CloudPreset(
            id = "gemini",
            displayName = "Google AI Studio",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
            defaultModel = "gemini-3-flash",
            keyPrefix = "AIza",
            freeTier = true,
            keyProbe = KeyProbe.MODELS,
        )

    val MISTRAL =
        CloudPreset(
            id = "mistral",
            displayName = "Mistral",
            baseUrl = "https://api.mistral.ai/v1",
            defaultModel = "mistral-small-latest",
            keyPrefix = null,
            freeTier = true,
            keyProbe = KeyProbe.MODELS,
        )

    val CEREBRAS =
        CloudPreset(
            id = "cerebras",
            displayName = "Cerebras",
            baseUrl = "https://api.cerebras.ai/v1",
            defaultModel = "",
            keyPrefix = "csk-",
            freeTier = true,
            keyProbe = KeyProbe.MODELS,
        )

    val OPENAI =
        CloudPreset(
            id = "openai",
            displayName = "OpenAI",
            baseUrl = "https://api.openai.com/v1",
            defaultModel = "gpt-5-mini",
            keyPrefix = "sk-",
            freeTier = false,
            keyProbe = KeyProbe.MODELS,
        )

    val ANTHROPIC =
        CloudPreset(
            id = "anthropic",
            displayName = "Anthropic",
            baseUrl = "https://api.anthropic.com/v1",
            defaultModel = "claude-haiku-4-5",
            keyPrefix = "sk-ant-",
            freeTier = false,
            keyProbe = KeyProbe.COMPLETIONS_PING,
        )

    val all: List<CloudPreset> =
        listOf(OPENROUTER, GROQ, GEMINI, MISTRAL, CEREBRAS, OPENAI, ANTHROPIC)

    /** Preset whose base URL matches the stored endpoint, if any. */
    fun match(baseUrl: String): CloudPreset? {
        val normalized = baseUrl.trim().trimEnd('/')
        return all.firstOrNull { it.baseUrl.trimEnd('/') == normalized }
    }

    /** Probe for a stored endpoint: preset's own, or the generic default. */
    fun probeFor(baseUrl: String): KeyProbe = match(baseUrl)?.keyProbe ?: KeyProbe.MODELS
}
