package app.anima.core.creature.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import app.anima.core.creature.engine.ValueNoise
import app.anima.core.model.Mood
import kotlin.math.sin

/**
 * Огонёк: a flame wisp. Its silhouette is a teardrop distorted by noise every
 * frame; sparks rise deterministically (hash of a time bucket — replayable);
 * thermal states are its native language: cozy gold when calm, ragged red
 * when the phone runs hot, dim pulsing embers when asleep.
 */
class EmberRenderer : CreatureRenderer {

    private val flamePath = Path()
    private val glow = GlowSkin()

    override fun DrawScope.render(ctx: RenderContext) {
        val pose = ctx.pose
        val c = Offset(size.width / 2f, size.height / 2f + size.minDimension * 0.06f)
        val r = size.minDimension * 0.24f
        val hot = ctx.mood == Mood.HOT
        val asleep = ctx.mood == Mood.ASLEEP
        val baseHue = when {
            hot -> 8f
            ctx.mood == Mood.ANXIOUS -> 22f
            else -> 36f
        }
        val bodyColor = Hues.bodyColor(baseHue, sat = 0.85f, light = if (asleep) 0.4f else 0.62f, ctx = ctx)
        val coreColor = Hues.hsl(baseHue + 18f, 0.9f, 0.82f)

        with(glow) {
            drawGlow(
                center = c.copy(y = c.y - r * 0.3f),
                radius = r,
                color = bodyColor,
                intensity = if (asleep) 0.25f + 0.1f * pose.breath else 0.5f + 0.3f * pose.energy,
                time = ctx.timeSeconds,
            )
        }

        // Flame silhouette: teardrop with noise-rippled flanks. Raggedness
        // rises with heat and anxiety; asleep collapses to a low ember dome.
        val rag = when {
            hot -> 0.28f
            ctx.mood == Mood.ANXIOUS -> 0.2f
            asleep -> 0.03f
            else -> 0.1f
        }
        val height = r * (if (asleep) 1.0f else 1.9f) * (1f + pose.breath * 0.12f)
        val width = r * 1.25f
        flamePath.reset()
        val steps = 14
        for (i in 0..steps) {
            val t = i / steps.toFloat() // 0 bottom-left → 1 top
            val angle = t * Math.PI.toFloat()
            val edge = sin(angle)
            val ripple = ValueNoise.noise(t * 3f + ctx.timeSeconds * (if (hot) 4.5f else 2.2f), ctx.seed, 80 + i) * rag
            // Left flank up.
            val fx = c.x - (edge * width / 2f) * (1f + ripple)
            val fy = c.y + r * 0.5f - height * t
            if (i == 0) flamePath.moveTo(fx, fy) else flamePath.lineTo(fx, fy)
        }
        for (i in steps downTo 0) {
            val t = i / steps.toFloat()
            val angle = t * Math.PI.toFloat()
            val edge = sin(angle)
            val ripple = ValueNoise.noise(t * 3f + ctx.timeSeconds * (if (hot) 4.5f else 2.2f), ctx.seed, 110 + i) * rag
            val fx = c.x + (edge * width / 2f) * (1f + ripple)
            val fy = c.y + r * 0.5f - height * t
            flamePath.lineTo(fx, fy)
        }
        flamePath.close()
        drawPath(flamePath, bodyColor.copy(alpha = 0.92f))

        // Inner core.
        drawOval(
            coreColor.copy(alpha = if (asleep) 0.5f + 0.2f * pose.breath else 0.85f),
            topLeft = Offset(c.x - width * 0.28f, c.y + r * 0.45f - height * 0.55f),
            size = androidx.compose.ui.geometry.Size(width * 0.56f, height * 0.5f),
        )

        // Sparks: deterministic particles from hashed time buckets.
        if (!asleep) {
            val sparkRate = if (hot) 10f else if (ctx.charging) 8f else 4f
            val bucket = (ctx.timeSeconds * sparkRate).toLong()
            for (s in 0..3) {
                val id = bucket - s
                val u = ValueNoise.hash01(id, ctx.seed, 90)
                val v = ValueNoise.hash01(id, ctx.seed, 91)
                val life = ((ctx.timeSeconds * sparkRate) - id) / 4f // 0..1 across 4 buckets
                if (life >= 1f) continue
                val sx = c.x + (u - 0.5f) * width * 1.1f
                val sy = c.y + r * 0.3f - height * (0.75f + life * 0.6f)
                drawCircle(
                    coreColor.copy(alpha = (1f - life) * 0.8f),
                    radius = r * 0.05f * (1f - life * 0.6f) * (0.6f + v * 0.8f),
                    center = Offset(sx, sy),
                )
            }
        }

        // Eyes: bright slits in the flame body.
        val eyeGap = width * 0.22f
        val eyeY = c.y + r * 0.45f - height * 0.42f
        val eyeR = r * 0.1f
        val iris = Color(0xFF3A1408)
        with(EyeKit) {
            drawRoundEye(
                Offset(c.x - eyeGap, eyeY), eyeR,
                openness(pose.blinkLeft, pose.lidDroop),
                pose.gazeX, pose.gazeY, eyeR * 0.7f, iris, happy = pose.petLean,
            )
            drawRoundEye(
                Offset(c.x + eyeGap, eyeY), eyeR,
                openness(pose.blinkRight, pose.lidDroop),
                pose.gazeX, pose.gazeY, eyeR * 0.7f, iris, happy = pose.petLean,
            )
        }
    }
}
