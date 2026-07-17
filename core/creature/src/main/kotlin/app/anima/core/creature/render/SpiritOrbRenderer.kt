package app.anima.core.creature.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import app.anima.core.creature.engine.ValueNoise
import app.anima.core.model.Mood
import kotlin.math.cos
import kotlin.math.sin

/**
 * Сфера-дух: a soft glowing orb of liquid glass with metaball satellites.
 * The AGSL glow is its skin; satellites ride the secondary spring chain so
 * they lag and settle like drops of the same liquid.
 */
class SpiritOrbRenderer : CreatureRenderer {
    private val glow = GlowSkin()

    override fun DrawScope.render(ctx: RenderContext) {
        val pose = ctx.pose
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension * 0.26f * (1f + pose.breath * 0.05f)
        val hue = 190f // pale cyan spirit
        val core = Hues.bodyColor(hue, sat = 0.55f, light = 0.72f, ctx = ctx)
        val rim = Hues.bodyColor(hue + 24f, sat = 0.65f, light = 0.6f, ctx = ctx)

        with(glow) {
            drawGlow(
                center = c,
                radius = r,
                color = rim,
                intensity = 0.45f + 0.4f * pose.energy + (if (ctx.mood == Mood.EATING) 0.25f else 0f),
                time = ctx.timeSeconds,
            )
        }

        // Metaball satellites: three droplets orbiting on chain lag.
        val satellites = 3
        for (i in 0 until satellites) {
            val chainIdx = (i * 2).coerceAtMost(pose.secondaryCount - 1)
            val phase = ctx.timeSeconds * (0.25f + i * 0.09f) + i * 2.1f
            val orbit = r * (1.45f + 0.18f * sin(phase * 0.7f))
            val sx = c.x + cos(phase) * orbit + pose.secondaryX[chainIdx] * r * 2f
            val sy = c.y + sin(phase) * orbit * 0.6f + pose.secondaryY[chainIdx] * r * 2f
            val sr = r * (0.16f - i * 0.03f) * (0.7f + 0.6f * pose.energy)
            // Neck: a soft bridge toward the core when close (metaball feel).
            val toward = Offset((c.x + sx) / 2f, (c.y + sy) / 2f)
            drawCircle(core.copy(alpha = 0.10f), radius = sr * 1.8f, center = toward)
            drawCircle(
                brush =
                    Brush.radialGradient(
                        listOf(core.copy(alpha = 0.9f), rim.copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(sx, sy),
                        radius = sr * 1.9f,
                    ),
                radius = sr * 1.9f,
                center = Offset(sx, sy),
            )
        }

        // Liquid-glass body: layered translucent discs with a bright caustic.
        drawCircle(
            brush =
                Brush.radialGradient(
                    listOf(
                        core.copy(alpha = 0.95f),
                        core.copy(alpha = 0.55f),
                        rim.copy(alpha = 0.30f),
                    ),
                    center = c.copy(y = c.y - r * 0.25f),
                    radius = r * 1.4f,
                ),
            radius = r,
            center = c,
        )
        // Inner wobble highlight (living caustic).
        val wob = ValueNoise.fbm2(ctx.timeSeconds * 0.4f, ctx.seed, 51)
        drawCircle(
            color = Color.White.copy(alpha = 0.35f + 0.1f * pose.breath),
            radius = r * 0.32f,
            center = Offset(c.x - r * 0.3f + wob * r * 0.12f, c.y - r * 0.38f),
        )

        // Eyes float inside the light.
        val eyeGap = r * 0.42f
        val eyeY = c.y - r * 0.05f
        val eyeR = r * 0.11f
        val open = EyeKit.openness(pose.blinkLeft, pose.lidDroop)
        val openR = EyeKit.openness(pose.blinkRight, pose.lidDroop)
        val iris = Color(0xFF20313E)
        with(EyeKit) {
            drawRoundEye(
                Offset(c.x - eyeGap, eyeY),
                eyeR,
                open,
                pose.gazeX,
                pose.gazeY,
                gazeRange = eyeR * 0.9f,
                irisColor = iris,
                happy = pose.petLean,
            )
            drawRoundEye(
                Offset(c.x + eyeGap, eyeY),
                eyeR,
                openR,
                pose.gazeX,
                pose.gazeY,
                gazeRange = eyeR * 0.9f,
                irisColor = iris,
                happy = pose.petLean,
            )
        }
    }
}
