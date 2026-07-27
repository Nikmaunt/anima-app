package app.anima.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.pow

/**
 * v1.1: the accent is derived from the creature's genome, so it is no longer
 * one reviewable constant — it is 360 of them, one per possible hue. The
 * safety argument is that only hue moves and S/L are pinned; this test is
 * that argument, executed.
 *
 * If someone later "just nudges" a lightness in SignatureHue, this fails
 * instead of shipping an unreadable phone to whoever rolled that hue.
 */
class SignatureHueContrastTest {
    private val everyHue = (0 until 360).map { it.toFloat() }

    @Test
    fun `text meets AA on every derived background, both themes`() {
        listOf(true, false).forEach { night ->
            everyHue.forEach { hue ->
                val c = SignatureHue.colors(hue, night)
                assertThat(contrast(c.text, c.background)).isAtLeast(4.5)
                assertThat(contrast(c.text, c.surface)).isAtLeast(4.5)
                assertThat(contrast(c.text, c.surfaceHigh)).isAtLeast(4.5)
                assertThat(contrast(c.textDim, c.surface)).isAtLeast(4.5)
            }
        }
    }

    @Test
    fun `accent is readable as a UI component on every hue`() {
        listOf(true, false).forEach { night ->
            everyHue.forEach { hue ->
                val c = SignatureHue.colors(hue, night)
                assertThat(contrast(c.accent, c.background)).isAtLeast(3.0)
            }
        }
    }

    @Test
    fun `pill button label is readable on every hue`() {
        // PillButton paints background-on-accent; that is normal-size text.
        listOf(true, false).forEach { night ->
            everyHue.forEach { hue ->
                val c = SignatureHue.colors(hue, night)
                assertThat(contrast(c.background, c.accent)).isAtLeast(4.5)
            }
        }
    }

    @Test
    fun `seasonal tint keeps AA on every hue`() {
        Season.entries.forEach { season ->
            listOf(true, false).forEach { night ->
                everyHue.forEach { hue ->
                    val c = SignatureHue.colors(hue, night).seasoned(season)
                    assertThat(contrast(c.text, c.background)).isAtLeast(4.5)
                    assertThat(contrast(c.text, c.surface)).isAtLeast(4.5)
                    assertThat(contrast(c.textDim, c.surface)).isAtLeast(4.5)
                }
            }
        }
    }

    @Test
    fun `every concept resolves to a hue and the genome shift stays bounded`() {
        val concepts =
            listOf(
                "SPIRIT_ORB",
                "FOX_KIT",
                "JELLY",
                "ROBOT",
                "PIXEL_PET",
                "SPROUT",
                "EMBER",
                "MOTH",
            )
        concepts.forEach { name ->
            // ±18° is the genome's documented range (CreatureSeed.kt:59).
            listOf(-18f, 0f, 18f).forEach { shift ->
                val h = SignatureHue.resolve(name, shift)
                assertThat(h).isAtLeast(0f)
                assertThat(h).isLessThan(360f)
            }
        }
        // Distinct concepts must not collapse onto one accent.
        assertThat(concepts.map { SignatureHue.of(it) }.toSet()).hasSize(concepts.size)
    }

    private fun contrast(
        fg: Color,
        bg: Color,
    ): Double {
        val l1 = relativeLuminance(fg)
        val l2 = relativeLuminance(bg)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        val argb = color.toArgb().toLong() and 0xFFFFFFFFL

        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF).toDouble() / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
