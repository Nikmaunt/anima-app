package app.anima.core.model

/**
 * The six languages the product ships in (v0.5, Phase 1D). The creature
 * speaks the interface language when the active mind genuinely supports it —
 * never through a translation layer, never faked. Everything else falls back
 * to English behind an honest badge (ADR-017).
 */
enum class MindLanguage(
    /** BCP-47 primary subtag, lowercase. */
    val tag: String,
    /** The language's own name for itself — shown untranslated in UI. */
    val selfName: String,
) {
    EN("en", "English"),
    RU("ru", "Русский"),
    PL("pl", "Polski"),
    DE("de", "Deutsch"),
    ES("es", "Español"),
    JA("ja", "日本語"),
    ;

    companion object {
        /**
         * Maps any BCP-47 tag ("ru", "ru-RU", "es-419") to a supported
         * language; unknown locales read as English — the app's resources do
         * the same via the standard res fallback chain, so mind and UI agree.
         */
        fun fromTag(tag: String): MindLanguage {
            val primary = tag.substringBefore('-').substringBefore('_').lowercase()
            return entries.firstOrNull { it.tag == primary } ?: EN
        }
    }
}
