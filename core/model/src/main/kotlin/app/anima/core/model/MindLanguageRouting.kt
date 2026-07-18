package app.anima.core.model

/**
 * Phase 1D: the honest language choice, as a pure function. The interface
 * language wins when the ACTIVE mind genuinely speaks it; otherwise the
 * creature answers in English behind a visible badge — it never garbles a
 * language it doesn't know, and it never silently switches tiers to chase
 * one. (Tier selection stays ADR-005/011's job; routing only picks words.)
 */
object MindLanguageRouting {
    enum class Mode {
        /** The creature speaks the interface language. */
        NATIVE,

        /** Model can't speak the UI language — English, with a UI badge. */
        ENGLISH_FALLBACK,
    }

    data class Decision(
        val language: MindLanguage,
        val mode: Mode,
    ) {
        val showBadge: Boolean get() = mode == Mode.ENGLISH_FALLBACK
    }

    fun decide(
        uiLanguage: MindLanguage,
        tier: MindTier,
        localSpec: MindModelSpec?,
    ): Decision =
        when (tier) {
            // Cloud presets are frontier-class multilingual models.
            MindTier.CLOUD -> Decision(uiLanguage, Mode.NATIVE)

            MindTier.GEMMA ->
                if (localSpec?.speaks(uiLanguage) == true) {
                    Decision(uiLanguage, Mode.NATIVE)
                } else {
                    fallback(uiLanguage)
                }

            // Gemini Nano's non-EN quality is device/version-dependent and
            // unverified — honesty over optimism (S24 checklist item).
            MindTier.NANO ->
                if (uiLanguage == MindLanguage.EN) {
                    Decision(MindLanguage.EN, Mode.NATIVE)
                } else {
                    fallback(uiLanguage)
                }

            // Asleep: nothing speaks; scripted UI copy is localized anyway.
            MindTier.NONE -> Decision(uiLanguage, Mode.NATIVE)
        }

    private fun fallback(uiLanguage: MindLanguage): Decision =
        if (uiLanguage == MindLanguage.EN) {
            Decision(MindLanguage.EN, Mode.NATIVE)
        } else {
            Decision(MindLanguage.EN, Mode.ENGLISH_FALLBACK)
        }
}
