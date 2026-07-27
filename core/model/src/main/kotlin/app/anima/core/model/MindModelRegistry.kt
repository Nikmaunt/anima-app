package app.anima.core.model

/**
 * How a model wants its prompt served (ADR-016). MediaPipe's LlmInference
 * takes raw text — the chat template is OUR job, per model family. PLAIN is
 * the v0.2–v0.4 Gemma-proven concatenation; CHATML is the Qwen family's
 * `<|im_start|>` framing.
 */
enum class PromptFormat { PLAIN, CHATML }

/** Redistribution terms that matter for the asset-pack slot (ADR-017). */
enum class ModelLicense {
    /** Clean redistribution; ship LICENSE/NOTICE and nothing else. */
    APACHE_2,

    /** Gemma Terms of Use: pack allowed WITH notice + flow-down obligations. */
    GEMMA_TOU,

    /** Unknown file the user brought — their license, their call. */
    UNKNOWN,
}

/**
 * Metadata for one known model artifact (ADR-017: model = data, not code).
 * The registry is how one engine serves many models: prompt format, stop
 * tokens, token budget and language support all come from here instead of
 * being baked into the engine.
 */
data class MindModelSpec(
    /** Stable id, also the S24-checklist / bake-off key. */
    val id: String,
    val displayName: String,
    /** Lowercase substrings matched against installed file names. */
    val fileNameHints: List<String>,
    /** Q4-artifact ballpark, for the Mind screen's honesty about space. */
    val approxSizeBytes: Long,
    /**
     * maxTokens handed to the engine (input+output combined). Conservative
     * 2048 unless a larger window is verified on-device via the bake-off
     * harness — a too-large value crashes load, a too-small one only trims.
     */
    val maxTokens: Int,
    val promptFormat: PromptFormat,
    /** Emitted-text terminators to trim from replies (streaming-safe). */
    val stopTokens: List<String>,
    /** Languages the model speaks at NATIVE quality (ADR-017 research). */
    val languages: Set<MindLanguage>,
    val license: ModelLicense,
) {
    fun speaks(language: MindLanguage): Boolean = language in languages

    /** Char budget for PromptBuilder: (maxTokens − reply reserve) × 3. */
    val promptCharBudget: Int
        get() = (maxTokens - REPLY_RESERVE_TOKENS) * CHARS_PER_TOKEN_FLOOR

    companion object {
        const val REPLY_RESERVE_TOKENS = 320
        const val CHARS_PER_TOKEN_FLOOR = 3
    }
}

/**
 * The known-model registry (ADR-017, docs/model-matrix.md). Additions are
 * data edits, not engine changes. Sizes/languages/licenses researched
 * 2026-07; anything only claimed upstream is conservative here.
 */
object MindModelRegistry {
    /**
     * All six product languages — a VENDOR CLAIM wherever it is still used
     * below, not a measurement. v1.0 measured two artifacts (ADR-023) and
     * narrowed both; the specs that keep `allSix` are ones whose weights
     * this project has never had on disk. Narrowing them by analogy would
     * repeat the mistake that made this correction necessary — so they stay
     * as claimed and stay listed as UNVERIFIED.
     */
    private val allSix = MindLanguage.entries.toSet()

    /** v0.2–v0.4 model: proven on the S24, but officially English-only. */
    val GEMMA3_1B =
        MindModelSpec(
            id = "gemma3-1b",
            displayName = "Gemma 3 1B",
            fileNameHints = listOf("gemma3-1b", "gemma-3-1b", "gemma3_1b"),
            approxSizeBytes = 555L * 1024 * 1024,
            maxTokens = 2048,
            promptFormat = PromptFormat.PLAIN,
            stopTokens = listOf("<end_of_turn>"),
            languages = setOf(MindLanguage.EN),
            license = ModelLicense.GEMMA_TOU,
        )

    /**
     * v0.5 pack default: the one candidate with ready .task/.litertlm
     * artifacts and Apache-2.0 terms.
     *
     * v1.0 CORRECTED THE LANGUAGE SET BY MEASUREMENT (ADR-023). It used to
     * be `allSix`, on the strength of the vendor's "29 trained languages".
     * Five product scenarios per language on the real artifact say
     * otherwise: Polish comes back with broken agreement and nonsense
     * ("Jestem Mika, twój ciało"), so PL is out and the router gives those
     * users English behind a visible badge instead of gibberish. RU/DE/ES/JA
     * stay in — they are understandable, though the creature sounds thinner
     * there than in English. Evidence: docs/lang-matrix-2026-07.md.
     */
    val QWEN25_15B =
        MindModelSpec(
            id = "qwen2.5-1.5b",
            displayName = "Qwen2.5 1.5B",
            fileNameHints = listOf("qwen2.5-1.5b", "qwen2_5-1.5b", "qwen2.5_1.5b"),
            approxSizeBytes = 1_100L * 1024 * 1024,
            maxTokens = 2048,
            promptFormat = PromptFormat.CHATML,
            stopTokens = listOf("<|im_end|>", "<|endoftext|>"),
            languages =
                setOf(
                    MindLanguage.EN,
                    MindLanguage.RU,
                    MindLanguage.DE,
                    MindLanguage.ES,
                    MindLanguage.JA,
                ),
            license = ModelLicense.APACHE_2,
        )

