package app.anima.core.creature.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import app.anima.core.creature.engine.ValueNoise
import app.anima.core.model.Mood

/**
 * Пиксельный ретро-питомец: a deliberate tamagotchi homage. The body is a
 * seeded symmetric sprite (classic procedural-invader trick) on a coarse
 * grid; animation is two alternating frames + pixel-quantized wander; the
 * blink turns eye pixels off. All squares, no anti-alias pretence.
 */
class PixelPetRenderer : CreatureRenderer {

    /** 12×12 logical grid; left 6 columns generated, mirrored right. */
    private val grid = 12
    private var spriteSeedCache = Long.MIN_VALUE
    private val frameA = Array(grid) { BooleanArray(grid) }
    private val frameB = Array(grid) { BooleanArray(grid) }

    override fun DrawScope.render(ctx: RenderContext) {
        val pose = ctx.pose
        buildSprites(ctx.seed)

        val px = size.minDimension * 0.75f / grid
        // Snap wander to whole pixels — motion stays chunky on purpose.
        val originX = size.width / 2f - (grid / 2f) * px +
            (pose.offsetX * size.minDimension / px).toInt() * px
        val originY = size.height / 2f - (grid / 2f) * px +
            (pose.offsetY * size.minDimension / px).toInt() * px

        // Two-frame walk cycle; frame rate follows energy (asleep ≈ frozen).
        val fps = 1.6f * (0.25f + pose.energy)
        val frame = if ((ctx.timeSeconds * fps).toInt() % 2 == 0) frameA else frameB
        // Breath on a pixel pet: the whole sprite shifts one pixel down at exhale.
        val breathShift = if (pose.breath < 0.4f) px else 0f

        val body = Hues.bodyColor(140f, sat = 0.55f, light = 0.55f, ctx = ctx)
        val shade = body.copy(alpha = 0.55f)

        for (y in 0 until grid) {
            for (x in 0 until grid) {
                if (!frame[y][x]) continue
                val isEdge = y + 1 >= grid || !frame[y + 1][x]
                drawRect(
                    color = if (isEdge) shade else body,
                    topLeft = Offset(originX + x * px, originY + y * px + breathShift),
                    size = Size(px * 0.92f, px * 0.92f),
                )
            }
        }

        // Eyes: 1-pixel squares; blink = pixels off; gaze = 1-pixel shift.
        val eyesClosed = pose.blinkLeft > 0.5f || pose.lidDroop > 0.7f || ctx.mood == Mood.ASLEEP
        if (!eyesClosed) {
            val gx = if (pose.gazeX > 0.35f) 1 else if (pose.gazeX < -0.35f) -1 else 0
            val gy = if (pose.gazeY > 0.35f) 1 else 0
            val eyeColor = Color(0xFF10141C)
            val eyeRow = grid / 2 - 2 + gy
            drawRect(eyeColor, Offset(originX + (grid / 2 - 2 + gx) * px, originY + eyeRow * px + breathShift), Size(px, px))
            drawRect(eyeColor, Offset(originX + (grid / 2 + 1 + gx) * px, originY + eyeRow * px + breathShift), Size(px, px))
        } else {
            // Closed eyes: 2-pixel dashes.
            val eyeColor = Color(0xFF10141C).copy(alpha = 0.8f)
            val eyeRow = grid / 2 - 2
            drawRect(eyeColor, Offset(originX + (grid / 2 - 2) * px, originY + eyeRow * px + breathShift), Size(px, px * 0.4f))
            drawRect(eyeColor, Offset(originX + (grid / 2 + 1) * px, originY + eyeRow * px + breathShift), Size(px, px * 0.4f))
        }

        // Mood pixels: hearts when petted, sweat drop when hot, Z when asleep.
        val accentColor = when {
            pose.petLean > 0.3f -> Hues.hsl(350f, 0.75f, 0.65f)
            ctx.mood == Mood.HOT -> Hues.hsl(200f, 0.7f, 0.6f)
            ctx.mood == Mood.ASLEEP -> Color(0xFF9AA3C0)
            else -> null
        }
        accentColor?.let {
            val bx = originX + (grid + 1) * px * 0.9f
            val by = originY + px * 1.5f
            drawRect(it, Offset(bx, by), Size(px, px))
            drawRect(it, Offset(bx + px, by - px), Size(px, px))
        }
    }

    /** Symmetric sprite from the seed; frame B perturbs the outline pixels. */
    private fun buildSprites(seed: Long) {
        if (spriteSeedCache == seed) return
        spriteSeedCache = seed
        val half = grid / 2
        for (y in 0 until grid) {
            for (xh in 0 until half) {
                // Body mass: denser toward centre, seeded speckle at the rim.
                val cx = (xh + 0.5f) / half
                val cy = kotlin.math.abs(y - grid / 2f) / (grid / 2f)
                val dist = cx * cx * 0.6f + cy * cy
                val roll = ValueNoise.hash01((y * half + xh).toLong(), seed, 70)
                val on = dist < 0.55f || (dist < 0.85f && roll > 0.45f)
                frameA[y][xh] = on
                frameA[y][grid - 1 - xh] = on
                // Frame B: rim pixels flicker (procedural 2-frame idle).
                val rim = dist in 0.45f..0.9f
                val onB = if (rim) roll > 0.5f else on
                frameB[y][xh] = onB
                frameB[y][grid - 1 - xh] = onB
            }
        }
        // Feet: two stubs always on, alternating between frames (walk beat).
        val footY = grid - 2
        frameA[footY][2] = true
        frameA[footY][grid - 3] = true
        frameB[footY][3] = true
        frameB[footY][grid - 4] = true
    }
}
