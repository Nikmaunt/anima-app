package app.anima.core.ui.theme

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
