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
 * Мотылёк: a night flyer.
 *
 * ## v1.1c task 4.2 — it was a butterfly, and one line of it said so
 *
 * The v1.1b contact sheet passed this body on readability and failed it on
 * recognisability: pale lavender wings held **up** in two rounded lobes, over a
 * fat dark body, with **knobbed antennae**. Knobbed antennae are the textbook
 * butterfly diagnostic — the single feature a field guide uses to tell the two
 * apart — so the drawing was not merely butterfly-ish, it was carrying the
 * identifying mark of the wrong order. Asleep, the wings squeezed to a twelfth
 * of their span and what remained was a brown capsule with two sticks.
 *
 * Four things changed, in descending order of how much each one is worth:
 *
 *  1. **Plumose antennae.** Feathered combs, no knobs. This is the recognition
 *     lever; everything else is support.
 *  2. **A delta silhouette.** Forewings sweep out *and back*, tip below the
 *     shoulder, with a scalloped trailing edge; the hindwing tucks under. A
 *     butterfly holds its wings up and a moth holds them out — the outline
 *     alone now answers the question.
 *  3. **A furred thorax and a banded, tapering abdomen** instead of one smooth
 *     capsule. The fur is what a moth feels like.
 *  4. **Dusty warm grey-brown**, not lavender pastel, with a pale band across
 *     the forewing.
 *
 * ## The light
 *
 * The strongest thing about a moth is not its shape, it is that it flies at
 * light — and the screen it lives on IS the light. That is said by *lighting
 * the creature*, not by drawing a lamp: a warm rim rides every upward-facing
 * edge, brightest at night, and the eyes carry the same warm point. Painting an
 * actual light source would fill the frame, and the frame is transparent by
 * contract (`Grounding`, and the glow-overflow regression the v1.1 handoff
 * records).
 *
 * Asleep, the wings fold into a roof over the abdomen instead of collapsing to
 * a sliver: a moth at rest is a triangle, and a triangle is still a moth.
 */
class MothRenderer : CreatureRenderer {
    private val wingPath = Path()
    private val abdomenPath = Path()

    private companion object {
        /** Dusty brown-grey; the wings are not a colour anyone would call pretty. */
        const val WING_HUE = 32f

        /** Grounding geometry, in body radii (v1.1b task 2). */
        const val SHADOW_DROP = 1.25f
        const val SHADOW_HALF_W = 0.75f
        const val SHADOW_HALF_H = 0.13f
        const val CONTOUR_SHARE = 0.06f

        /** Warm light the creature is flying at — the screen itself. */
        const val LIGHT_HUE = 44f

        /** Wing spread when asleep: a roof, not a sliver (it used to be 0.12). */
        const val ASLEEP_SPREAD = 0.72f

        const val ANTENNA_BARBS = 11

        /** How far each barb leans toward the tip, in barb lengths. */
        const val BARB_SWEEP = 0.55f
    }

    override fun DrawScope.render(ctx: RenderContext) {
        val pose = ctx.pose
        val c = Offset(size.width / 2f, size.height / 2f + size.minDimension * RigScale.MOTH_BIAS)
        val r = size.minDimension * 0.22f * RigScale.MOTH
        val asleep = ctx.mood == Mood.ASLEEP

        val wingLit = Hues.bodyColor(WING_HUE, sat = 0.26f, light = if (ctx.night) 0.48f else 0.57f, ctx = ctx)
        val wingDeep = Hues.bodyColor(WING_HUE - 10f, sat = 0.30f, light = if (ctx.night) 0.34f else 0.43f, ctx = ctx)
        val band = Hues.bodyColor(WING_HUE + 6f, sat = 0.18f, light = if (ctx.night) 0.72f else 0.82f, ctx = ctx)
        val fur = Hues.bodyColor(26f, sat = 0.34f, light = 0.44f, ctx = ctx)
        val furLit = Hues.bodyColor(30f, sat = 0.28f, light = 0.60f, ctx = ctx)
        // The light it is flying at. Brighter after dark, because that is when a
        // moth and a phone screen are actually in the same story.
        val lit = Hues.bodyColor(LIGHT_HUE, sat = 0.45f, light = if (ctx.night) 0.88f else 0.80f, ctx = ctx)

        with(Grounding) {
            drawContactShadow(
                center = Offset(c.x, c.y + r * SHADOW_DROP),
                halfWidth = r * SHADOW_HALF_W,
                halfHeight = r * SHADOW_HALF_H,
            )
        }

        // Wing beat: fast and shallow, the way a moth's is. Asleep it settles
        // into the roof rather than folding flat.
        val flapRate = if (asleep) 0.4f else 3.0f + pose.energy * 3.0f
        val flapRaw = (sin(ctx.timeSeconds * flapRate * Math.PI.toFloat()) + 1f) / 2f
        val flap = if (asleep) ASLEEP_SPREAD else 0.62f + flapRaw * 0.42f
        // Flourish 2: wing shimmer — a full slow beat.
        val shimmerBeat =
            if (pose.flourishPhase > 0f && pose.flourishKind == 2) {
                sin(pose.flourishPhase * Math.PI.toFloat()) * 0.22f
            } else {
                0f
            }

        for (side in intArrayOf(-1, 1)) {
            val spread = (flap + shimmerBeat).coerceIn(0.4f, 1.1f)
            scale(scaleX = side * spread, scaleY = 1f, pivot = c) {
                drawWingPair(ctx, c, r, wingLit, wingDeep, band, lit, asleep)
            }
        }

        drawBody(ctx, c, r, fur, furLit, lit, pose.breath)
        drawAntennae(c, r, fur, pose)
        drawFace(c, r, pose, lit)
    }

