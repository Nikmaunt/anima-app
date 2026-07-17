package app.anima.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Semantic colors beyond Material's scheme; the creature layer reads these. */
@Immutable
data class AnimaColors(
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val text: Color,
    val textDim: Color,
    val outline: Color,
    val accent: Color,
    val accentSoft: Color,
    val warn: Color,
    val danger: Color,
    val isNight: Boolean,
)

val LocalAnimaColors = staticCompositionLocalOf {
    animaDarkColors() // safe default; real value always provided by AnimaTheme
}

fun animaDarkColors() = AnimaColors(
    background = Color(AnimaPalette.NightDeep),
    surface = Color(AnimaPalette.NightSurface),
    surfaceHigh = Color(AnimaPalette.NightSurfaceHigh),
    text = Color(AnimaPalette.NightText),
    textDim = Color(AnimaPalette.NightTextDim),
    outline = Color(AnimaPalette.NightOutline),
    accent = Color(AnimaPalette.NightAccent),
    accentSoft = Color(AnimaPalette.NightAccentDeep),
    warn = Color(AnimaPalette.NightWarn),
    danger = Color(AnimaPalette.NightDanger),
    isNight = true,
)

fun animaLightColors() = AnimaColors(
    background = Color(AnimaPalette.PaperDeep),
    surface = Color(AnimaPalette.PaperSurface),
    surfaceHigh = Color(AnimaPalette.PaperSurfaceHigh),
    text = Color(AnimaPalette.PaperText),
    textDim = Color(AnimaPalette.PaperTextDim),
    outline = Color(AnimaPalette.PaperOutline),
    accent = Color(AnimaPalette.PaperAccent),
    accentSoft = Color(AnimaPalette.PaperAccentSoft),
    warn = Color(AnimaPalette.PaperWarn),
    danger = Color(AnimaPalette.PaperDanger),
    isNight = false,
)

@Composable
fun AnimaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) animaDarkColors() else animaLightColors()
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.background,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.surfaceHigh,
            onSurfaceVariant = colors.textDim,
            outline = colors.outline,
            error = colors.danger,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.surfaceHigh,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.surfaceHigh,
            onSurfaceVariant = colors.textDim,
            outline = colors.outline,
            error = colors.danger,
        )
    }
    CompositionLocalProvider(LocalAnimaColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = animaTypography(colors),
            shapes = Shapes(
                extraSmall = RoundedCornerShape(10.dp),
                small = RoundedCornerShape(14.dp),
                medium = RoundedCornerShape(20.dp),
                large = RoundedCornerShape(28.dp),
                extraLarge = RoundedCornerShape(36.dp),
            ),
            content = content,
        )
    }
}
