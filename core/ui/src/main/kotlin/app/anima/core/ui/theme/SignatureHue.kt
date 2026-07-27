package app.anima.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The app's accent belongs to the creature, not to the app
 * (docs/design-system-v11.md §2). Every phone gets its own palette because
 * every phone got its own creature.
 *
 * The hue is not invented here: it is the same base hue each renderer already
 * uses for the body (the per-concept renderers in core:creature), lifted to the
 * theme so the chrome can agree with the body instead of ignoring it. On top
 * of it sits `CreatureGenome.hueShiftDeg` (±18°, derived from the device seed,
 * core/model/.../CreatureSeed.kt:59) and any unlocked palette shift.
 *
 * Only the HUE is free. Saturation and lightness are fixed below, which is
 * what makes this safe: contrast cannot depend on which creature you got.
 * `SignatureHueContrastTest` walks all 360 degrees and asserts it.
 */
object SignatureHue {
    /**
     * Concept wire name -> base hue in degrees. Keyed by the enum name so
     * core:ui does not have to depend on core:model's CreatureConcept.
     */
    private val BY_CONCEPT =
        mapOf(
            "SPIRIT_ORB" to 190f,
            "FOX_KIT" to 24f,
            "JELLY" to 280f,
            // the eye glow, not the shell: the shell is nearly grey
            "ROBOT" to 165f,
            "PIXEL_PET" to 140f,
            "SPROUT" to 130f,
            "EMBER" to 28f,
            "MOTH" to 265f,
        )

    /** Falls back to the orb's hue for an unknown name; never throws. */
    fun of(conceptName: String): Float = BY_CONCEPT[conceptName] ?: 190f

    fun resolve(
        conceptName: String,
        genomeHueShiftDeg: Float,
        paletteShiftDeg: Float = 0f,
    ): Float = normalize(of(conceptName) + genomeHueShiftDeg + paletteShiftDeg)

    fun normalize(deg: Float): Float {
        var h = deg % 360f
        if (h < 0f) h += 360f
        return h
    }

    // --- Fixed S/L. Free hue, pinned everything else. ---
    const val ACCENT_S = 0.45f

    /**
     * 0.72, not the 0.70 first drafted: at 0.70 the worst hue put
     * background-on-accent (the PillButton label) at 4.49 against the 4.5 AA
     * floor. Caught by SignatureHueContrastTest walking all 360 degrees,
     * which is the whole reason that test sweeps rather than samples.
     */
    const val ACCENT_L_NIGHT = 0.72f
    const val ACCENT_L_PAPER = 0.30f

    const val BACKGROUND_S = 0.22f
    const val BACKGROUND_L_NIGHT = 0.055f
    const val BACKGROUND_L_PAPER = 0.96f

    const val SURFACE_S = 0.20f
    const val SURFACE_L_NIGHT = 0.10f
    const val SURFACE_L_PAPER = 0.99f

    const val SURFACE_HIGH_S = 0.18f
    const val SURFACE_HIGH_L_NIGHT = 0.145f
    const val SURFACE_HIGH_L_PAPER = 1.0f

    fun accent(
        hueDeg: Float,
        night: Boolean,
    ): Color = Color.hsl(normalize(hueDeg), ACCENT_S, if (night) ACCENT_L_NIGHT else ACCENT_L_PAPER)

    fun accentSoft(
        hueDeg: Float,
        night: Boolean,
    ): Color =
        Color.hsl(
            normalize(hueDeg),
            ACCENT_S * 0.5f,
            if (night) 0.20f else 0.90f,
        )

    fun background(
        hueDeg: Float,
        night: Boolean,
    ): Color =
        Color.hsl(
            normalize(hueDeg),
            BACKGROUND_S,
            if (night) BACKGROUND_L_NIGHT else BACKGROUND_L_PAPER,
        )

    fun surface(
        hueDeg: Float,
        night: Boolean,
    ): Color =
        Color.hsl(
            normalize(hueDeg),
            SURFACE_S,
            if (night) SURFACE_L_NIGHT else SURFACE_L_PAPER,
        )

    fun surfaceHigh(
        hueDeg: Float,
        night: Boolean,
    ): Color =
        Color.hsl(
            normalize(hueDeg),
            SURFACE_HIGH_S,
            if (night) SURFACE_HIGH_L_NIGHT else SURFACE_HIGH_L_PAPER,
        )

    fun outline(
        hueDeg: Float,
        night: Boolean,
    ): Color =
        Color.hsl(
            normalize(hueDeg),
            0.16f,
            if (night) 0.28f else 0.82f,
        )

    /**
     * Text stays neutral on purpose: it is the one thing that must not move
     * with the creature, and it is why the existing contrast proof still
     * holds.
     */
    fun text(night: Boolean): Color = Color(if (night) AnimaPalette.NightText else AnimaPalette.PaperText)

    fun textDim(night: Boolean): Color = Color(if (night) AnimaPalette.NightTextDim else AnimaPalette.PaperTextDim)

    /** The whole AnimaColors set for one creature hue. */
    fun colors(
        hueDeg: Float,
        night: Boolean,
    ): AnimaColors =
        AnimaColors(
            background = background(hueDeg, night),
            surface = surface(hueDeg, night),
            surfaceHigh = surfaceHigh(hueDeg, night),
            text = text(night),
            textDim = textDim(night),
            outline = outline(hueDeg, night),
            accent = accent(hueDeg, night),
            accentSoft = accentSoft(hueDeg, night),
            warn = Color(if (night) AnimaPalette.NightWarn else AnimaPalette.PaperWarn),
            danger = Color(if (night) AnimaPalette.NightDanger else AnimaPalette.PaperDanger),
            isNight = night,
        )
}
