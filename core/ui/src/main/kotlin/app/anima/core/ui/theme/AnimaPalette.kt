package app.anima.core.ui.theme

/**
 * Anima's palette as plain ARGB longs — testable on the JVM (WCAG contrast
 * test lives beside these tokens). Dark is the primary theme: a creature that
 * lives inside the phone lives in the dark; light is warm paper, not white.
 *
 * Not a Material seed palette — chosen by hand around a deep indigo night sky
 * with a single aurora accent, so the creature's own colors always win the
 * screen.
 */
object AnimaPalette {
    // --- Night (primary) ---
    const val NightDeep = 0xFF0B0E1AL
    const val NightSurface = 0xFF141829L
    const val NightSurfaceHigh = 0xFF1D2338L
    const val NightText = 0xFFEDEFF7L
    const val NightTextDim = 0xFFA8AEC6L
    const val NightOutline = 0xFF3A4160L
    const val NightAccent = 0xFF8FD3C7L // aurora mint
    const val NightAccentDeep = 0xFF223C3EL
    const val NightWarn = 0xFFE8B84FL
    const val NightDanger = 0xFFE8807FL

    // --- Paper (light) ---
    const val PaperDeep = 0xFFF6F2EAL
    const val PaperSurface = 0xFFFFFDF8L
    const val PaperSurfaceHigh = 0xFFFFFFFFL
    const val PaperText = 0xFF23283EL
    const val PaperTextDim = 0xFF5A6079L
    const val PaperOutline = 0xFFD8D2C4L
    const val PaperAccent = 0xFF1F7A6BL // deep aurora on paper
    const val PaperAccentSoft = 0xFFDCEFE9L
    const val PaperWarn = 0xFF9A6B00L
    const val PaperDanger = 0xFFB3403FL
}
