package app.anima.core.creature.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import app.anima.core.creature.engine.ValueNoise
import kotlin.math.sin

/**
 * Осьминожек-медуза: a translucent bell with five tentacles on the spring
 * chain — the secondary-motion showcase. The bell pulse IS the breath.
 */
class JellyRenderer : CreatureRenderer {

    private val bellPath = Path()
    private val tentaclePath = Path()

    override fun DrawScope.render(ctx: RenderContext) {
        val pose = ctx.pose
        val c = Offset(size.width / 2f, size.height / 2f - size.minDimension * 0.05f)
        val r = size.minDimension * 0.24f
        val pulse = 1f + pose.breath * 0.1f
        val bellW = r * 1.25f * pulse
        val bellH = r * (1.12f - pose.breath * 0.08f)
        val skin = Hues.bodyColor(280f, sat = 0.45f, light = 0.68f, ctx = ctx)
        val skinDeep = Hues.bodyColor(265f, sat = 0.5f, light = 0.5f, ctx = ctx)

        // Tentacles first (behind the bell): 5 strands, chain-lagged, each with
        // its own phase so they never move in unison (follow-through).
        val strands = 5
        for (i in 0 until strands) {
            val fx = (i - (strands - 1) / 2f) / ((strands - 1) / 2f) // -1..1
            val rootX = c.x + fx * bellW * 0.7f
            val rootY = c.y + bellH * 0.55f
            val phase = ctx.timeSeconds * 1.1f + i * 0.9f
            tentaclePath.reset()
            tentaclePath.moveTo(rootX, rootY)
            var px = rootX
            var py = rootY
            val segments = 3
            for (s in 1..segments) {
                val chainIdx = ((i + s) % pose.secondaryCount.coerceAtLeast(1))
                val sway = pose.secondaryX[chainIdx] * r * 3f +
                    sin(phase + s * 0.8f) * r * 0.16f * (0.5f + pose.energy)
                val nx = rootX + fx * r * 0.24f * s + sway
                val ny = rootY + (r * 1.5f / segments) * s * (1f + pose.secondaryY[chainIdx] * 0.8f)
                tentaclePath.quadraticTo(px, py, (px + nx) / 2f, (py + ny) / 2f)
                px = nx
                py = ny
            }
            tentaclePath.lineTo(px, py)
            drawPath(
                tentaclePath,
                color = skinDeep.copy(alpha = 0.55f),
                style = Stroke(width = r * (0.12f - i % 2 * 0.03f), cap = StrokeCap.Round),
            )
        }

        // Bell: dome + wavy skirt whose hem ripples with noise.
        bellPath.reset()
        bellPath.moveTo(c.x - bellW, c.y + bellH * 0.35f)
        bellPath.cubicTo(
            c.x - bellW * 1.02f, c.y - bellH * 0.9f,
            c.x + bellW * 1.02f, c.y - bellH * 0.9f,
            c.x + bellW, c.y + bellH * 0.35f,
        )
        // Skirt hem: 6 scallops rippled by noise.
        val scallops = 6
        for (i in scallops downTo 0) {
            val t = i / scallops.toFloat()
            val x = c.x - bellW + t * bellW * 2f
            val ripple = ValueNoise.noise(t * 4f + ctx.timeSeconds * 0.8f, ctx.seed, 61) * r * 0.08f
            val y = c.y + bellH * 0.35f + sin(t * Math.PI.toFloat() * scallops) * r * 0.07f + ripple
            bellPath.lineTo(x, y)
        }
        bellPath.close()
        drawPath(
            bellPath,
            brush = Brush.verticalGradient(
                listOf(skin.copy(alpha = 0.9f), skinDeep.copy(alpha = 0.65f)),
                startY = c.y - bellH, endY = c.y + bellH * 0.5f,
            ),
        )
        // Inner organs glow — a soft heart that beats with breath.
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.4f + 0.2f * pose.breath), Color.Transparent),
                center = c.copy(y = c.y - bellH * 0.15f), radius = r * 0.55f,
            ),
            radius = r * 0.55f, center = c.copy(y = c.y - bellH * 0.15f),
        )

        // Eyes inside the bell.
        val eyeGap = bellW * 0.34f
        val eyeY = c.y - bellH * 0.05f
        val eyeR = r * 0.11f
        val iris = Color(0xFF2C2440)
        with(EyeKit) {
            drawRoundEye(
                Offset(c.x - eyeGap, eyeY), eyeR,
                openness(pose.blinkLeft, pose.lidDroop),
                pose.gazeX, pose.gazeY, eyeR * 0.8f, iris, happy = pose.petLean,
            )
            drawRoundEye(
                Offset(c.x + eyeGap, eyeY), eyeR,
                openness(pose.blinkRight, pose.lidDroop),
                pose.gazeX, pose.gazeY, eyeR * 0.8f, iris, happy = pose.petLean,
            )
        }
    }
}
