package app.anima.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import app.anima.core.ui.theme.AnimaMotion
import kotlinx.coroutines.delay

/**
 * Staggered entrance (defect D11: screens appear with no motion at all).
 * Each group starts one `STAGGER_STEP_MS` after the previous, rises
 * `ENTER_RISE_DP` on a spatial spring and fades in on an effects spring —
 * the effects token has damping 1.0, so alpha never overshoots past 1.
 *
 * Finite by construction: the springs settle and stop. This does not create
 * a standing frame loop, so the "animation only on a visible screen,
 * wallpaper 0 fps at rest" invariant is untouched.
 *
 * Pass `enabled = false` for calm motion (the Settings toggle) or in the
 * golden rig: the content is then simply there, with no rise and no fade.
 */
@Composable
fun Modifier.enterStaggered(
    index: Int = 0,
    enabled: Boolean = true,
): Modifier {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(index, enabled) {
        if (!enabled) {
            shown = true
            return@LaunchedEffect
        }
        delay(index.toLong() * AnimaMotion.STAGGER_STEP_MS)
        shown = true
    }
    if (!enabled) return this

    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = AnimaMotion.spatialDefault(),
        label = "enter-rise-$index",
    )
    val fade by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = AnimaMotion.effectsDefault(),
        label = "enter-fade-$index",
    )
    return this
        .alpha(fade.coerceIn(0f, 1f))
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val rise = (AnimaMotion.ENTER_RISE_DP.dp.toPx() * (1f - progress)).toInt()
            layout(placeable.width, placeable.height) {
                placeable.placeRelative(0, rise)
            }
        }
}
