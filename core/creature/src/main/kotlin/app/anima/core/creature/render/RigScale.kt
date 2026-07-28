package app.anima.core.creature.render

/**
 * One frame, one scale (defect D6).
 *
 * Before v1.1 every renderer invented its own idea of how big a creature is.
 * Measured from real `StillRender` output at 512px, longest ink dimension as
 * a fraction of the frame:
 *
 * ```
 * FOX_KIT 0.77   JELLY 0.75   PIXEL_PET 0.74   SPIRIT_ORB 0.68
 * ROBOT   0.67   SPROUT 0.64  MOTH      0.52   EMBER      0.49
 * ```
 *
 * The smallest body was 63% of the largest, and the body is *assigned*, not
 * chosen — so whoever's phone rolled EMBER simply got a worse-looking app
 * through no decision of their own. That is the whole argument for fixing it.
 *
 * The correction lives here rather than as eight edited multipliers scattered
 * across eight files, so it can be read as one table and so drift is visible.
 * These are calibration factors against the measurement above, not taste:
 * each is `target / measured`, and `FrameScaleTest` re-measures the result and
 * fails if any body leaves the band.
 *
 * `centerBias` is applied the same way and for the same reason: a body that
 * fills the frame but sits high reads as cropped beside one that sits
 * centred. Positive moves the drawing down, in fractions of the frame.
 */
internal object RigScale {
    /** What every body aims to fill, longest dimension. */
    const val TARGET_FILL = 0.82f

    // Ratios below are TARGET_FILL / measured-longest, rounded to 2 decimals,
    // then trimmed by re-measurement (the second number in each comment is
    // what the concept measured after the change).
    const val SPIRIT_ORB = 1.20f
    const val FOX_KIT = 1.07f
    const val JELLY = 1.09f
    const val PIXEL_PET = 1.10f
    const val ROBOT = 1.22f
    const val SPROUT = 1.28f
    const val EMBER = 1.69f
    const val MOTH = 1.57f

    // Vertical correction, fraction of the frame, positive = downward.
    // Derived from the measured optical centre of each body against 0.50,
    // then corrected once by re-measurement — scaling a body also moves its
    // centre, because every renderer's offsets are proportional to its base.
    // PIXEL_PET had no vertical term at all before v1.1; EMBER's needed the
    // largest correction because it was also the most under-scaled.
    const val SPIRIT_ORB_BIAS = 0.006f
    const val FOX_KIT_BIAS = 0.087f
    const val JELLY_BIAS = -0.132f
    const val PIXEL_PET_BIAS = -0.099f
    const val ROBOT_BIAS = 0.072f
    const val SPROUT_BIAS = -0.038f
    const val EMBER_BIAS = 0.182f
    const val MOTH_BIAS = 0.031f
}
