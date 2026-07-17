package app.anima.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.pow

/**
 * WCAG contrast locked on the raw ARGB tokens (hermes-app D27/D28 pattern):
 * a palette tweak that breaks readability fails the build, no device needed.
 */
class PaletteContrastTest {
    @Test
    fun `night text on night backgrounds meets AA`() {
        assertThat(contrast(AnimaPalette.NightText, AnimaPalette.NightDeep)).isAtLeast(4.5)
        assertThat(contrast(AnimaPalette.NightText, AnimaPalette.NightSurface)).isAtLeast(4.5)
        assertThat(contrast(AnimaPalette.NightText, AnimaPalette.NightSurfaceHigh)).isAtLeast(4.5)
        assertThat(contrast(AnimaPalette.NightTextDim, AnimaPalette.NightSurface)).isAtLeast(4.5)
    }

    @Test
    fun `paper text on paper backgrounds meets AA`() {
        assertThat(contrast(AnimaPalette.PaperText, AnimaPalette.PaperDeep)).isAtLeast(4.5)
        assertThat(contrast(AnimaPalette.PaperText, AnimaPalette.PaperSurface)).isAtLeast(4.5)
        assertThat(contrast(AnimaPalette.PaperTextDim, AnimaPalette.PaperSurface)).isAtLeast(4.5)
    }

    @Test
    fun `seasonal tint keeps text AA in every season, both themes`() {
        // v0.3 seasonality shifts the deep surfaces by a few percent; the
        // token test alone no longer covers what the user actually sees.
        Season.entries.forEach { season ->
            listOf(animaDarkColors(), animaLightColors()).forEach { base ->
                val seasoned = base.seasoned(season)
                assertThat(contrastC(seasoned.text, seasoned.background)).isAtLeast(4.5)
                assertThat(contrastC(seasoned.text, seasoned.surface)).isAtLeast(4.5)
                assertThat(contrastC(seasoned.text, seasoned.surfaceHigh)).isAtLeast(4.5)
                assertThat(contrastC(seasoned.textDim, seasoned.surface)).isAtLeast(4.5)
            }
        }
    }

    private fun contrastC(
        fg: Color,
        bg: Color,
    ): Double = contrast(fg.toArgb().toLong() and 0xFFFFFFFFL, bg.toArgb().toLong() and 0xFFFFFFFFL)

    @Test
    fun `accents are readable as large text or UI components (3_0)`() {
        assertThat(contrast(AnimaPalette.NightAccent, AnimaPalette.NightDeep)).isAtLeast(3.0)
        assertThat(contrast(AnimaPalette.PaperAccent, AnimaPalette.PaperDeep)).isAtLeast(3.0)
        // Button label: background-on-accent must be readable as normal text.
        assertThat(contrast(AnimaPalette.NightDeep, AnimaPalette.NightAccent)).isAtLeast(4.5)
    }

    private fun contrast(
        fgArgb: Long,
        bgArgb: Long,
    ): Double {
        val l1 = relativeLuminance(fgArgb)
        val l2 = relativeLuminance(bgArgb)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF).toDouble() / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