    /**
     * Thorax and abdomen as two things rather than one capsule: a moth's thorax
     * is a furry block and its abdomen is a banded taper, and the join between
     * them is most of what makes the animal read as an insect.
     */
    private fun DrawScope.drawBody(
        ctx: RenderContext,
        c: Offset,
        r: Float,
        fur: Color,
        furLit: Color,
        lit: Color,
        breath: Float,
    ) {
        // Abdomen: taper, drawn under the thorax so the fur overlaps its top.
        val abdomenTop = c.y - r * 0.05f
        val abdomenBottom = c.y + r * (0.98f + breath * 0.06f)
        val halfTop = r * 0.23f
        val halfEnd = r * 0.09f
        abdomenPath.reset()
        abdomenPath.moveTo(c.x - halfTop, abdomenTop)
        abdomenPath.cubicTo(
            c.x - halfTop,
            abdomenTop + (abdomenBottom - abdomenTop) * 0.7f,
            c.x - halfEnd,
            abdomenBottom - r * 0.1f,
            c.x,
            abdomenBottom,
        )
        abdomenPath.cubicTo(
            c.x + halfEnd,
            abdomenBottom - r * 0.1f,
            c.x + halfTop,
            abdomenTop + (abdomenBottom - abdomenTop) * 0.7f,
            c.x + halfTop,
            abdomenTop,
        )
        abdomenPath.close()
        drawPath(
            abdomenPath,
            brush =
                Grounding.verticalBody(
                    top = furLit,
                    bottom = Hues.bodyColor(22f, sat = 0.36f, light = 0.26f, ctx = ctx),
                    topY = abdomenTop,
                    bottomY = abdomenBottom,
                ),
        )
        // Segment bands: three darker rings, the insect tell.
        for (i in 1..3) {
            val t = i / 4f
            val y = abdomenTop + (abdomenBottom - abdomenTop) * t
            val half = halfTop + (halfEnd - halfTop) * t
            drawLine(
                Hues.bodyColor(20f, sat = 0.4f, light = 0.22f, ctx = ctx).copy(alpha = 0.55f),
                start = Offset(c.x - half * 0.85f, y),
                end = Offset(c.x + half * 0.85f, y),
                strokeWidth = r * 0.045f,
                cap = StrokeCap.Round,
            )
        }
        with(Grounding) {
            drawTwoToneContour(abdomenPath, 26f, ctx, r * CONTOUR_SHARE * 0.8f)
        }

        // Thorax: a furry block. The fur is strokes, not a texture — at widget
        // size a texture is noise and a dozen strokes are still a silhouette.
        val thoraxW = r * 0.66f
        val thoraxH = r * 0.7f
        val thoraxTop = c.y - r * 0.5f
        drawOval(
            brush = Grounding.verticalBody(furLit, fur, thoraxTop, thoraxTop + thoraxH),
            topLeft = Offset(c.x - thoraxW / 2f, thoraxTop),
            size = Size(thoraxW, thoraxH),
        )
        for (i in 0 until 9) {
            val t = i / 8f
            val x = c.x - thoraxW * 0.42f + thoraxW * 0.84f * t
            val lean = (t - 0.5f) * r * 0.12f
            drawLine(
                if (i % 2 == 0) furLit else fur,
                start = Offset(x, thoraxTop + thoraxH * 0.12f),
                end = Offset(x + lean, thoraxTop + thoraxH * 0.9f),
                strokeWidth = r * 0.05f,
                cap = StrokeCap.Round,
            )
        }
        // The light, on the top of the thorax.
        drawLine(
            lit.copy(alpha = 0.55f),
            start = Offset(c.x - thoraxW * 0.3f, thoraxTop + thoraxH * 0.16f),
            end = Offset(c.x + thoraxW * 0.3f, thoraxTop + thoraxH * 0.16f),
            strokeWidth = r * 0.07f,
            cap = StrokeCap.Round,
        )
    }

