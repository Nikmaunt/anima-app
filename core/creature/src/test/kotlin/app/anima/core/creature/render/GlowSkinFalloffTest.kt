package app.anima.core.creature.render

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.sqrt

/**
 * Defect D5, in test form.
 *
 * On the hardware canvas the glow is an AGSL shader painted into a plain
 * `drawRect`. The shader fades out on its own schedule; the rect crops
 * unconditionally. If the rect is narrower than the fade, the crop becomes a
 * visible hard-edged square around the creature — which is what the owner saw
 * on the S24 while the goldens looked clean, because the goldens go down the
 * software branch where a radial gradient is drawn to Transparent.
 *
 * Two independent readers of this file (the Kotlin constants and the AGSL
 * string) have to agree, so the AGSL is asserted textually too: a silent edit
 * to the shader's falloff would otherwise reopen D5 without failing anything.
 */
class GlowSkinFalloffTest {
    @Test
    fun `the rect edge is past the point where the shader has faded out`() {
        assertThat(GlowSkin.HALF_SIDE).isGreaterThan(GlowSkin.FALLOFF_RADII)
    }

    @Test
    fun `even the nearest edge midpoint clears the falloff`() {
        // The midpoint of a side is the closest point of the rect to the
        // centre, so it is the strictest case; corners are further out.
        val nearestEdgeDistance = GlowSkin.HALF_SIDE
        val corner = GlowSkin.HALF_SIDE * sqrt(2f)
        assertThat(nearestEdgeDistance).isGreaterThan(GlowSkin.FALLOFF_RADII)
        assertThat(corner).isGreaterThan(GlowSkin.FALLOFF_RADII)
    }

    @Test
    fun `the pre-v1_1 half side is recorded as failing, so nobody restores it`() {
        val old = 2.2f
        assertThat(old).isLessThan(GlowSkin.FALLOFF_RADII)
    }

    @Test
    fun `the AGSL falloff still matches the constants it is mirrored into`() {
        val agsl = GlowSkin.AGSL_SOURCE
        // ripple amplitude
        assertThat(agsl).contains("* ${GlowSkin.RIPPLE_MAX}")
        // edge = uRadius * (1.35 + ripple)
        assertThat(agsl).contains("uRadius * (1.35 + ripple)")
        // fall = 1 - smoothstep(..., edge * 2.0, dist)
        assertThat(agsl).contains("edge * 2.0")
    }
}
