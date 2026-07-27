package app.anima.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * System fonts only (nothing is downloaded, ever).
 *
 * v1.1 rebuild (docs/design-system-v11.md §1). The v1.0 scale put the hero
 * number at 26sp and its caption at 12sp — a ratio of 2.17 with both at
 * medium weight, which is exactly what the owner reported as "the hero digit
 * and its caption are almost the same size" (defect D8). M3's own scale
 * spreads Display Large to Label Medium by 4.75x; the floor adopted here is
 * 3x, and the pair below is 44/12 = 3.67x with opposed weights — a Light
 * hero over a Medium caption, so the contrast is carried by weight as well
 * as size.
 *
 * Roles are named by job, not by size, so `hero` cannot land on a caption by
 * accident. They map onto Material slots because the whole app reads
 * `MaterialTheme.typography.*`:
 *
 * | role    | M3 slot        | size |
 * |---------|----------------|------|
 * | hero    | displayLarge   | 44   |
 * | display | displayMedium  | 32   |
 * | title   | headlineMedium | 24   |
 * | voice   | headlineSmall  | 19   |
 * | body    | bodyLarge      | 16   |
 * | bodyDim | bodyMedium     | 15   |
 * | action  | labelLarge     | 15   |
 * | caption | labelMedium    | 12   |
 *
 * `voice` belongs to the creature and to nothing else: when it speaks you
 * see it before you read it.
 */
fun animaTypography(colors: AnimaColors): Typography {
    val base = TextStyle(fontFamily = FontFamily.SansSerif, color = colors.text)
    return Typography(
        // hero: one number or one word per screen
        displayLarge =
            base.copy(
                fontSize = HERO_SP.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-0.03).em,
            ),
        // display: the headline of a state screen (hatching, an empty state)
        displayMedium =
            base.copy(
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-0.02).em,
            ),
        // title: the name of a screen
        headlineMedium =
            base.copy(
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-0.01).em,
            ),
        // voice: the creature speaking
        headlineSmall =
            base.copy(
                fontSize = 19.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.em,
            ),
        titleMedium =
            base.copy(
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.em,
            ),
        bodyLarge =
            base.copy(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal,
            ),
        bodyMedium =
            base.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textDim,
            ),
        bodySmall =
            base.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textDim,
            ),
        // action: buttons and links
        labelLarge =
            base.copy(
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.01.em,
            ),
        // caption: under a hero, and every all-caps section label
        labelMedium =
            base.copy(
                fontSize = CAPTION_SP.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.06.em,
                color = colors.textDim,
            ),
    )
}

/** Hero size in sp. Public so the scale contract is assertable in a test. */
const val HERO_SP = 44f

/** Caption size in sp; the other half of the hero/caption contrast. */
const val CAPTION_SP = 12f

/**
 * Floor for hero-to-caption size contrast, derived in
 * docs/research-v11-design.md §1.4 from M3's own proportions. Decorative
 * contrast — v1.0's 2.17x — is what D8 looked like on the device.
 */
const val MIN_HERO_CAPTION_RATIO = 3.0f
