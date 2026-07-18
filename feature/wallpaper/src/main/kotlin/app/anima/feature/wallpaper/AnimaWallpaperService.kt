package app.anima.feature.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Paint
import android.os.BatteryManager
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import app.anima.core.creature.render.StillRender
import app.anima.core.data.repo.IdentityRepository
import app.anima.core.model.CreatureConcept
import app.anima.core.model.Mood
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ADR-012, strictest form. The creature as a STILL wallpaper:
 *
 * - 0 fps steady state — there is no frame loop anywhere in this file.
 * - A redraw happens only for SURFACE reasons (created/changed/visible) or
 *   for a STATE reason (battery edge observed while visible), the latter
 *   debounced by [WallpaperBudget].
 * - Invisible = dead stop: the battery receiver is unregistered, the render
 *   scope cancelled, and every draw re-checks visibility (defense against
 *   the Samsung-AOD missed-callback pitfall recorded in ADR-012).
 * - No wake locks, no WorkManager, no sensors, no timers.
 */
class AnimaWallpaperService : WallpaperService() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WallpaperEntryPoint {
        fun identity(): IdentityRepository

        fun prefs(): app.anima.core.data.prefs.AnimaPrefs
    }

    override fun onCreateEngine(): Engine = StillEngine()

    inner class StillEngine : Engine() {
        private var scope: CoroutineScope? = null
        private var lastStateDrawMs: Long? = null
        private var lastMood: Mood? = null

        private val batteryReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    val mood = moodFrom(intent)
                    if (mood == lastMood) return
                    requestDraw(WallpaperBudget.Reason.STATE, intent)
                }
            }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                scope?.cancel()
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
                registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                requestDraw(WallpaperBudget.Reason.SURFACE, stickyBattery())
            } else {
                stopEverything()
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int,
        ) {
            requestDraw(WallpaperBudget.Reason.SURFACE, stickyBattery())
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            stopEverything()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            stopEverything()
            super.onDestroy()
        }

        private fun stopEverything() {
            runCatching { unregisterReceiver(batteryReceiver) }
            scope?.cancel()
            scope = null
        }

        private fun stickyBattery(): Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        private fun requestDraw(
            reason: WallpaperBudget.Reason,
            battery: Intent?,
        ) {
            val now = System.currentTimeMillis()
            if (!WallpaperBudget.allowsDraw(reason, isVisible, now, lastStateDrawMs)) return
            if (reason == WallpaperBudget.Reason.STATE) lastStateDrawMs = now
            scope?.launch { drawOnce(battery) }
        }

        /** One frame, then silence. Never runs when invisible. */
        private fun drawOnce(battery: Intent?) {
            if (!isVisible) return
            val holder = surfaceHolder ?: return
            val entry =
                EntryPointAccessors
                    .fromApplication(applicationContext, WallpaperEntryPoint::class.java)
            val identity = entry.identity()
            val concept =
                runCatching { kotlinx.coroutines.runBlocking { identity.concept() } }
                    .getOrNull() ?: CreatureConcept.SPIRIT_ORB
            val seed =
                runCatching { kotlinx.coroutines.runBlocking { identity.seed() } }
                    .getOrNull() ?: 0L
            // v0.4 milestones: the worn palette follows onto the wallpaper.
            val paletteShift =
                runCatching {
                    kotlinx.coroutines.runBlocking {
                        val now = System.currentTimeMillis()
                        app.anima.core.model.Milestones.effectiveShiftDeg(
                            entry.prefs().paletteVariant().first(),
                            identity.stats(now),
                            now,
                        )
                    }
                }.getOrDefault(0f)

            val percent = batteryPercent(battery)
            val charging = batteryCharging(battery)
            val night = isNightNow()
            val mood = moodOf(percent, charging, night)
            lastMood = mood

            val canvas = runCatching { holder.lockCanvas() }.getOrNull() ?: return
            try {
                canvas.drawColor(if (night) NIGHT_BG else DAY_BG)
                val side = minOf(canvas.width, canvas.height) * CREATURE_FRACTION
                val bitmap =
                    StillRender.tile(
                        concept = concept,
                        seed = seed,
                        mood = mood,
                        batteryPercent = percent,
                        charging = charging,
                        night = night,
                        growth = DEFAULT_GROWTH,
                        sizePx = side.toInt().coerceAtLeast(MIN_TILE_PX),
                        paletteShiftDeg = paletteShift,
                    )
                val left = (canvas.width - bitmap.width) / 2f
                val top = (canvas.height - bitmap.height) / 2f
                canvas.drawBitmap(bitmap, left, top, Paint(Paint.FILTER_BITMAP_FLAG))
            } finally {
                runCatching { holder.unlockCanvasAndPost(canvas) }
            }
        }

        private fun moodFrom(intent: Intent): Mood =
            moodOf(batteryPercent(intent), batteryCharging(intent), isNightNow())
    }

    private companion object {
        const val NIGHT_BG = 0xFF0B0E14.toInt()
        val DAY_BG = Color.rgb(244, 240, 233)
        const val CREATURE_FRACTION = 0.55f
        const val MIN_TILE_PX = 256
        const val DEFAULT_GROWTH = 0.5f
        const val SLEEPY_BATTERY = 20
        const val NIGHT_FROM = 22
        const val NIGHT_UNTIL = 7

        fun batteryPercent(intent: Intent?): Int {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            return if (level >= 0 && scale > 0) level * 100 / scale else 50
        }

        fun batteryCharging(intent: Intent?): Boolean = (intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0

        fun isNightNow(): Boolean {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return hour >= NIGHT_FROM || hour < NIGHT_UNTIL
        }

        /** WidgetSnapshot.moodFor's rules, kept in sync by the shared test. */
        fun moodOf(
            percent: Int,
            charging: Boolean,
            night: Boolean,
        ): Mood =
            when {
                charging -> Mood.EATING
                night -> Mood.ASLEEP
                percent <= SLEEPY_BATTERY -> Mood.SLEEPY
                else -> Mood.ALERT
            }
    }
}
