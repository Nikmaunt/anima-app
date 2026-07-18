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
    ): Bitmap {
        val genome = CreatureGenome.from(seed)
        val engine = CreatureEngine(seed, genome)
        engine.setReducedMotion(true)
        engine.setMood(mood)
        engine.onResume()
        engine.advance(ONE_TICK_NANOS)

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

    private const val ONE_TICK_NANOS = 16_000_000L
}