    /**
     * Plumose antennae — the feathered combs that separate a moth from a
     * butterfly. The old ones ended in a ball, which is precisely the butterfly
     * feature; that one detail was doing more damage than the wing shape.
     */
    private fun DrawScope.drawAntennae(
        c: Offset,
        r: Float,
        fur: Color,
        pose: app.anima.core.creature.engine.CreaturePose,
    ) {
        for (side in intArrayOf(-1, 1)) {
            val link = if (side < 0) 0 else 1
            val baseX = c.x + side * r * 0.14f
            val baseY = c.y - r * 0.62f
            val tipX = c.x + side * r * 0.78f + pose.secondaryX[link] * r * 1.6f
            val tipY = c.y - r * (1.18f + 0.12f * pose.energy) + pose.secondaryY[link] * r * 1.2f
            // Shaft.
            drawLine(
                fur,
                start = Offset(baseX, baseY),
                end = Offset(tipX, tipY),
                strokeWidth = r * 0.045f,
                cap = StrokeCap.Round,
            )
            // Barbs: many, short, and swept toward the tip, so a pair reads as a
            // feather. Long perpendicular barbs were the first attempt and read
            // as a television aerial.
            val dx = tipX - baseX
            val dy = tipY - baseY
            val len =
                kotlin.math
                    .sqrt((dx * dx + dy * dy).toDouble())
                    .toFloat()
                    .coerceAtLeast(0.0001f)
            val ux = dx / len
            val uy = dy / len
            for (i in 1..ANTENNA_BARBS) {
                val t = i / (ANTENNA_BARBS + 1f)
                val px = baseX + dx * t
                val py = baseY + dy * t
                // Widest in the middle: a comb, not a cross.
                val taper = kotlin.math.sin(t * Math.PI).toFloat()
                val barb = r * 0.13f * (0.45f + 0.55f * taper)
                for (dir in intArrayOf(-1, 1)) {
                    drawLine(
                        fur,
                        start = Offset(px, py),
                        end =
                            Offset(
                                px + dir * -uy * barb + ux * barb * BARB_SWEEP,
                                py + dir * ux * barb + uy * barb * BARB_SWEEP,
                            ),
                        strokeWidth = r * 0.018f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }

    private fun DrawScope.drawFace(
        c: Offset,
        r: Float,
        pose: app.anima.core.creature.engine.CreaturePose,
        lit: Color,
    ) {
        val eyeGap = r * 0.19f
        val eyeY = c.y - r * 0.5f
        val eyeR = r * 0.105f
        with(EyeKit) {
            drawRoundEye(
                Offset(c.x - eyeGap, eyeY),
                eyeR,
                openness(pose.blinkLeft, pose.lidDroop),
                pose.gazeX,
                pose.gazeY,
                eyeR * 0.7f,
                irisColor = Color(0xFF241A12),
                happy = pose.petLean,
            )
            drawRoundEye(
                Offset(c.x + eyeGap, eyeY),
                eyeR,
                openness(pose.blinkRight, pose.lidDroop),
                pose.gazeX,
                pose.gazeY,
                eyeR * 0.7f,
                irisColor = Color(0xFF241A12),
                happy = pose.petLean,
            )
        }
        // The light it is looking at, caught in both eyes at the same point.
        val catch = EyeKit.openness(pose.blinkLeft, pose.lidDroop)
        if (catch > 0.3f) {
            for (side in intArrayOf(-1, 1)) {
                drawCircle(
                    lit.copy(alpha = 0.85f * catch),
                    radius = eyeR * 0.3f,
                    center = Offset(c.x + side * eyeGap - eyeR * 0.22f, eyeY - eyeR * 0.3f),
                )
            }
        }
    }

    /**
     * One side's forewing and hindwing, drawn in the mirrored/scaled space the
     * caller set up. The forewing sweeps out and DOWN — tip below the shoulder —
     * which is the whole difference between this outline and a butterfly's.
     */
    @Suppress("LongParameterList")
    private fun DrawScope.drawWingPair(
        ctx: RenderContext,
        c: Offset,
        r: Float,
        wingLit: Color,
        wingDeep: Color,
        band: Color,
        lit: Color,
        asleep: Boolean,
    ) {
        drawHindwing(ctx, c, r, wingDeep)
        drawForewing(ctx, c, r, wingLit, wingDeep, band, lit)
        drawNightDust(ctx, c, r, lit, asleep)
    }

    /** Behind and under the forewing; small enough not to read as a boot. */
    private fun DrawScope.drawHindwing(
        ctx: RenderContext,
        c: Offset,
        r: Float,
        wingDeep: Color,
    ) {
        wingPath.reset()
        wingPath.moveTo(c.x + r * 0.07f, c.y + r * 0.28f)
        wingPath.cubicTo(
            c.x + r * 0.50f,
            c.y + r * 0.44f,
            c.x + r * 0.62f,
            c.y + r * 0.70f,
            c.x + r * 0.36f,
            c.y + r * 0.88f,
        )
        wingPath.cubicTo(
            c.x + r * 0.26f,
            c.y + r * 0.94f,
            c.x + r * 0.14f,
            c.y + r * 0.88f,
            c.x + r * 0.07f,
            c.y + r * 0.72f,
        )
        wingPath.close()
        drawPath(wingPath, wingDeep)
        with(Grounding) {
            drawTwoToneContour(wingPath, WING_HUE, ctx, r * CONTOUR_SHARE)
        }
    }

    /**
     * The delta. Built once into [wingPath] by [buildForewing] and reused for
     * fill, contour and lit edge, because three hand-copied versions of the same
     * eight control points is how a shape drifts out of alignment with itself.
     */
    private fun buildForewing(
        c: Offset,
        r: Float,
    ) {
        wingPath.reset()
        wingPath.moveTo(c.x + r * 0.06f, c.y - r * 0.30f)
        wingPath.cubicTo(
            c.x + r * 0.56f,
            c.y - r * 0.40f,
            c.x + r * 1.06f,
            c.y - r * 0.20f,
            c.x + r * 1.34f,
            c.y + r * 0.20f,
        )
        // Scalloped trailing edge back toward the body.
        wingPath.cubicTo(
            c.x + r * 1.10f,
            c.y + r * 0.42f,
            c.x + r * 0.88f,
            c.y + r * 0.40f,
            c.x + r * 0.70f,
            c.y + r * 0.62f,
        )
        wingPath.cubicTo(
            c.x + r * 0.54f,
            c.y + r * 0.78f,
            c.x + r * 0.38f,
            c.y + r * 0.56f,
            c.x + r * 0.14f,
            c.y + r * 0.70f,
        )
        wingPath.close()
    }

    @Suppress("LongParameterList")
    private fun DrawScope.drawForewing(
        ctx: RenderContext,
        c: Offset,
        r: Float,
        wingLit: Color,
        wingDeep: Color,
        band: Color,
        lit: Color,
    ) {
        buildForewing(c, r)
        drawPath(
            wingPath,
            brush =
                Brush.linearGradient(
                    listOf(wingLit, wingDeep),
                    start = Offset(c.x + r * 0.6f, c.y - r * 0.5f),
                    end = Offset(c.x + r * 1.1f, c.y + r * 0.9f),
                ),
        )

        // The pale band across the forewing: the marking a moth has where a
        // butterfly would carry an eye-spot. Quiet, or the wing turns to ceramic.
        wingPath.reset()
        wingPath.moveTo(c.x + r * 0.20f, c.y + r * 0.06f)
        wingPath.cubicTo(
            c.x + r * 0.58f,
            c.y - r * 0.06f,
            c.x + r * 0.94f,
            c.y + r * 0.02f,
            c.x + r * 1.20f,
            c.y + r * 0.28f,
        )
        wingPath.cubicTo(
            c.x + r * 0.92f,
            c.y + r * 0.16f,
            c.x + r * 0.56f,
            c.y + r * 0.10f,
            c.x + r * 0.22f,
            c.y + r * 0.20f,
        )
        wingPath.close()
        drawPath(wingPath, band.copy(alpha = 0.20f))

        buildForewing(c, r)
        with(Grounding) {
            drawTwoToneContour(wingPath, WING_HUE, ctx, r * CONTOUR_SHARE)
        }
        // The leading edge, catching the light it is flying at.
        drawPath(
            Path().apply {
                moveTo(c.x + r * 0.06f, c.y - r * 0.30f)
                cubicTo(
                    c.x + r * 0.56f,
                    c.y - r * 0.40f,
                    c.x + r * 1.06f,
                    c.y - r * 0.20f,
                    c.x + r * 1.34f,
                    c.y + r * 0.20f,
                )
            },
            color = lit.copy(alpha = if (ctx.night) 0.8f else 0.55f),
            style = Stroke(width = r * 0.075f, cap = StrokeCap.Round),
        )
    }

    /** Specks coming off the wing, drifting up toward whatever it is flying at. */
    private fun DrawScope.drawNightDust(
        ctx: RenderContext,
        c: Offset,
        r: Float,
        lit: Color,
        asleep: Boolean,
    ) {
        if (!ctx.night || asleep) return
        for (i in 0..4) {
            val u = ValueNoise.hash01((i + (ctx.timeSeconds * 0.5f).toLong() * 7).toLong(), ctx.seed, 95)
            val t = i / 4f
            drawCircle(
                lit.copy(alpha = 0.2f + 0.4f * u),
                radius = r * 0.033f,
                center = Offset(c.x + r * (0.6f + t * 1.1f), c.y - r * (0.5f + u * 0.9f)),
            )
        }
    }
}
