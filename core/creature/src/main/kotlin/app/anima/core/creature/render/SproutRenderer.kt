package app.anima.core.creature.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sin

/**
 * Растение-дух: a sprout in a mound whose growth is the soul made visible —
 * more remembered facts, more leaves. The stem sways on the spring chain;
 * the face lives in the blossom.
 */
class SproutRenderer : CreatureRenderer {
    private val stemPath = Path()
    private val leafPath = Path()

    override fun DrawScope.render(ctx: RenderContext) {
        val pose = ctx.pose
        val baseC = Offset(size.width / 2f, size.height / 2f + size.minDimension * (0.28f + RigScale.SPROUT_BIAS))
        val r = size.minDimension * 0.24f * RigScale.SPROUT
        val soil = Hues.bodyColor(25f, sat = 0.35f, light = 0.35f, ctx = ctx)
        val stem = Hues.bodyColor(130f, sat = 0.45f, light = 0.42f, ctx = ctx)
        val leaf = Hues.bodyColor(135f, sat = 0.5f, light = 0.5f, ctx = ctx)
        val petal = Hues.bodyColor(45f, sat = 0.75f, light = 0.72f, ctx = ctx)

        // Mound.
        drawOval(
            brush = Brush.verticalGradient(listOf(soil, soil.copy(alpha = 0.75f))),
            topLeft = Offset(baseC.x - r * 0.9f, baseC.y - r * 0.32f),
            size = Size(r * 1.8f, r * 0.65f),
        )

        // Stem: a curve whose top rides chain link 1; breath = slow sway lift.
        val topSway = pose.secondaryX[1] * r * 2.4f
        val stemTop = Offset(baseC.x + topSway, baseC.y - r * (1.5f + pose.breath * 0.08f))
        stemPath.reset()
        stemPath.moveTo(baseC.x, baseC.y - r * 0.1f)
        stemPath.cubicTo(
            baseC.x - r * 0.05f,
            baseC.y - r * 0.7f,
            stemTop.x - topSway * 0.5f,
            stemTop.y + r * 0.6f,
            stemTop.x,
            stemTop.y,
        )
        drawPath(stemPath, stem, style = Stroke(width = r * 0.11f, cap = StrokeCap.Round))

        // Leaves: count grows with the soul (2..6). Each flutters with noise.
        val leafCount = 2 + (ctx.growth.coerceIn(0f, 1f) * 4).toInt()
        for (i in 0 until leafCount) {
            val t = (i + 1f) / (leafCount + 1f)
            val lx = baseC.x + (stemTop.x - baseC.x) * t * 0.8f
            val ly = baseC.y - r * 0.1f + (stemTop.y - baseC.y + r * 0.1f) * t
            val side = if (i % 2 == 0) -1f else 1f
            val flutter =
                sin(ctx.timeSeconds * 1.4f + i * 1.3f) * 8f * pose.energy +
                    pose.secondaryX[(i + 2) % pose.secondaryCount.coerceAtLeast(1)] * 30f
            rotate(degrees = side * 42f + flutter, pivot = Offset(lx, ly)) {
                leafPath.reset()
                leafPath.moveTo(lx, ly)
                leafPath.quadraticTo(lx + side * r * 0.42f, ly - r * 0.28f, lx + side * r * 0.62f, ly)
                leafPath.quadraticTo(lx + side * r * 0.42f, ly + r * 0.2f, lx, ly)
                leafPath.close()
                drawPath(leafPath, leaf.copy(alpha = 0.9f))
            }
        }

        // Blossom head: petals + face disc. Flourish 1 = a petal shiver.
        val headR = r * (0.5f + ctx.growth * 0.15f)
        val petals = 6
        val petalShiver =
            if (pose.flourishPhase > 0f && pose.flourishKind == 1) {
                sin(pose.flourishPhase * Math.PI.toFloat() * 4f) * 6f
            } else {
                0f
            }
        for (i in 0 until petals) {
            rotate(degrees = i * (360f / petals) + petalShiver, pivot = stemTop) {
                drawOval(
                    petal.copy(alpha = 0.9f),
                    topLeft = Offset(stemTop.x - headR * 0.22f, stemTop.y - headR * 1.25f),
                    size = Size(headR * 0.44f, headR * 0.85f),
                )
            }
        }
        drawCircle(
            brush =
                Brush.radialGradient(
                    listOf(Hues.hsl(48f, 0.6f, 0.8f), petal),
                    center = stemTop,
                    radius = headR * 0.62f,
                ),
            radius = headR * 0.6f,
            center = stemTop,
        )

        // Face in the blossom.
        val eyeGap = headR * 0.28f
        val eyeR = headR * 0.1f
        val iris = Color(0xFF3A2E1E)
        with(EyeKit) {
            drawRoundEye(
                Offset(stemTop.x - eyeGap, stemTop.y - headR * 0.05f),
                eyeR,
                openness(pose.blinkLeft, pose.lidDroop),
                pose.gazeX,
                pose.gazeY,
                eyeR * 0.7f,
                iris,
                happy = pose.petLean,
            )
            drawRoundEye(
                Offset(stemTop.x + eyeGap, stemTop.y - headR * 0.05f),
                eyeR,
                openness(pose.blinkRight, pose.lidDroop),
                pose.gazeX,
                pose.gazeY,
                eyeR * 0.7f,
                iris,
                happy = pose.petLean,
            )
        }
    }
}
