package app.anima.core.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntOffset

/**
 * Motion tokens, transcribed by hand from Material 3's own numbers
 * (`androidx/compose/material3/tokens/ExpressiveMotionTokens.kt`, branch
 * `androidx-main`) — see docs/research-v11-design.md §1.3. We copy the values
 * rather than take the library: material3 on our pinned BOM is 1.3.2, where
 * `MotionScheme` does not exist yet.
 *
 * Scheme is Expressive, not Standard: this app is a creature, not a table.
 *
 * The one rule the numbers themselves enforce — every effects token has
 * damping exactly 1.0 in both M3 schemes, so colour and alpha never overshoot.
 * Spatial may overshoot; that is what makes it feel alive.
 *
 * These describe *chrome* motion (screens appearing, taps responding, the
 * creature travelling between screens). The creature's own body runs on
 * core/creature's engine, whose springs use a different parameterisation and
 * are not interchangeable with these.
 */
object AnimaMotion {
    // --- Expressive spatial: position, size, rotation. Overshoot allowed. ---
    const val SPATIAL_FAST_DAMPING = 0.6f
    const val SPATIAL_FAST_STIFFNESS = 800f
    const val SPATIAL_DEFAULT_DAMPING = 0.8f
    const val SPATIAL_DEFAULT_STIFFNESS = 380f
    const val SPATIAL_SLOW_DAMPING = 0.8f
    const val SPATIAL_SLOW_STIFFNESS = 200f

    // --- Expressive effects: colour, alpha. Damping 1.0 = never overshoots. ---
    const val EFFECTS_FAST_DAMPING = 1.0f
    const val EFFECTS_FAST_STIFFNESS = 3800f
    const val EFFECTS_DEFAULT_DAMPING = 1.0f
    const val EFFECTS_DEFAULT_STIFFNESS = 1600f
    const val EFFECTS_SLOW_DAMPING = 1.0f
    const val EFFECTS_SLOW_STIFFNESS = 800f

    /** Finger response: a press, a release. */
    fun <T> spatialFast(): SpringSpec<T> = spring(SPATIAL_FAST_DAMPING, SPATIAL_FAST_STIFFNESS)

    /** The default. Most motion should use this one. */
    fun <T> spatialDefault(): SpringSpec<T> = spring(SPATIAL_DEFAULT_DAMPING, SPATIAL_DEFAULT_STIFFNESS)

    /** Reserved for the creature travelling between screens. */
    fun <T> spatialSlow(): SpringSpec<T> = spring(SPATIAL_SLOW_DAMPING, SPATIAL_SLOW_STIFFNESS)

    fun <T> effectsFast(): SpringSpec<T> = spring(EFFECTS_FAST_DAMPING, EFFECTS_FAST_STIFFNESS)

    fun <T> effectsDefault(): SpringSpec<T> = spring(EFFECTS_DEFAULT_DAMPING, EFFECTS_DEFAULT_STIFFNESS)

    fun <T> effectsSlow(): SpringSpec<T> = spring(EFFECTS_SLOW_DAMPING, EFFECTS_SLOW_STIFFNESS)

    /**
     * IntOffset needs a visibility threshold of one pixel, otherwise the
     * spring keeps animating sub-pixel deltas forever.
     */
    fun spatialDefaultOffset(): SpringSpec<IntOffset> =
        spring(
            dampingRatio = SPATIAL_DEFAULT_DAMPING,
            stiffness = SPATIAL_DEFAULT_STIFFNESS,
            visibilityThreshold = IntOffset(1, 1),
        )

    /** Stagger step between appearing groups. */
    const val STAGGER_STEP_MS = 40

    /** How far a group slides up as it appears, in dp. */
    const val ENTER_RISE_DP = 12

    /** Scale a pressable settles to while held. */
    const val PRESS_SCALE = 0.96f

    /** Guard so a hand-written spec cannot silently pick Standard damping. */
    val ALL_EFFECTS_DAMPINGS = listOf(EFFECTS_FAST_DAMPING, EFFECTS_DEFAULT_DAMPING, EFFECTS_SLOW_DAMPING)

    init {
        require(ALL_EFFECTS_DAMPINGS.all { it == Spring.DampingRatioNoBouncy }) {
            "effects tokens must not overshoot"
        }
    }
}
