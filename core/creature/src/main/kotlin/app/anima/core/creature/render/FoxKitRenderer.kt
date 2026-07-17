package app.anima.core.creature.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import app.anima.core.model.Mood

/**
 * Кот-лисёнок: a minimal kit built from three shapes — body blob, head, and
 * two antenna-ears that ride the spring chain, perking on events and wilting
 * when sleepy. Deliberately flat and graphic against the glow concepts.
 */
class FoxKitRenderer : CreatureRenderer {
    private val earPath = Path()
    private val tailPath = Path()

    override fun DrawScope.render(ctx: RenderContext) {
        val pose = ctx.pose
        val c = Offset(size.width / 2f, size.height / 2f + size.minDimension * 0.04f)
        val bodyR = size.minDimension * 0.24f
        val fur = Hues.bodyColor(24f, sat = 0.68f, light = 0.62f, ctx = ctx)
        val furDeep = Hues.bodyColor(18f, sat = 0.62f, light = 0.42f, ctx = ctx)
        val cream = Hues.bodyColor(38f, sat = 0.5f, light = 0.86f, ctx = ctx)

        // Tail: a fat comma that follows the last chain link.
        val tailSwing = pose.secondaryX[(pose.secondaryCount - 1).coerceAtLeast(0)] * bodyR * 4f
        tailPath.reset()
        tailPath.moveTo(c.x + bodyR * 0.5f, c.y + bodyR * 0.75f)
        tailPath.cubicTo(
            c.x + bodyR * 1.6f + tailSwing,
            c.y + bodyR * 0.9f,
            c.x + bodyR * 1.9f + tailSwing * 1.4f,
            c.y - bodyR * 0.1f,
            c.x + bodyR * 1.35f + tailSwing,
            c.y - bodyR * 0.55f,
        )
        tailPath.cubicTo(
            c.x + bodyR * 1.5f + tailSwing,
            c.y + bodyR * 0.5f,
            c.x + bodyR * 1.1f,
            c.y + bodyR * 0.95f,
            c.x + bodyR * 0.4f,
            c.y + bodyR * 1.0f,
        )
        drawPath(tailPath, furDeep)
        drawCircle(cream, radius = bodyR * 0.22f, center = Offset(c.x + bodyR * 1.38f + tailSwing, c.y - bodyR * 0.42f))

        // Body: one soft blob, breath-inflated.
        val breathR = bodyR * (1f + pose.breath * 0.06f)
        drawOval(
            brush = Brush.verticalGradient(listOf(fur, furDeep), startY = c.y - breathR, endY = c.y + breathR * 1.3f),
            topLeft = Offset(c.x - breathR * 1.05f, c.y - breathR * 0.7f),
            size =
                androidx.compose.ui.geometry
                    .Size(breathR * 2.1f, breathR * 1.75f),
        )

        // Head.
        val headR = bodyR * 0.78f
        val headC = Offset(c.x, c.y - bodyR * 0.85f)
        drawCircle(fur, radius = headR, center = headC)
        // Muzzle patch.
        drawOval(
            cream,
            topLeft = Offset(headC.x - headR * 0.5f, headC.y + headR * 0.05f),
            size =
                androidx.compose.ui.geometry
                    .Size(headR, headR * 0.75f),
        )

        // Antenna-ears: triangles whose tips ride chain links 0 and 1.
        val earPerk =
            when (ctx.mood) {
                Mood.SLEEPY, Mood.ASLEEP -> 0.35f
                Mood.BORED -> 0.6f
                Mood.ANXIOUS -> 1.15f
                else -> 1f
            } * (0.85f + 0.3f * pose.energy)
        // Flourish 0: ear flick.
        val flick =
            if (pose.flourishPhase > 0f && pose.flourishKind == 0) {
                kotlin.math.sin(pose.flourishPhase * Math.PI.toFloat() * 3f) * 0.25f
            } else {
                0f
            }
        val earLagL = pose.secondaryX[0] * headR * 1.6f
        val earLagR = pose.secondaryX[1] * headR * 1.6f
        drawEar(headC, headR, side = -1f, perk = earPerk, flick = flick, lagX = earLagL, fur = furDeep, inner = cream)
        drawEar(headC, headR, side = 1f, perk = earPerk, flick = -flick, lagX = earLagR, fur = furDeep, inner = cream)

        // Eyes + nose.
        val eyeGap = headR * 0.42f
        val eyeY = headC.y - headR * 0.05f
        val eyeR = headR * 0.13f
        val iris = Color(0xFF29211C)
        with(EyeKit) {
            drawRoundEye(
                Offset(headC.x - eyeGap, eyeY),
                eyeR,
                openness(pose.blinkLeft, pose.lidDroop),
                pose.gazeX,
                pose.gazeY,
                eyeR * 0.8f,
                iris,
                happy = pose.petLean,
            )
            drawRoundEye(
                Offset(headC.x + eyeGap, eyeY),
                eyeR,
                openness(pose.blinkRight, pose.lidDroop),
                pose.gazeX,
                pose.gazeY,
                eyeR * 0.8f,
                iris,
                happy = pose.petLean,
            )
        }
        drawCircle(iris, radius = headR * 0.075f, center = Offset(headC.x, headC.y + headR * 0.3f))

        // Anxious whisker shiver / cheek blush when petted.
        if (pose.petLean > 0.2f) {
            val blush = Hues.hsl(8f, 0.7f, 0.68f, 0.35f * pose.petLean)
            drawCircle(blush, radius = headR * 0.16f, center = Offset(headC.x - headR * 0.62f, headC.y + headR * 0.22f))
            drawCircle(blush, radius = headR * 0.16f, center = Offset(headC.x + headR * 0.62f, headC.y + headR * 0.22f))
        }
    }

    private fun DrawScope.drawEar(
        headC: Offset,
        headR: Float,
        side: Float,
        perk: Float,
        flick: Float,
        lagX: Float,
        fur: Color,
        inner: Color,
    ) {
        val baseX = headC.x + side * headR * 0.55f
        val baseY = headC.y - headR * 0.55f
        val tipX = baseX + side * headR * (0.35f + flick) + lagX
        val tipY = baseY - headR * (0.95f * perk)
        earPath.reset()
        earPath.moveTo(baseX - side * headR * 0.32f, baseY + headR * 0.1f)
        earPath.lineTo(tipX, tipY)
        earPath.lineTo(baseX + side * headR * 0.45f, baseY + headR * 0.28f)
        earPath.close()
        drawPath(earPath, fur)
        earPath.reset()
        earPath.moveTo(baseX - side * headR * 0.12f, baseY + headR * 0.08f)
        earPath.lineTo(tipX - side * headR * 0.05f, tipY + headR * 0.18f)
        earPath.lineTo(baseX + side * headR * 0.22f, baseY + headR * 0.18f)
        earPath.close()
        drawPath(earPath, inner.copy(alpha = 0.8f))
    }
}
