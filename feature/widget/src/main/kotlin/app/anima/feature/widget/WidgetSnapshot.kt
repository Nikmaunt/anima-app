package app.anima.feature.widget

import android.content.Context
import android.graphics.Bitmap
import android.os.BatteryManager
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import app.anima.core.creature.engine.CreatureEngine
import app.anima.core.creature.render.RenderContext
import app.anima.core.creature.render.Renderers
import app.anima.core.model.CreatureConcept
import app.anima.core.model.CreatureGenome
import app.anima.core.model.Mood

/**
 * ADR-007: ONE static frame of the rig, rendered off-screen into a bitmap.
 * The engine is created, told the mood, settled in reduced-motion statics,
 * advanced exactly one tick and thrown away — no loop survives this call.
 *
 * Bitmap budget: RemoteViews caps total bitmap memory at ~1.5 screens; a
 * single 512 px square (1 MB) is far inside it.
 */
object WidgetSnapshot {
    const val SIZE_PX = 512

    data class Vitals(
        val batteryPercent: Int,
        val charging: Boolean,
    )

    fun readVitals(context: Context): Vitals {
        val bm = context.getSystemService(BatteryManager::class.java)
        val percent = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: FALLBACK_BATTERY
        val charging = bm?.isCharging ?: false
        return Vitals(percent.coerceIn(0, 100), charging)
    }

    /** Deterministic mood for the snapshot — MoodEngine-lite on two signals. */
    fun moodFor(
        vitals: Vitals,
        night: Boolean,
    ): Mood =
        when {
            vitals.charging -> Mood.EATING
            night -> Mood.ASLEEP
            vitals.batteryPercent <= SLEEPY_BATTERY -> Mood.SLEEPY
            else -> Mood.ALERT
        }

    fun render(
        concept: CreatureConcept,
        seed: Long,
        mood: Mood,
        vitals: Vitals,
        night: Boolean,
        growth: Float,
    ): Bitmap {
        val genome = CreatureGenome.from(seed)
        val engine = CreatureEngine(seed, genome)
        engine.setReducedMotion(true)
        engine.setMood(mood)
        engine.onResume()
        engine.advance(ONE_TICK_NANOS)

        val image = ImageBitmap(SIZE_PX, SIZE_PX)
        val renderContext =
            RenderContext().apply {
                pose = engine.pose
                this.genome = genome
                this.mood = mood
                timeSeconds = 0f
                this.night = night
                this.seed = seed
                batteryPercent = vitals.batteryPercent
                charging = vitals.charging
                this.growth = growth
            }
        val renderer = Renderers.forConcept(concept)
        CanvasDrawScope().draw(
            Density(1f),
            LayoutDirection.Ltr,
            Canvas(image),
            Size(SIZE_PX.toFloat(), SIZE_PX.toFloat()),
        ) {
            with(renderer) { render(renderContext) }
        }
        return image.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
    }

    private const val FALLBACK_BATTERY = 50
    private const val SLEEPY_BATTERY = 20
    private const val ONE_TICK_NANOS = 16_000_000L
}
