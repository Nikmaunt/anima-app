package app.anima.core.creature

import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.anima.core.creature.render.RenderContext
import app.anima.core.creature.render.Renderers
import kotlinx.coroutines.isActive
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.android.awaitFrame

/**
 * The creature on screen. Owns the ONE frame loop (withFrameNanos via
 * awaitFrame), strictly gated by RESUMED — leaving the screen or backgrounding
 * the app stops the loop entirely (hermes-lens discipline; ADR-001). All
 * animated state is read only inside the draw lambda: composition never
 * recomposes per frame.
 *
 * @param interactive gestures + haptics (gallery previews pass false).
 * @param reducedMotionOverride null = follow the system animator scale.
 */
@Composable
fun CreatureSurface(
    controller: CreatureController,
    night: Boolean,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    reducedMotionOverride: Boolean? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val engine = controller.engine
    val renderer = remember(controller.concept) { Renderers.forConcept(controller.concept) }
    val renderContext = remember { RenderContext() }
    val haptics = remember(interactive) { if (interactive) PurrHaptics(context) else null }

    val reducedMotion = reducedMotionOverride ?: remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }

    // Frame clock output. Written by the loop, read ONLY in the draw lambda.
    var frameTimeNanos by remember { mutableLongStateOf(0L) }
    val currentReduced by rememberUpdatedState(reducedMotion)

    LaunchedEffect(lifecycleOwner, engine) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            engine.onResume()
            engine.setReducedMotion(currentReduced)
            var frameCount = 0L
            var lastDrawnNanos = 0L
            while (isActive) {
                val t = awaitFrame()
                frameCount++
                engine.advance(t)
                val redraw = when {
                    // Reduced motion: statics; redraw at 4 Hz for rare blinks.
                    currentReduced -> t - lastDrawnNanos >= REDUCED_REDRAW_NANOS
                    // Slow states self-throttle to every other frame.
                    engine.lowPower -> frameCount % 2L == 0L
                    else -> true
                }
                if (redraw) {
                    lastDrawnNanos = t
                    frameTimeNanos = t
                }
            }
        }
    }
    val gestures = if (interactive) {
        Modifier
            .pointerInput(engine) {
                detectTapGestures(
                    onPress = { offset ->
                        engine.onLookAt(
                            (offset.x / size.width) * 2f - 1f,
                            (offset.y / size.height) * 2f - 1f,
                        )
                        tryAwaitRelease()
                        engine.onLookAway()
                    },
                    onTap = {
                        engine.onTap()
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    },
                )
            }
            .pointerInput(engine) {
                detectDragGestures(
                    onDragStart = { offset ->
                        engine.onPetStart()
                        engine.onPetMove(
                            (offset.x / size.width) * 2f - 1f,
                            (offset.y / size.height) * 2f - 1f,
                        )
                    },
                    onDrag = { change, _ ->
                        engine.onPetMove(
                            (change.position.x / size.width) * 2f - 1f,
                            (change.position.y / size.height) * 2f - 1f,
                        )
                        haptics?.purr(change.uptimeMillis, engine.pose.petLean)
                    },
                    onDragEnd = { engine.onPetEnd() },
                    onDragCancel = { engine.onPetEnd() },
                )
            }
    } else {
        Modifier
    }

    Spacer(
        modifier = modifier
            .then(gestures)
            .drawBehind {
                // The only per-frame invalidation point.
                @Suppress("UNUSED_EXPRESSION")
                frameTimeNanos

                val pose = engine.pose
                renderContext.pose = pose
                renderContext.genome = controller.genome
                renderContext.mood = engine.mood
                renderContext.timeSeconds = engine.timeSeconds
                renderContext.night = night
                renderContext.seed = controller.seed
                renderContext.batteryPercent = controller.batteryPercent
                renderContext.charging = controller.charging
                renderContext.growth = controller.growth

                val squashY = 1f + pose.squash * 0.10f
                val squashX = 1f / squashY
                val breathScale = 1f + pose.breath * 0.015f * pose.energy
                val lean = pose.petLean * size.minDimension * 0.01f

                translate(
                    left = pose.offsetX * size.minDimension,
                    top = pose.offsetY * size.minDimension + lean,
                ) {
                    rotate(degrees = pose.tiltDeg, pivot = center) {
                        scale(
                            scaleX = squashX * breathScale * controller.genome.sizeScale,
                            scaleY = squashY * breathScale * controller.genome.sizeScale,
                            pivot = center,
                        ) {
                            with(renderer) { render(renderContext) }
                        }
                    }
                }
            },
    )
}

private const val REDUCED_REDRAW_NANOS = 250_000_000L