    /**
     * Smallest option, and v1.0 measured how small (ADR-023): on the same
     * six-language sweep its non-English output is not language, it is
     * word salad — "Твои мысли — это тихие мысли, ощущение, без тебя, без
     * связи, без дуэли", "Nienikny jest to, co czujesz" — and the Japanese
     * prompt got an English answer. English is claimed here because the
     * router needs somewhere to fall back to, NOT because 0.6B is good at
     * it. Treat this spec as a fast bench tier, not a recommendation.
     */
    val QWEN3_06B =
        MindModelSpec(
            id = "qwen3-0.6b",
            displayName = "Qwen3 0.6B",
            fileNameHints = listOf("qwen3-0.6b", "qwen3_0.6b", "qwen3-0_6b"),
            approxSizeBytes = 500L * 1024 * 1024,
            maxTokens = 2048,
            promptFormat = PromptFormat.CHATML,
            stopTokens = listOf("<|im_end|>", "<|endoftext|>"),
            languages = setOf(MindLanguage.EN),
            license = ModelLicense.APACHE_2,
        )

    /** Target default once a .litertlm conversion is verified (ADR-017). */
    val QWEN3_17B =
        MindModelSpec(
            id = "qwen3-1.7b",
            displayName = "Qwen3 1.7B",
            fileNameHints = listOf("qwen3-1.7b", "qwen3_1.7b", "qwen3-1_7b"),
            approxSizeBytes = 1_200L * 1024 * 1024,
            maxTokens = 2048,
            promptFormat = PromptFormat.CHATML,
            stopTokens = listOf("<|im_end|>", "<|endoftext|>"),
            languages = allSix,
            license = ModelLicense.APACHE_2,
        )

    /** Premium alternative for roomy devices: Apache-2.0 since Gemma 4. */
    val GEMMA4_E2B =
        MindModelSpec(
            id = "gemma4-e2b",
            displayName = "Gemma 4 E2B",
            fileNameHints = listOf("gemma-4-e2b", "gemma4-e2b", "gemma4_e2b"),
            approxSizeBytes = 2_640L * 1024 * 1024,
            maxTokens = 2048,
            promptFormat = PromptFormat.PLAIN,
            stopTokens = listOf("<end_of_turn>"),
            languages = allSix,
            license = ModelLicense.APACHE_2,
        )

    /** 140-language sibling still under the Gemma ToU. */
    val GEMMA3N_E2B =
        MindModelSpec(
            id = "gemma3n-e2b",
            displayName = "Gemma 3n E2B",
            fileNameHints = listOf("gemma-3n-e2b", "gemma3n-e2b", "gemma3n_e2b"),
            approxSizeBytes = 3_000L * 1024 * 1024,
            maxTokens = 2048,
            promptFormat = PromptFormat.PLAIN,
            stopTokens = listOf("<end_of_turn>"),
            languages = allSix,
            license = ModelLicense.GEMMA_TOU,
        )

    val all: List<MindModelSpec> =
        listOf(QWEN25_15B, QWEN3_17B, QWEN3_06B, GEMMA4_E2B, GEMMA3N_E2B, GEMMA3_1B)

    /** What the mind-pack slot is built for in v0.5 (ADR-017). */
    val packDefault: MindModelSpec = QWEN25_15B

    /** Match an installed file to a known spec by name, else null. */
    fun match(fileName: String): MindModelSpec? {
        val normalized = fileName.lowercase()
        return all.firstOrNull { spec -> spec.fileNameHints.any { normalized.contains(it) } }
    }

    /**
     * Spec for any installed file. Unknown files get an honest fallback:
     * Gemma-era PLAIN format and English-only routing — never a guessed
     * language claim.
     */
    fun specFor(model: InstalledMindModel): MindModelSpec =
        match(model.fileName) ?: unknown(model.fileName, model.sizeBytes)

    fun unknown(
        fileName: String,
        sizeBytes: Long,
    ): MindModelSpec =
        MindModelSpec(
            id = "unknown",
            displayName = fileName,
            fileNameHints = emptyList(),
            approxSizeBytes = sizeBytes,
            maxTokens = 2048,
            promptFormat = PromptFormat.PLAIN,
            stopTokens = emptyList(),
            languages = setOf(MindLanguage.EN),
            license = ModelLicense.UNKNOWN,
        )
}
