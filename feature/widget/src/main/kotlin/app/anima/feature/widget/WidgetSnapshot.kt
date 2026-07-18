package app.anima.feature.widget

import android.content.Context
import android.graphics.Bitmap
import android.os.BatteryManager
import app.anima.core.creature.render.StillRender
import app.anima.core.model.CreatureConcept
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

    /**
     * v0.4: delegates to the shared [StillRender] recipe (also used by the
     * live wallpaper and pinned bit-for-bit by RigGoldenTest's goldens).
     */
    fun render(
        concept: CreatureConcept,
        seed: Long,
        mood: Mood,
        vitals: Vitals,
        night: Boolean,
        growth: Float,
    ): Bitmap =
        StillRender.tile(
            concept = concept,
            seed = seed,
            mood = mood,
            batteryPercent = vitals.batteryPercent,
            charging = vitals.charging,
            night = night,
            growth = growth,
            sizePx = SIZE_PX,
        )

    private const val FALLBACK_BATTERY = 50
    private const val SLEEPY_BATTERY = 20
}
