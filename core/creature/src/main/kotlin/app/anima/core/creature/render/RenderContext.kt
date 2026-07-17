package app.anima.core.creature.render

import androidx.compose.ui.graphics.Color
import app.anima.core.creature.engine.CreaturePose
import app.anima.core.model.CreatureGenome
import app.anima.core.model.Mood

/**
 * Everything a concept renderer may read for one frame. Mutable single
 * instance reused across frames (zero-alloc rule); renderers never hold on
 * to it between frames.
 */
class RenderContext {
    lateinit var pose: CreaturePose
    lateinit var genome: CreatureGenome
    var mood: Mood = Mood.ALERT
    var timeSeconds: Float = 0f
    var night: Boolean = true
    var seed: Long = 0L

    /** Vitals some concepts embody directly (robot's charge bar, sprout growth). */
    var batteryPercent: Int = 80
    var charging: Boolean = false

    /** 0..1, how much soul has accumulated (drives the sprout's growth). */
    var growth: Float = 0.3f
}

/** Hue helpers shared by renderers. */
object Hues {
    fun hsl(
        hue: Float,
        saturation: Float,
        lightness: Float,
        alpha: Float = 1f,
    ): Color {
        var h = hue % 360f
        if (h < 0) h += 360f
        return Color.hsl(h, saturation.coerceIn(0f, 1f), lightness.coerceIn(0f, 1f), alpha)
    }

    /** Mood-wide tint: flush pushes toward ember red, low energy desaturates. */
    fun bodyColor(
        baseHue: Float,
        sat: Float,
        light: Float,
        ctx: RenderContext,
        alpha: Float = 1f,
    ): Color {
        val pose = ctx.pose
        val hue = baseHue + ctx.genome.hueShiftDeg - pose.flush * 18f
        val s = sat * (0.55f + 0.45f * pose.energy)
        val l = light * (0.75f + 0.25f * pose.energy) + pose.flush * 0.05f
        return hsl(hue, s, l, alpha)
    }
}
