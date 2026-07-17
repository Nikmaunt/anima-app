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
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import java.time.LocalDate

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

val LocalAnimaColors =
    staticCompositionLocalOf {
        animaDarkColors() // safe default; real value always provided by AnimaTheme
    }

fun animaDarkColors() =
    AnimaColors(
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

fun animaLightColors() =
    AnimaColors(
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

/**
 * v0.3 seasonality: the world the creature lives in breathes with the year —
 * QUIET variations only (a few percent toward a seasonal tint on the deep
 * surfaces; text/accent untouched, so contrast stays proven by test).
 */
enum class Season {
    WINTER,
    SPRING,
    SUMMER,
    AUTUMN,
    ;

    companion object {
        fun fromMonth(month: Int): Season =
            when (month) {
                12, 1, 2 -> WINTER
                3, 4, 5 -> SPRING
                6, 7, 8 -> SUMMER
                else -> AUTUMN
            }
    }
}

fun AnimaColors.seasoned(season: Season): AnimaColors {
    val tint =
        when (season) {
            Season.WINTER -> Color(0xFF3A5A8C) // colder blue
            Season.SPRING -> Color(0xFF3A6B4F) // young green
            Season.SUMMER -> Color(0xFF8C6A3A) // warm gold
            Season.AUTUMN -> Color(0xFF8C4A3A) // ember rust
        }
    return copy(
        background = lerp(background, tint, SEASON_TINT),
        surface = lerp(surface, tint, SEASON_TINT),
        surfaceHigh = lerp(surfaceHigh, tint, SEASON_TINT * 0.7f),
        accentSoft = lerp(accentSoft, tint, SEASON_TINT),
    )
}

const val SEASON_TINT = 0.06f

@Composable
fun AnimaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val season = remember { Season.fromMonth(LocalDate.now().monthValue) }
    val colors = (if (darkTheme) animaDarkColors() else animaLightColors()).seasoned(season)
    val scheme =
        if (darkTheme) {
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
            shapes =
                Shapes(
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
