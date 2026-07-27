package app.anima.core.ui.theme

import androidx.compose.ui.text.font.FontWeight
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D8 in test form. The v1.0 scale let a hero digit and its caption sit at
 * 26sp and 12sp, both at medium weight, and the owner reported they looked
 * the same size. This pins the contrast so a future "let's calm the hero
 * down" edit fails here instead of on someone's phone.
 */
class TypeScaleContractTest {
    private val type = animaTypography(animaDarkColors())

    @Test
    fun `hero to caption contrast is multiplicative, not decorative`() {
        val hero = type.displayLarge.fontSize.value
        val caption = type.labelMedium.fontSize.value
        assertThat(hero / caption).isAtLeast(MIN_HERO_CAPTION_RATIO)
    }

    @Test
    fun `hero and caption pull in opposite directions by weight`() {
        // Same weight at different sizes reads weaker than opposed weights.
        val heroWeight = type.displayLarge.fontWeight!!.weight
        val captionWeight = type.labelMedium.fontWeight!!.weight
        assertThat(heroWeight).isLessThan(captionWeight)
        assertThat(type.displayLarge.fontWeight).isEqualTo(FontWeight.Light)
    }

    @Test
    fun `the scale descends without ties`() {
        val ladder =
            listOf(
                type.displayLarge,
                type.displayMedium,
                type.headlineMedium,
                type.headlineSmall,
                type.bodyLarge,
                type.labelMedium,
            ).map { it.fontSize.value }
        ladder.zipWithNext { a, b -> assertThat(a).isGreaterThan(b) }
    }

    @Test
    fun `the creature's voice is visibly larger than body text`() {
        assertThat(type.headlineSmall.fontSize.value).isGreaterThan(type.bodyLarge.fontSize.value)
    }

    @Test
    fun `line height never crowds its own size`() {
        listOf(
            type.displayLarge,
            type.displayMedium,
            type.headlineMedium,
            type.headlineSmall,
            type.titleMedium,
            type.bodyLarge,
            type.bodyMedium,
            type.bodySmall,
            type.labelLarge,
            type.labelMedium,
        ).forEach { style ->
            assertThat(style.lineHeight.value).isAtLeast(style.fontSize.value * 1.05f)
        }
    }
}
