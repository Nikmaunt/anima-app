package app.anima.core.creature.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import app.anima.core.creature.engine.ValueNoise
import app.anima.core.model.Mood
import kotlin.math.sin

/**
 * Мотылёк: a night flyer. Wings flap on a rate tied to energy (slow fold when
 * asleep), antennae ride the spring chain, and after dark the wings carry a
 * luminescent dust that shimmers — this creature is most alive at night.
 */
class MothRenderer : CreatureRenderer {
    private val wingPath = Path()

    private companion object {
        const val WING_HUE = 280f

        /** Grounding geometry, in body radii (v1.1b task 2). */
        const val SHADOW_DROP = 1.15f
        const val SHADOW_HALF_W = 0.55f
        const val SHADOW_HALF_H = 0.13f
        const val CONTOUR_SHARE = 0.07f
    }

    override fun DrawScope.render(ctx: RenderContext) {
        val pose = ctx.pose
        val c = Offset(size.width / 2f, size.height / 2f + size.minDimension * RigScale.MOTH_BIAS)
        val r = size.minDimension * 0.22f * RigScale.MOTH
        val asleep = ctx.mood == Mood.ASLEEP
        val dust = Hues.bodyColor(265f, sat = 0.4f, light = 0.72f, ctx = ctx)
        val bodyC = Hues.bodyColor(30f, sat = 0.25f, light = 0.45f, ctx = ctx)

        // v1.1b task 2: the wings were alpha 0.9 / 0.5 / 0.75 and the body a
        // 0.44r-wide capsule, so over a photo the moth was a thin brown stripe
        // with the wallpaper showing through where the wings should be. They are
        // opaque now and shaded from the leading edge inward, and each wing
        // carries a two-tone contour. The luminescent dust stays exactly as it
        // was — it was never the problem; it was the only thing there.
        val wingLit = Hues.bodyColor(WING_HUE, sat = 0.26f, light = if (ctx.night) 0.66f else 0.74f, ctx = ctx)
        val wingDeep = Hues.bodyColor(WING_HUE - 12f, sat = 0.36f, light = if (ctx.night) 0.4f else 0.48f, ctx = ctx)

        with(Grounding) {
            drawContactShadow(
                center = Offset(c.x, c.y + r * SHADOW_DROP),
                halfWidth = r * SHADOW_HALF_W,
                halfHeight = r * SHADOW_HALF_H,
            )
        }

        // Wing flap: sine on energy; asleep = folded (flap ≈ 0.15).
        val flapRate = if (asleep) 0.4f else 2.2f + pose.energy * 2.6f
        val flapRaw = (sin(ctx.timeSeconds * flapRate * Math.PI.toFloat()) + 1f) / 2f
        val flap = if (asleep) 0.12f else 0.25f + flapRaw * 0.75f
        // Flourish 2: wing shimmer — a full slow beat.
        val shimmerBeat =
            if (pose.flourishPhase > 0f && pose.flourishKind == 2) {
                sin(pose.flourishPhase * Math.PI.toFloat()) * 0.4f
            } else {
                0f
            }

        for (side in intArrayOf(-1, 1)) {
            val spread = (flap + shimmerBeat).coerceIn(0.1f, 1.1f)
            scale(scaleX = side * spread, scaleY = 1f, pivot = c) {
                drawWingPair(ctx, c, r, wingLit, wingDeep, dust, asleep)
            }
        }

        // Body: fuzzy segmented capsule. Opaque, and wider than it was — at
        // 0.44r it was a stripe, and it is the part of a moth you actually
        // recognise.
        val bodyW = r * 0.56f
        val bodyH = r * (1.35f + pose.breath * 0.08f)
        drawOval(
            brush =
                Grounding.verticalBody(
                    top = Hues.bodyColor(30f, sat = 0.28f, light = 0.56f, ctx = ctx),
                    bottom = Hues.bodyColor(24f, sat = 0.3f, light = 0.3f, ctx = ctx),
                    topY = c.y - r * 0.55f,
                    bottomY = c.y - r * 0.55f + bodyH,
                ),
            topLeft = Offset(c.x - bodyW / 2f, c.y - r * 0.55f),
            size = Size(bodyW, bodyH),
        )
        drawOval(
            Grounding.contour(30f, ctx),
            topLeft = Offset(c.x - bodyW / 2f, c.y - r * 0.55f),
            size = Size(bodyW, bodyH),
            style = Stroke(width = r * CONTOUR_SHARE * 0.8f),
        )

        // Antennae: feathered curves riding chain links 0/1.
        for (side in intArrayOf(-1, 1)) {
            val link = if (side < 0) 0 else 1
            val tipX = c.x + side * r * 0.55f + pose.secondaryX[link] * r * 2f
            val tipY = c.y - r * (1.1f + 0.15f * pose.energy) + pose.secondaryY[link] * r * 1.5f
            drawLine(
                bodyC,
                start = Offset(c.x + side * r * 0.08f, c.y - r * 0.5f),
                end = Offset(tipX, tipY),
                strokeWidth = r * 0.05f,
                cap = StrokeCap.Round,
            )
            drawCircle(dust, radius = r * 0.07f, center = Offset(tipX, tipY))
        }

        // Eyes on the head segment.
        val eyeGap = r * 0.2f
        val eyeY = c.y - r * 0.42f
        val eyeR = r * 0.1f
        with(EyeKit) {
            drawRoundEye(
                Offset(c.x - eyeGap, eyeY),
                eyeR,
                openness(pose.blinkLeft, pose.lidDroop),
                pose.gazeX,
                pose.gazeY,
                eyeR * 0.7f,
                irisColor = Color(0xFF1E1830),
                happy = pose.petLean,
            )
            drawRoundEye(
                Offset(c.x + eyeGap, eyeY),
                eyeR,
                openness(pose.blinkRight, pose.lidDroop),
                pose.gazeX,
                pose.gazeY,
                eyeR * 0.7f,
                irisColor = Color(0xFF1E1830),
                happy = pose.petLean,
            )
        }
    }

