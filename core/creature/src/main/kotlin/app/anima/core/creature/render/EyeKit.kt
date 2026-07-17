package app.anima.core.creature.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Shared eyes: blink, lid droop, gaze offset, happy-crescent while petted.
 * Every concept routes its eyes through here so the gaze language is one
 * language across all bodies.
 */
object EyeKit {

    private val lidPath = Path()

    /**
     * Draws one round eye.
     *
     * @param openAmount 1 = fully open (after blink+droop composition).
     * @param gazeRange max pupil travel in px.
     */
    fun DrawScope.drawRoundEye(
        center: Offset,
        radius: Float,
        openAmount: Float,
        gazeX: Float,
        gazeY: Float,
        gazeRange: Float,
        irisColor: Color,
        scleraColor: Color? = null,
        happy: Float = 0f,
    ) {
        val open = openAmount.coerceIn(0f, 1f)
        if (open <= 0.05f) {
            // Closed: a soft arc line.
            drawArc(
                color = irisColor,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius * 0.7f),
                size = Size(radius * 2f, radius * 1.4f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.32f),
            )
            return
        }
        if (happy > 0.35f) {
            // Content crescent (petting): an upward arc instead of a disc.
            drawArc(
                color = irisColor,
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius * 0.4f),
                size = Size(radius * 2f, radius * 1.6f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.34f),
            )
            return
        }
        scleraColor?.let {
            drawOval(
                color = it,
                topLeft = Offset(center.x - radius * 1.25f, center.y - radius * 1.25f * open),
                size = Size(radius * 2.5f, radius * 2.5f * open),
            )
        }
        val pupil = Offset(center.x + gazeX * gazeRange, center.y + gazeY * gazeRange * 0.7f)
        // Vertical squeeze from the lid.
        drawOval(
            color = irisColor,
            topLeft = Offset(pupil.x - radius, pupil.y - radius * open),
            size = Size(radius * 2f, radius * 2f * open),
        )
        // A living highlight.
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = radius * 0.22f,
            center = Offset(pupil.x + radius * 0.3f, pupil.y - radius * 0.35f * open),
        )
    }

    /** Composes blink and droop into one openness value. */
    fun openness(blink: Float, droop: Float): Float =
        ((1f - blink) * (1f - droop * 0.85f)).coerceIn(0f, 1f)
}
