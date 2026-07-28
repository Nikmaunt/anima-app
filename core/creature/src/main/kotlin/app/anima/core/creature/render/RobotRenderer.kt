package app.anima.core.creature.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import app.anima.core.model.Mood
import kotlin.math.sin

/**
 * Робот-компаньон: friendly geometry. Display-eyes glow and blink by scaleY;
 * the charge indicator is part of the body — a chest bar that fills with the
 * real battery percent and shimmers while eating.
 */
class RobotRenderer : CreatureRenderer {
    override fun DrawScope.render(ctx: RenderContext) {
        val pose = ctx.pose
        val c = Offset(size.width / 2f, size.height / 2f + size.minDimension * RigScale.ROBOT_BIAS)
        val bodyW = size.minDimension * 0.46f * RigScale.ROBOT
        val bodyH = size.minDimension * 0.4f * RigScale.ROBOT * (1f + pose.breath * 0.03f)
        val shell = Hues.bodyColor(215f, sat = 0.16f, light = 0.72f, ctx = ctx)
        val shellDeep = Hues.bodyColor(220f, sat = 0.2f, light = 0.5f, ctx = ctx)
        val screen = Color(0xFF11151F)
        val eyeGlow = Hues.bodyColor(165f, sat = 0.8f, light = 0.62f, ctx = ctx)

        // Hover shadow — the robot floats a little; wander is its thruster sway.
        drawOval(
            Color.Black.copy(alpha = 0.22f),
            topLeft = Offset(c.x - bodyW * 0.42f, c.y + bodyH * 0.72f),
            size = Size(bodyW * 0.84f, bodyH * 0.16f),
        )

        // Antenna on chain link 0: springy lag.
        val antX = c.x + pose.secondaryX[0] * bodyW * 1.3f
        val antTipY = c.y - bodyH * 0.95f + pose.secondaryY[0] * bodyH
        drawLine(
            shellDeep,
            start = Offset(c.x, c.y - bodyH * 0.55f),
            end = Offset(antX, antTipY),
            strokeWidth = bodyW * 0.03f,
        )
        val antennaLight = if (ctx.charging) eyeGlow else shellDeep
        drawCircle(antennaLight, radius = bodyW * 0.05f, center = Offset(antX, antTipY))

        // Body shell.
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(shell, shellDeep), startY = c.y - bodyH, endY = c.y + bodyH),
            topLeft = Offset(c.x - bodyW / 2f, c.y - bodyH * 0.6f),
            size = Size(bodyW, bodyH * 1.25f),
            cornerRadius = CornerRadius(bodyW * 0.22f, bodyW * 0.22f),
        )

        // Face screen.
        val faceW = bodyW * 0.74f
        val faceH = bodyH * 0.52f
        drawRoundRect(
            color = screen,
            topLeft = Offset(c.x - faceW / 2f, c.y - bodyH * 0.48f),
            size = Size(faceW, faceH),
            cornerRadius = CornerRadius(bodyW * 0.12f, bodyW * 0.12f),
        )

        // Display eyes: rounded bars; blink = vertical squeeze; gaze shifts them.
        val openL = EyeKit.openness(pose.blinkLeft, pose.lidDroop).coerceAtLeast(0.08f)
        val openR = EyeKit.openness(pose.blinkRight, pose.lidDroop).coerceAtLeast(0.08f)
        val eyeW = faceW * 0.18f
        val eyeHFull = faceH * 0.42f
        val eyeY = c.y - bodyH * 0.48f + faceH * 0.5f + pose.gazeY * faceH * 0.12f
        val gazeShift = pose.gazeX * faceW * 0.08f
        // Thinking: the eyes become a scanning wave.
        if (pose.thinking) {
            val phase = sin(ctx.timeSeconds * 6f)
            drawRoundRect(
                eyeGlow.copy(alpha = 0.9f),
                topLeft = Offset(c.x - faceW * 0.3f + phase * faceW * 0.18f + gazeShift, eyeY - eyeHFull * 0.18f),
                size = Size(eyeW * 1.4f, eyeHFull * 0.36f),
                cornerRadius = CornerRadius(eyeW * 0.4f, eyeW * 0.4f),
            )
        } else {
            for (side in intArrayOf(-1, 1)) {
                val open = if (side < 0) openL else openR
                val h = eyeHFull * open
                val happy = pose.petLean > 0.35f
                val color = eyeGlow.copy(alpha = 0.75f + 0.25f * pose.energy)
                if (happy) {
                    // Happy display: ^ ^ arcs drawn as two bars.
                    drawRoundRect(
                        color,
                        topLeft = Offset(c.x + side * faceW * 0.22f - eyeW / 2f + gazeShift, eyeY - eyeHFull * 0.1f),
                        size = Size(eyeW, eyeHFull * 0.14f),
                        cornerRadius = CornerRadius(eyeW * 0.3f, eyeW * 0.3f),
                    )
                } else {
                    drawRoundRect(
                        color,
                        topLeft = Offset(c.x + side * faceW * 0.22f - eyeW / 2f + gazeShift, eyeY - h / 2f),
                        size = Size(eyeW, h),
                        cornerRadius = CornerRadius(eyeW * 0.45f, eyeW * 0.45f),
                    )
                }
            }
        }

        // Chest charge bar — the battery IS the body.
        val barW = bodyW * 0.56f
        val barH = bodyH * 0.12f
        val barTop = Offset(c.x - barW / 2f, c.y + bodyH * 0.28f)
        drawRoundRect(
            screen.copy(alpha = 0.85f),
            topLeft = barTop,
            size = Size(barW, barH),
            cornerRadius = CornerRadius(barH / 2f, barH / 2f),
        )
        val level = ctx.batteryPercent.coerceIn(0, 100) / 100f
        val fillColor =
            when {
                ctx.batteryPercent <= 20 && !ctx.charging -> Hues.hsl(18f, 0.85f, 0.6f)
                else -> eyeGlow
            }
        // Eating shimmer: the filled edge pulses forward.
        val shimmer = if (ctx.charging) (sin(ctx.timeSeconds * 4f) * 0.03f + 0.03f) else 0f
        drawRoundRect(
            fillColor.copy(alpha = 0.95f),
            topLeft = Offset(barTop.x + barH * 0.18f, barTop.y + barH * 0.18f),
            size = Size((barW - barH * 0.36f) * (level + shimmer).coerceIn(0.02f, 1f), barH * 0.64f),
            cornerRadius = CornerRadius(barH / 2f, barH / 2f),
        )

        // Hot: vents open — three slots glowing on the shell.
        if (ctx.mood == Mood.HOT) {
            for (i in 0..2) {
                drawRoundRect(
                    Hues.hsl(12f, 0.8f, 0.55f, 0.5f + 0.2f * sin(ctx.timeSeconds * 5f + i)),
                    topLeft = Offset(c.x - bodyW * 0.3f + i * bodyW * 0.22f, c.y + bodyH * 0.52f),
                    size = Size(bodyW * 0.12f, bodyH * 0.05f),
                    cornerRadius = CornerRadius(bodyH * 0.025f, bodyH * 0.025f),
                )
            }
        }
    }
}
