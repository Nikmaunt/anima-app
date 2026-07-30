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

    private companion object {
        const val BELL_HUE = 280f

        /** Grounding geometry, in body radii (v1.1b task 2). */
        const val SHADOW_DROP = 2.35f
        const val SHADOW_HALF_W = 0.85f
        const val SHADOW_HALF_H = 0.14f
        const val CONTOUR_SHARE = 0.055f

        /** The dark casing a tentacle is drawn inside, as a share of its width. */
        const val TENTACLE_CASING = 1.7f
    }

    override fun DrawScope.render(ctx: RenderContext) {
        val pose = ctx.pose
        val c = Offset(size.width / 2f, size.height / 2f + size.minDimension * RigScale.JELLY_BIAS)
        val r = size.minDimension * 0.24f * RigScale.JELLY
        val pulse = 1f + pose.breath * 0.1f
        val bellW = r * 1.25f * pulse
        val bellH = r * (1.12f - pose.breath * 0.08f)
        val skin = Hues.bodyColor(BELL_HUE, sat = 0.45f, light = 0.68f, ctx = ctx)
        val skinDeep = Hues.bodyColor(265f, sat = 0.5f, light = 0.5f, ctx = ctx)

        // v1.1b task 2. The owner called JELLY borderline and asked whether it
        // needs the same treatment. It does, and only in one place: the bell was
        // already close to opaque (0.9 / 0.65) and read on every backdrop, but the
        // tentacles were single 0.55-alpha strokes and vanished over anything
        // bright. They are opaque now and each one is drawn inside a darker
        // casing, which is the two-tone contour idea applied to a stroke instead
        // of to a filled path. The bell keeps its glass; it gains the contour and
        // a faint occlusion below the tentacle tips — a jellyfish floats, so the
        // shadow is weaker than the ones under a standing body.
        with(Grounding) {
            drawContactShadow(
                center = Offset(c.x, c.y + r * SHADOW_DROP),
                halfWidth = r * SHADOW_HALF_W,
                halfHeight = r * SHADOW_HALF_H,
                strength = 0.6f,
            )
        }

        // Tentacles first (behind the bell).
        drawTentacles(ctx, c, r, bellW, bellH, skinDeep)

        // Bell: dome + wavy skirt whose hem ripples with noise.
        bellPath.reset()
        bellPath.moveTo(c.x - bellW, c.y + bellH * 0.35f)
        bellPath.cubicTo(
            c.x - bellW * 1.02f,
            c.y - bellH * 0.9f,
            c.x + bellW * 1.02f,
            c.y - bellH * 0.9f,
            c.x + bellW,
            c.y + bellH * 0.35f,
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
            brush =
                Grounding.verticalBody(
                    top = skin,
                    bottom = skinDeep,
                    topY = c.y - bellH,
                    bottomY = c.y + bellH * 0.5f,
                ),
        )
        with(Grounding) {
            drawTwoToneContour(bellPath, BELL_HUE, ctx, r * CONTOUR_SHARE)
        }
        // Inner organs glow — a soft heart that beats with breath.
        drawCircle(
            brush =
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.4f + 0.2f * pose.breath), Color.Transparent),
                    center = c.copy(y = c.y - bellH * 0.15f),
                    radius = r * 0.55f,
                ),
            radius = r * 0.55f,
            center = c.copy(y = c.y - bellH * 0.15f),
        )

        // Eyes inside the bell.
        val eyeGap = bellW * 0.34f
        val eyeY = c.y - bellH * 0.05f
        val eyeR = r * 0.11f
        val iris = Color(0xFF2C2440)
        with(EyeKit) {
            drawRoundEye(
                Offset(c.x - eyeGap, eyeY),
                eyeR,
                openness(pose.blinkLeft, pose.lidDroop),
                pose.gazeX,
                pose.gazeY,
                eyeR * 0.8f,
                iris,
                happy = pose.petLean,
            )
            drawRoundEye(
                Offset(c.x + eyeGap, eyeY),
                eyeR,
                openness(pose.blinkRight, pose.lidDroop),
                pose.gazeX,
                pose.gazeY,
                eyeR * 0.8f,
                iris,
                happy = pose.petLean,
            )
        }
    }

    /**
     * Five chain-lagged strands, each with its own phase so they never move in
     * unison (follow-through). Extracted in v1.1b only because the grounding work
     * pushed `render` past the LongMethod threshold.
     */
    private fun DrawScope.drawTentacles(
        ctx: RenderContext,
        c: Offset,
        r: Float,
        bellW: Float,
        bellH: Float,
        skinDeep: Color,
    ) {
        val pose = ctx.pose
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
                val sway =
                    pose.secondaryX[chainIdx] * r * 3f +
                        sin(phase + s * 0.8f) * r * 0.16f * (0.5f + pose.energy)
                val nx = rootX + fx * r * 0.24f * s + sway
                val ny = rootY + (r * 1.5f / segments) * s * (1f + pose.secondaryY[chainIdx] * 0.8f)
                tentaclePath.quadraticTo(px, py, (px + nx) / 2f, (py + ny) / 2f)
                px = nx
                py = ny
            }
            tentaclePath.lineTo(px, py)
            val strandWidth = r * (0.12f - i % 2 * 0.03f)
            drawPath(
                tentaclePath,
                color = Grounding.contour(BELL_HUE, ctx),
                style = Stroke(width = strandWidth * TENTACLE_CASING, cap = StrokeCap.Round),
            )
            drawPath(
                tentaclePath,
                color = skinDeep,
                style = Stroke(width = strandWidth, cap = StrokeCap.Round),
            )
        }
    }
}
