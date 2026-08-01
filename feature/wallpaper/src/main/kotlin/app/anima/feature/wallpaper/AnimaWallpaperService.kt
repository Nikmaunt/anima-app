package app.anima.feature.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import app.anima.core.data.repo.IdentityRepository
import app.anima.core.model.Mood
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
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
        private var lastNight: Boolean? = null

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

        /**
         * v1.1c task 6 — found by putting the wallpaper on an actual home
         * screen, which is the thing eleven runs never did.
         *
         * The day frame is a light beige (#F4F0E9). The launcher paints its
         * date, its app labels and its clock in whatever colour the *wallpaper*
         * tells it to use, and this service told it nothing — so on the light
         * frame the launcher kept white text and the labels became unreadable.
         * Visible in `docs/design/v11/device/home-wallpaper-day.png`.
         *
         * `WallpaperColors.fromBitmap` computes the hints — including
         * `HINT_SUPPORTS_DARK_TEXT` — from real pixels, so the answer stays
         * correct if the palette ever changes. It is given a thumbnail of the
         * frame the wallpaper would actually draw, creature included, rather
         * than a swatch of the background: the launcher's own contrast maths
         * should see what the user sees.
         */
        override fun onComputeColors(): android.app.WallpaperColors {
            val night = isNightNow()
            val thumb =
                android.graphics.Bitmap.createBitmap(
                    COLOR_THUMB_W,
                    COLOR_THUMB_H,
                    android.graphics.Bitmap.Config.ARGB_8888,
                )
            val battery = stickyBattery()
            val concept = runCatching { kotlinx.coroutines.runBlocking { identityRepo().concept() } }.getOrNull()
            val seed = runCatching { kotlinx.coroutines.runBlocking { identityRepo().seed() } }.getOrNull()
            WallpaperFrame.draw(
                canvas = android.graphics.Canvas(thumb),
                concept = concept,
                seed = seed,
                mood = moodOf(batteryPercent(battery), batteryCharging(battery), night),
                batteryPercent = batteryPercent(battery),
                charging = batteryCharging(battery),
                night = night,
            )
            return android.app.WallpaperColors
                .fromBitmap(thumb)
                .also { thumb.recycle() }
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

        private fun identityRepo(): IdentityRepository =
            EntryPointAccessors
                .fromApplication(applicationContext, WallpaperEntryPoint::class.java)
                .identity()

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
            // v1.1b task 1c: this is a FULL-SCREEN surface on the home screen —
            // the place where a substituted body is least likely to be read as a
            // bug and most likely to be believed. If identity cannot be read,
            // the frame is background only. An empty wallpaper is a visible
            // problem; a stranger's creature is an invisible one.
            val concept =
                runCatching { kotlinx.coroutines.runBlocking { identity.concept() } }
                    .getOrNull()
            val seed =
                runCatching { kotlinx.coroutines.runBlocking { identity.seed() } }
                    .getOrNull()
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
            val wasNight = lastNight
            lastMood = mood
            lastNight = night
            // Day and night are different background LUMINANCES, so the answer
            // to onComputeColors changes with them and the launcher has to be
            // told. Cheap: it only asks when it is told something moved.
            if (wasNight != null && wasNight != night) notifyColorsChanged()

            val canvas = runCatching { holder.lockCanvas() }.getOrNull() ?: return
            try {
                WallpaperFrame.draw(
                    canvas = canvas,
                    concept = concept,
                    seed = seed,
                    mood = mood,
                    batteryPercent = percent,
                    charging = charging,
                    night = night,
                    paletteShiftDeg = paletteShift,
                )
            } finally {
                runCatching { holder.unlockCanvasAndPost(canvas) }
            }
        }

        private fun moodFrom(intent: Intent): Mood =
            moodOf(batteryPercent(intent), batteryCharging(intent), isNightNow())
    }

    private companion object {
        const val SLEEPY_BATTERY = 20

        /**
         * Big enough for `WallpaperColors.fromBitmap` to see the creature as
         * well as the wall, small enough that computing it costs nothing.
         */
        const val COLOR_THUMB_W = 108
        const val COLOR_THUMB_H = 234
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
