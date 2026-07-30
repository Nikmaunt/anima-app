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

    private companion object {
        /** The pet's base hue, shared by body, shading, outline and rim. */
        const val PIXEL_HUE = 140f
    }

    private var spriteSeedCache = Long.MIN_VALUE
    private val frameA = Array(grid) { BooleanArray(grid) }
    private val frameB = Array(grid) { BooleanArray(grid) }

    override fun DrawScope.render(ctx: RenderContext) {
        val pose = ctx.pose
        buildSprites(ctx.seed)

        val px = size.minDimension * 0.75f * RigScale.PIXEL_PET / grid
        // Snap wander to whole pixels — motion stays chunky on purpose.
        val originX =
            size.width / 2f - (grid / 2f) * px +
                (pose.offsetX * size.minDimension / px).toInt() * px
        val originY =
            size.height / 2f + size.minDimension * RigScale.PIXEL_PET_BIAS - (grid / 2f) * px +
                (pose.offsetY * size.minDimension / px).toInt() * px

        // Two-frame walk cycle; frame rate follows energy (asleep ≈ frozen).
        val fps = 1.6f * (0.25f + pose.energy)
        val frame = if ((ctx.timeSeconds * fps).toInt() % 2 == 0) frameA else frameB
        // Breath on a pixel pet: the whole sprite shifts one pixel down at exhale.
        val breathShift = if (pose.breath < 0.4f) px else 0f

        // v1.1b task 2. Three things were making this the worst body on a photo,
        // and none of them was the grid idea:
        //
        //  - the cells were drawn at 0.92 of a cell, so 8 % of every cell was a
        //    gutter the wallpaper came through — the sprite read as a mesh laid
        //    over the photo rather than as a creature standing on it;
        //  - the bottom edge row was the body colour at alpha 0.55, i.e. the one
        //    row that should read as the sprite's own shadow was the one row you
        //    could see through;
        //  - there was no outline at all.
        //
        // Cells are now contiguous and opaque, shaded top-to-bottom so the sprite
        // has its own light, and the silhouette carries a one-cell outline — along
        // the SPRITE's edge, not around each cell (which would be noise) and not
        // around the bounding box (which would kill the shape). That is the genre
        // convention, not a workaround: every sprite of this era had one.
        val topLight = Hues.bodyColor(PIXEL_HUE, sat = 0.55f, light = 0.62f, ctx = ctx)
        val bottomLight = Hues.bodyColor(PIXEL_HUE, sat = 0.6f, light = 0.4f, ctx = ctx)
        val edgeShade = Hues.bodyColor(PIXEL_HUE, sat = 0.62f, light = 0.3f, ctx = ctx)
        val outline = Grounding.contour(PIXEL_HUE, ctx)
        // Not Grounding.rim() here. On a stroked path the rim is a thin line; on a
        // 12-cell grid it is a whole cell tall, and at the shared brightness the
        // sprite's jagged top edge came out as a row of near-white teeth — looked
        // at on the sheet it was the loudest thing in the frame. A lighter shade of
        // the body's own green does the same job quietly, and the body's mid-green
        // is already what carries the silhouette on a black wallpaper.
        val rimLight = Hues.bodyColor(PIXEL_HUE, sat = 0.5f, light = 0.76f, ctx = ctx)

        // Lambdas rather than local functions: Kotlin has no callable reference to
        // a local fun, and both of these have to be handed to drawSprite().
        val on: (Int, Int) -> Boolean = { x, y ->
            y in 0 until grid && x in 0 until grid && frame[y][x]
        }
        val cell: (Int, Int, Color) -> Unit = { x, y, color ->
            drawRect(
                color = color,
                topLeft = Offset(originX + x * px, originY + y * px + breathShift),
                size = Size(px, px),
            )
        }

        // Occlusion under the feet, before anything else.
        with(Grounding) {
            drawContactShadow(
                center = Offset(originX + grid * px * 0.5f, originY + grid * px * 0.98f + breathShift),
                halfWidth = px * grid * 0.34f,
                halfHeight = px * 0.55f,
            )
        }

        drawSprite(frame, on, cell, topLight, bottomLight, edgeShade, outline, rimLight)

        // Eyes: 1-pixel squares; blink = pixels off; gaze = 1-pixel shift.
        val eyesClosed = pose.blinkLeft > 0.5f || pose.lidDroop > 0.7f || ctx.mood == Mood.ASLEEP
        if (!eyesClosed) {
            val gx =
                if (pose.gazeX > 0.35f) {
                    1
                } else if (pose.gazeX < -0.35f) {
                    -1
                } else {
                    0
                }
            val gy = if (pose.gazeY > 0.35f) 1 else 0
            val eyeColor = Color(0xFF10141C)
            val eyeRow = grid / 2 - 2 + gy
            val eyeY = originY + eyeRow * px + breathShift
            drawRect(eyeColor, Offset(originX + (grid / 2 - 2 + gx) * px, eyeY), Size(px, px))
            drawRect(eyeColor, Offset(originX + (grid / 2 + 1 + gx) * px, eyeY), Size(px, px))
        } else {
            // Closed eyes: 2-pixel dashes.
            val eyeColor = Color(0xFF10141C).copy(alpha = 0.8f)
            val eyeRow = grid / 2 - 2
            val eyeY = originY + eyeRow * px + breathShift
            drawRect(eyeColor, Offset(originX + (grid / 2 - 2) * px, eyeY), Size(px, px * 0.4f))
            drawRect(eyeColor, Offset(originX + (grid / 2 + 1) * px, eyeY), Size(px, px * 0.4f))
        }

        // Mood pixels: hearts when petted, sweat drop when hot, Z when asleep.
        val accentColor =
            when {
                pose.petLean > 0.3f -> Hues.hsl(350f, 0.75f, 0.65f)
                ctx.mood == Mood.HOT -> Hues.hsl(200f, 0.7f, 0.6f)
                ctx.mood == Mood.ASLEEP -> Color(0xFF9AA3C0)
                else -> null
            }
        // v1.1b task 2: these two cells used to sit at column grid+1, i.e. outside
        // the sprite — and the second of them was drawn PAST THE EDGE OF THE FRAME
        // and clipped. Visible on all three contact sheets as one or two stray
        // squares at the top right, and it is why asleep-night measured 0.912 of
        // the frame while the same body measured 0.818 awake. Status pips now sit
        // on the sprite's own shoulder, which is where a tamagotchi kept them.
        accentColor?.let {
            cell(grid - 4, 1, it)
            cell(grid - 3, 0, it)
        }
    }

    /**
     * Flood fill over the one-cell margin around the grid, through OFF cells.
     * Returns a predicate: is (x, y) reachable from outside the sprite?
     *
     * Coordinates run -1..grid inclusive, so the returned lambda takes sprite
     * coordinates and does the offset itself.
     */
    private fun floodOutside(frame: Array<BooleanArray>): (Int, Int) -> Boolean {
        val span = grid + 2
        val seen = Array(span) { BooleanArray(span) }

        fun solid(
            x: Int,
            y: Int,
        ) = y in 0 until grid && x in 0 until grid && frame[y][x]

        val queue = ArrayDeque<Int>()

        fun push(
            x: Int,
            y: Int,
        ) {
            if (x < -1 || y < -1 || x > grid || y > grid) return
            if (seen[y + 1][x + 1] || solid(x, y)) return
            seen[y + 1][x + 1] = true
            queue.addLast((y + 1) * span + (x + 1))
        }
        for (i in -1..grid) {
            push(i, -1)
            push(i, grid)
            push(-1, i)
            push(grid, i)
        }
        while (queue.isNotEmpty()) {
            val v = queue.removeFirst()
            val x = v % span - 1
            val y = v / span - 1
            push(x - 1, y)
            push(x + 1, y)
            push(x, y - 1)
            push(x, y + 1)
        }
        return { x, y ->
            x in -1..grid && y in -1..grid && seen[y + 1][x + 1]
        }
    }

    /** Straight-line blend; the ramp needs no easing and no allocation. */
    private fun lerpColor(
        a: Color,
        b: Color,
        t: Float,
    ): Color =
        Color(
            red = a.red + (b.red - a.red) * t,
            green = a.green + (b.green - a.green) * t,
            blue = a.blue + (b.blue - a.blue) * t,
            alpha = 1f,
        )

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

    /**
     * The outline ring and the body cells.
     *
     * Extracted in v1.1b: with the silhouette outline added, `render` crossed
     * detekt's cyclomatic-complexity threshold, and the two nested loops were also
     * the only place with enough branching to be worth reading on its own.
     *
     * `outside` answers "is this OFF cell reachable from beyond the sprite" — the
     * first version of the outline skipped that question and put pale cells in the
     * sprite's interior holes, which read as speckles scattered through the body.
     */
    private fun drawSprite(
        frame: Array<BooleanArray>,
        on: (Int, Int) -> Boolean,
        cell: (Int, Int, Color) -> Unit,
        topLight: Color,
        bottomLight: Color,
        edgeShade: Color,
        outline: Color,
        rimLight: Color,
    ) {
        val outside = floodOutside(frame)

        // Outline ring, one cell wide, along the sprite's own silhouette. Cells
        // sitting directly above the sprite get the rim instead of the dark
        // contour, which is what makes the silhouette survive a black wallpaper.
        for (y in -1..grid) {
            for (x in -1..grid) {
                val touches = on(x - 1, y) || on(x + 1, y) || on(x, y - 1) || on(x, y + 1)
                if (!on(x, y) && outside(x, y) && touches) {
                    cell(x, y, if (on(x, y + 1)) rimLight else outline)
                }
            }
        }

        for (y in 0 until grid) {
            for (x in 0 until grid) {
                if (frame[y][x]) {
                    val ramp = y / (grid - 1f)
                    val lit = lerpColor(topLight, bottomLight, ramp)
                    cell(x, y, if (on(x, y + 1)) lit else edgeShade)
                } else if (!outside(x, y)) {
                    // Enclosed hole: filled dark rather than left transparent. It
                    // reads as the sprite's own interior shading, and it is one
                    // fewer place for a wallpaper to show through the creature.
                    cell(x, y, edgeShade)
                }
            }
        }
    }
}