    /**
     * One side's upper and lower wing, drawn in the mirrored/scaled space the
     * caller set up. Extracted in v1.1b only because the grounding work pushed
     * `render` past the LongMethod threshold; the drawing is unchanged apart from
     * the opaque fills and the contour.
     */
    private fun DrawScope.drawWingPair(
        ctx: RenderContext,
        c: Offset,
        r: Float,
        wingLit: Color,
        wingDeep: Color,
        dust: Color,
        asleep: Boolean,
    ) {
        // Upper wing.
        wingPath.reset()
        wingPath.moveTo(c.x, c.y - r * 0.3f)
        wingPath.cubicTo(
            c.x + r * 1.7f,
            c.y - r * 1.5f,
            c.x + r * 2.1f,
            c.y - r * 0.2f,
            c.x + r * 0.6f,
            c.y + r * 0.15f,
        )
        wingPath.close()
        drawPath(
            wingPath,
            brush =
                Brush.radialGradient(
                    listOf(wingLit, wingDeep),
                    center = Offset(c.x + r, c.y - r * 0.5f),
                    radius = r * 1.8f,
                ),
        )
        with(Grounding) {
            drawTwoToneContour(wingPath, WING_HUE, ctx, r * CONTOUR_SHARE)
        }

        // Lower wing.
        wingPath.reset()
        wingPath.moveTo(c.x, c.y + r * 0.05f)
        wingPath.cubicTo(
            c.x + r * 1.3f,
            c.y + r * 0.7f,
            c.x + r * 0.9f,
            c.y + r * 1.5f,
            c.x + r * 0.15f,
            c.y + r * 0.75f,
        )
        wingPath.close()
        drawPath(wingPath, wingDeep)
        with(Grounding) {
            drawTwoToneContour(wingPath, WING_HUE, ctx, r * CONTOUR_SHARE)
        }

        // Wing eye-spot.
        drawCircle(
            dust.copy(alpha = 0.5f),
            radius = r * 0.22f,
            center = Offset(c.x + r * 1.05f, c.y - r * 0.55f),
        )

        // Night dust: luminescent specks along the upper wing edge.
        if (ctx.night && !asleep) {
            for (i in 0..4) {
                val u = ValueNoise.hash01((i + (ctx.timeSeconds * 0.5f).toLong() * 7).toLong(), ctx.seed, 95)
                val t = i / 4f
                drawCircle(
                    Color.White.copy(alpha = 0.25f + 0.35f * u),
                    radius = r * 0.035f,
                    center = Offset(c.x + r * (0.5f + t * 1.2f), c.y - r * (0.4f + u * 0.8f)),
                )
            }
        }
    }
}
