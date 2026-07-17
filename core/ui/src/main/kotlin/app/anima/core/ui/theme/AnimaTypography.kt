package app.anima.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * System fonts only (nothing is downloaded, ever). Character comes from the
 * scale: airy display sizes with tight tracking for the creature's voice,
 * relaxed body text, generous line heights. The creature speaks in
 * `headlineSmall`; chrome whispers in `label*`.
 */
fun animaTypography(colors: AnimaColors): Typography {
    val base = TextStyle(fontFamily = FontFamily.SansSerif, color = colors.text)
    return Typography(
        displayMedium = base.copy(
            fontSize = 40.sp, lineHeight = 46.sp,
            fontWeight = FontWeight.Light, letterSpacing = (-0.02).em,
        ),
        headlineMedium = base.copy(
            fontSize = 26.sp, lineHeight = 33.sp,
            fontWeight = FontWeight.Medium, letterSpacing = (-0.01).em,
        ),
        headlineSmall = base.copy(
            fontSize = 21.sp, lineHeight = 30.sp,
            fontWeight = FontWeight.Normal, letterSpacing = 0.em,
        ),
        titleMedium = base.copy(
            fontSize = 17.sp, lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.em,
        ),
        bodyLarge = base.copy(
            fontSize = 16.sp, lineHeight = 25.sp,
            fontWeight = FontWeight.Normal,
        ),
        bodyMedium = base.copy(
            fontSize = 14.sp, lineHeight = 21.sp,
            fontWeight = FontWeight.Normal, color = colors.textDim,
        ),
        labelLarge = base.copy(
            fontSize = 15.sp, lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.01.em,
        ),
        labelMedium = base.copy(
            fontSize = 12.sp, lineHeight = 16.sp,
            fontWeight = FontWeight.Medium, letterSpacing = 0.04.em,
            color = colors.textDim,
        ),
    )
}
