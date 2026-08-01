package app.anima.feature.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import app.anima.core.creature.render.StillRender
import app.anima.core.model.CreatureConcept
import app.anima.core.model.Mood

/**
 * v1.1c task 6 — one wallpaper frame, as a function of what it draws.
 *
 * This used to live inside `AnimaWallpaperService.Engine.drawOnce`, wrapped
 * around a `SurfaceHolder`, which meant the only way to see a wallpaper frame
 * was to put the wallpaper on a home screen and look. Across eleven runs nobody
 * did. Pulled out here it can be handed any `Canvas` — including one over a
 * `Bitmap` in an instrumented test — so all eight bodies and the
 * identity-unknown path can be rendered, saved and *looked at*.
 *
 * The service still owns everything this deliberately does not know about:
 * visibility, the frame budget, battery, the clock, and reading identity.
 */
object WallpaperFrame {
    const val NIGHT_BG = 0xFF0B0E14.toInt()
    val DAY_BG: Int = Color.rgb(244, 240, 233)

    /** The creature's share of the shorter screen edge. */
    const val CREATURE_FRACTION = 0.55f
    const val MIN_TILE_PX = 256
    const val DEFAULT_GROWTH = 0.5f

    /**
     * Background always; the creature only if there is one.
     *
     * v1.1b task 1c: a null [concept] or [seed] leaves the frame as background
     * alone. This is a full-screen surface on the home screen — the place where
     * a substituted body is least likely to be read as a bug and most likely to
     * be believed. An empty wallpaper is a visible problem; a stranger's
     * creature is an invisible one.
     */
    @Suppress("LongParameterList")
    fun draw(
        canvas: Canvas,
        concept: CreatureConcept?,
        seed: Long?,
        mood: Mood,
        batteryPercent: Int,
        charging: Boolean,
        night: Boolean,
        paletteShiftDeg: Float = 0f,
    ) {
        canvas.drawColor(if (night) NIGHT_BG else DAY_BG)
        if (concept == null || seed == null) return
        val side = minOf(canvas.width, canvas.height) * CREATURE_FRACTION
        val bitmap =
            StillRender.tile(
                concept = concept,
                seed = seed,
                mood = mood,
                batteryPercent = batteryPercent,
                charging = charging,
                night = night,
                growth = DEFAULT_GROWTH,
                sizePx = side.toInt().coerceAtLeast(MIN_TILE_PX),
                paletteShiftDeg = paletteShiftDeg,
            )
        val left = (canvas.width - bitmap.width) / 2f
        val top = (canvas.height - bitmap.height) / 2f
        canvas.drawBitmap(bitmap, left, top, Paint(Paint.FILTER_BITMAP_FLAG))
    }
}
