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

    /**
     * v1.1b task 1c: no default seed. [pose] and [genome] are `lateinit` for
     * this exact reason and a Long cannot be, so the same contract is spelled
     * out by hand: reading before the frame set it fails loudly instead of
     * quietly drawing seed 0 — which is a different creature's randomness, and
     * on this frame there is no way to see that from the picture.
     *
     * A `Long?` would have been shorter and would have boxed on every frame;
     * this class is reused per frame under a zero-alloc rule.
     */
    private var seedValue: Long = 0L
    private var seedAssigned: Boolean = false

    var seed: Long
        get() {
            check(seedAssigned) { "RenderContext.seed read before the frame set it" }
            return seedValue
        }
        set(value) {
            seedValue = value
            seedAssigned = true
        }

    /** Vitals some concepts embody directly (robot's charge bar, sprout growth). */
    var batteryPercent: Int = 80
    var charging: Boolean = false

    /** 0..1, how much soul has accumulated (drives the sprout's growth). */
    var growth: Float = 0.3f

    /**
     * v0.4 milestones: the unlocked palette variant's hue rotation
     * (Milestones/PaletteVariant), on top of the genome's ±18°. 0 = true
     * self.
     */
    var paletteShiftDeg: Float = 0f
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
        val hue = baseHue + ctx.genome.hueShiftDeg + ctx.paletteShiftDeg - pose.flush * 18f
        val s = sat * (0.55f + 0.45f * pose.energy)
        val l = light * (0.75f + 0.25f * pose.energy) + pose.flush * 0.05f
        return hsl(hue, s, l, alpha)
    }
}
