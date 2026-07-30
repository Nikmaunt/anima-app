package app.anima.core.creature.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate

/**
 * v1.1b task 2 — the subtractive half of a body.
 *
 * SPIRIT_ORB, PIXEL_PET and MOTH failed the contact sheet on a busy photo, and
 * the reason turned out to be deeper than "they are transparent": all three
 * existed **only as additive light**. Light laid over a bright photo pixel adds
 * nothing — the body had no way to be *darker* than its background, so the
 * wallpaper became part of the creature.
 *
 * Three things a body needs in order to sit on any backdrop, and none of them is
 * a plate behind it:
 *
 *  1. **Occlusion.** A soft shadow under the body. ROBOT and SPROUT already had
 *     one (a flat ellipse and the soil mound) and they were the two concepts
 *     that read on all three backdrops — this generalises what already worked,
 *     with a gradient so the edge is soft rather than a hard grey oval.
 *  2. **A contour that reads both ways.** One colour cannot do it: dark
 *     disappears on dark, light disappears on light. So the contour is two
 *     strokes — a dark one, and a lighter one offset upward inside it, which is
 *     the ordinary top-lit rim. On a light backdrop the dark stroke carries the
 *     silhouette; on a dark backdrop the rim does.
 *  3. **Its own lightness.** Not glow. The body is filled opaquely and shaded
 *     from top to bottom, so it has a value of its own that survives whatever is
 *     behind it. Glow then sits on top as an addition instead of as the body.
 *
 * The frame stays fully transparent — nothing here fills the tile, and every
 * shadow is bounded to the body's own footprint.
 */
internal object Grounding {
    /**
     * Shadow alpha at the darkest point. Deliberately below the 0.5 that
     * `FrameScaleTest` counts as ink: a shadow is not silhouette, and letting it
     * into the measurement would let a body "fill the frame" with its shadow.
     */
    private const val SHADOW_ALPHA = 0.30f

    /** Contour lightness/saturation. Dark enough to read on white paper. */
    private const val CONTOUR_LIGHT = 0.16f
    private const val CONTOUR_SAT = 0.5f

    /**
     * Rim lightness. Bright enough to read on a black wallpaper, and no brighter:
     * the first attempt at 0.93/0.22 came out near-white and, looked at on the
     * sheet, read as a white sticker outline drawn around each body rather than as
     * light falling on it. Keeping saturation up keeps it in the creature's hue.
     */
    private const val RIM_LIGHT = 0.84f
    private const val RIM_SAT = 0.4f

    /** The rim stroke's share of the contour stroke, and how far up it sits. */
    private const val RIM_WIDTH_SHARE = 0.4f
    private const val RIM_LIFT_SHARE = 0.5f

    /**
     * The body's own dark edge, in the creature's own hue so it reads as shadow
     * on that body rather than as a black sticker outline.
     */
    fun contour(
        hue: Float,
        ctx: RenderContext,
    ): Color = Hues.bodyColor(hue, sat = CONTOUR_SAT, light = CONTOUR_LIGHT, ctx = ctx)

    /** The top-lit rim. Same hue, nearly white, so it stays in family. */
    fun rim(
        hue: Float,
        ctx: RenderContext,
    ): Color = Hues.bodyColor(hue, sat = RIM_SAT, light = RIM_LIGHT, ctx = ctx)

    /**
     * A soft ellipse of occlusion centred at [center]. Gradient, so it has no
     * edge of its own to be mistaken for part of the body.
     */
    fun DrawScope.drawContactShadow(
        center: Offset,
        halfWidth: Float,
        halfHeight: Float,
        strength: Float = 1f,
    ) {
        val alpha = SHADOW_ALPHA * strength.coerceIn(0f, 1f)
        // Drawn as a circle brush scaled by the oval's own bounds: a radial
        // gradient inside drawOval would be clipped square by the brush radius.
        val steps = 4
        for (i in steps downTo 1) {
            val t = i / steps.toFloat()
            drawOval(
                Color.Black.copy(alpha = alpha / steps),
                topLeft = Offset(center.x - halfWidth * t, center.y - halfHeight * t),
                size = Size(halfWidth * 2f * t, halfHeight * 2f * t),
            )
        }
    }

    /**
     * Dark contour plus the rim above it, along [path].
     *
     * The rim is the same path stroked thinner and lifted, not an inset outline —
     * insetting an arbitrary path is not something Compose offers, and a lifted
     * stroke gives the same read for a fraction of the work. Draw this AFTER the
     * fill: the dark stroke straddles the silhouette so half of it lands inside.
     */
    fun DrawScope.drawTwoToneContour(
        path: Path,
        hue: Float,
        ctx: RenderContext,
        width: Float,
    ) {
        drawPath(path, contour(hue, ctx), style = Stroke(width = width))
        translate(top = -width * RIM_LIFT_SHARE) {
            drawPath(path, rim(hue, ctx), style = Stroke(width = width * RIM_WIDTH_SHARE))
        }
    }

    /** The same two-tone contour for a circular body. */
    fun DrawScope.drawTwoToneRing(
        center: Offset,
        radius: Float,
        hue: Float,
        ctx: RenderContext,
        width: Float,
    ) {
        drawCircle(contour(hue, ctx), radius = radius, center = center, style = Stroke(width = width))
        drawCircle(
            rim(hue, ctx),
            radius = radius,
            center = center.copy(y = center.y - width * RIM_LIFT_SHARE),
            style = Stroke(width = width * RIM_WIDTH_SHARE),
        )
    }

    /**
     * A body's own top-to-bottom shading, opaque. This is the "own lightness"
     * part: [top] at the crown, [bottom] at the base.
     */
    fun verticalBody(
        top: Color,
        bottom: Color,
        topY: Float,
        bottomY: Float,
    ): Brush = Brush.verticalGradient(listOf(top, bottom), startY = topY, endY = bottomY)
}
