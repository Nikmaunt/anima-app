package app.anima.core.creature.render

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import app.anima.core.creature.engine.CreatureEngine
import app.anima.core.model.CreatureConcept
import app.anima.core.model.CreatureGenome
import app.anima.core.model.Mood

/**
 * ONE static frame of the rig, rendered off-screen into a bitmap — the shared
 * recipe behind every creature surface that lives outside the app (widget,
 * live wallpaper). The engine is created, told the mood, settled in
 * reduced-motion statics, advanced exactly one tick and thrown away — no loop
 * survives this call.
 *
 * "Exactly one tick" became true in v1.1; before that it was zero, and the
 * frame carried the default ALERT pose no matter what mood was asked for
 * (audit-v10 finding 0.3-A). StillRenderTicksTest holds it.
 */
object StillRender {
    fun tile(
        concept: CreatureConcept,
        seed: Long,
        mood: Mood,
        batteryPercent: Int,
        charging: Boolean,
        night: Boolean,
        growth: Float,
        sizePx: Int,
        paletteShiftDeg: Float = 0f,
    ): Bitmap {
        val genome = CreatureGenome.from(seed)
        val engine = CreatureEngine(seed, genome)
        engine.setReducedMotion(true)
        engine.setMood(mood)
        engine.onResume()
        // v1.1 (audit-v10 finding 0.3-A). `onResume()` sets lastFrameNanos to
        // Long.MIN_VALUE, and `advance` treats that as "first frame ever":
        // it anchors the clock and returns without simulating. So the single
        // advance below used to run ZERO ticks, `advanceReduced` never ran,
        // and pose.energy / pose.lidDroop / pose.flush kept their defaults
        // whatever the mood said. That is why five of the eight concepts had
        // a widget that was literally the same picture in every state, and
        // why ember-asleep-night had its eyes open.
        //
        // Anchoring is now a separate, explicit call, and the tick after it
        // is the one tick this class always claimed to run.
        engine.advance(CLOCK_ANCHOR_NANOS)
        engine.advance(CLOCK_ANCHOR_NANOS + ONE_TICK_NANOS)

        val image = ImageBitmap(sizePx, sizePx)
        val renderContext =
            RenderContext().apply {
                pose = engine.pose
                this.genome = genome
                this.mood = mood
                timeSeconds = 0f
                this.night = night
                this.seed = seed
                this.batteryPercent = batteryPercent
                this.charging = charging
                this.growth = growth
                this.paletteShiftDeg = paletteShiftDeg
            }
        val renderer = Renderers.forConcept(concept)
        CanvasDrawScope().draw(
            Density(1f),
            LayoutDirection.Ltr,
            Canvas(image),
            Size(sizePx.toFloat(), sizePx.toFloat()),
        ) {
            with(renderer) { render(renderContext) }
        }
        return image.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
    }

    /** Arbitrary monotonic origin; only the delta to the next call matters. */
    private const val CLOCK_ANCHOR_NANOS = 0L

    private const val ONE_TICK_NANOS = 16_000_000L
}
