package app.anima.core.model

/**
 * Assembles the mind prompt: persona ("I am this phone") + body state + top-N
 * soul facts + trailing dialogue window. Pure and deterministic; budget
 * trimming drops dialogue history before facts, facts before body state.
 *
 * v0.5 (Phase 1D): the whole prompt is written in the ROUTED language —
 * a native reply comes from a natively-worded prompt (MindVoice), never from
 * translating output. English wording is unchanged from v0.4.
 *
 * Budgets (ADR-005): per-backend, in characters with a conservative
 * 3 chars/token floor for mixed-language text. NANO: input cap ~4 000 tokens
 * → 9 000 chars. Local models (ekv2048-class artifacts): 2 048 tokens shared
 * by input AND output, minus a 320-token reply reserve → ~1 700 input tokens
 * → 5 100 chars, or whatever the model's registry spec says (ADR-017).
 */
object PromptBuilder {
    const val MAX_PROMPT_CHARS = 9000
    const val GEMMA_PROMPT_CHARS = 5100

    /**
     * ADR-011: a remote model has real context to spare — ~8k tokens at the
     * same conservative 3 chars/token floor. Still a WINDOW, not the soul:
     * the fact/dialogue caps below apply to every tier equally; cloud only
     * relaxes the char-trimming pressure.
     */
    const val CLOUD_PROMPT_CHARS = 24000
    const val MAX_FACTS = 24
    const val MAX_DIALOGUE_TURNS = 12

    fun budgetFor(
        tier: MindTier,
        spec: MindModelSpec? = null,
    ): Int =
        when (tier) {
            MindTier.CLOUD -> CLOUD_PROMPT_CHARS
            MindTier.GEMMA -> spec?.promptCharBudget ?: GEMMA_PROMPT_CHARS
            MindTier.NANO, MindTier.NONE -> MAX_PROMPT_CHARS
        }

    fun build(
        creatureName: String,
        state: BodyState,
        facts: List<SoulFact>,
        dialogue: List<ChatMessage>,
        userMessage: String,
        budgetChars: Int = MAX_PROMPT_CHARS,
        personality: Personality = Personality.Default,
        language: MindLanguage = MindLanguage.EN,
    ): MindPrompt {
        val system =
            buildString {
                appendLine(MindVoice.persona(creatureName, personality, language))
                appendLine()
                appendLine(MindVoice.bodyReport(state, language))
                val liveFacts = facts.filter { it.isLive }.take(MAX_FACTS)
                if (liveFacts.isNotEmpty()) {
                    appendLine()
                    appendLine(MindVoice.factsHeader(language))
                    liveFacts.forEach { appendLine("- [${it.category.wire}] ${it.text}") }
                }
            }.trim()

        val window = dialogue.takeLast(MAX_DIALOGUE_TURNS)
        val person = MindVoice.personLabel(language)
        val me = MindVoice.meLabel(language)
        val user =
            buildString {
                if (window.isNotEmpty()) {
                    appendLine(MindVoice.recentConversation(language))
                    window.forEach {
                        val who = if (it.role == ChatRole.USER) person else me
                        appendLine("$who: ${it.text}")
                    }
                    appendLine()
                }
                append("$person: ").append(userMessage.trim())
            }

        return trim(
            MindPrompt(system, user, language),
            state,
            creatureName,
            userMessage,
            budgetChars,
            personality,
            language,
        )
    }

    /** Facts survive; dialogue history is the first ballast overboard. */
    private fun trim(
        prompt: MindPrompt,
        state: BodyState,
        creatureName: String,
        userMessage: String,
        budgetChars: Int,
        personality: Personality,
        language: MindLanguage,
    ): MindPrompt {
        if (prompt.system.length + prompt.user.length <= budgetChars) return prompt
        val bareUser = "${MindVoice.personLabel(language)}: ${userMessage.trim()}"
        if (prompt.system.length + bareUser.length <= budgetChars) {
            return MindPrompt(prompt.system, bareUser, language)
        }
        val bareSystem =
            MindVoice.persona(creatureName, personality, language) +
                "\n\n" + MindVoice.bodyReport(state, language)
        return MindPrompt(bareSystem, bareUser.take((budgetChars - bareSystem.length).coerceAtLeast(0)), language)
    }
}
