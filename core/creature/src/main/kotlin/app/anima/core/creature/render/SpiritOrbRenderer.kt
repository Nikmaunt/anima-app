package app.anima.core.creature.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import app.anima.core.creature.engine.ValueNoise
import app.anima.core.model.Mood
import kotlin.math.cos
import kotlin.math.sin

/**
 * Сфера-дух: a soft glowing orb of liquid glass with metaball satellites.
 * The AGSL glow is its skin; satellites ride the secondary spring chain so
 * they lag and settle like drops of the same liquid.
 */
class SpiritOrbRenderer : CreatureRenderer {
    private val glow = GlowSkin()

    private companion object {
        /** Grounding geometry, in body radii (v1.1b task 2). */
        const val SHADOW_DROP = 1.02f
        const val SHADOW_HALF_W = 0.62f
        const val SHADOW_HALF_H = 0.15f
        const val CONTOUR_SHARE = 0.055f
    }

    override fun DrawScope.render(ctx: RenderContext) {
        val pose = ctx.pose
        val c = Offset(size.width / 2f, size.height / 2f + size.minDimension * RigScale.SPIRIT_ORB_BIAS)
        val r = size.minDimension * 0.26f * RigScale.SPIRIT_ORB * (1f + pose.breath * 0.05f)
        val hue = 190f // pale cyan spirit
        val core = Hues.bodyColor(hue, sat = 0.55f, light = 0.72f, ctx = ctx)
        val rim = Hues.bodyColor(hue + 24f, sat = 0.65f, light = 0.6f, ctx = ctx)

        // v1.1b task 2: occlusion first, under everything. Without it the orb had
        // nothing anywhere in its frame darker than the backdrop, which is why a
        // busy photo showed straight through it.
        with(Grounding) {
            drawContactShadow(
                center = c.copy(y = c.y + r * SHADOW_DROP),
                halfWidth = r * SHADOW_HALF_W,
                halfHeight = r * SHADOW_HALF_H,
            )
        }

        with(glow) {
            drawGlow(
                center = c,
                radius = r,
                color = rim,
                intensity = 0.45f + 0.4f * pose.energy + (if (ctx.mood == Mood.EATING) 0.25f else 0f),
                time = ctx.timeSeconds,
            )
        }

        // Metaball satellites: three droplets orbiting on chain lag.
        val satellites = 3
        for (i in 0 until satellites) {
            // secondaryCount is 0 in reduced-motion statics (the widget
            // snapshot) — clamp to a valid slot; the arrays are zero-filled,
            // so the satellites simply ride their pure orbits. Found by the
            // v0.3 GMD suite (index -1 crash on the snapshot path).
            val chainIdx = (i * 2).coerceIn(0, pose.secondaryCount.coerceAtLeast(1) - 1)
            val phase = ctx.timeSeconds * (0.25f + i * 0.09f) + i * 2.1f
            val orbit = r * (1.45f + 0.18f * sin(phase * 0.7f))
            val sx = c.x + cos(phase) * orbit + pose.secondaryX[chainIdx] * r * 2f
            val sy = c.y + sin(phase) * orbit * 0.6f + pose.secondaryY[chainIdx] * r * 2f
            val sr = r * (0.16f - i * 0.03f) * (0.7f + 0.6f * pose.energy)
            // Neck: a soft bridge toward the core when close (metaball feel).
            val toward = Offset((c.x + sx) / 2f, (c.y + sy) / 2f)
            drawCircle(core.copy(alpha = 0.10f), radius = sr * 1.8f, center = toward)
            drawCircle(
                brush =
                    Brush.radialGradient(
                        listOf(core.copy(alpha = 0.9f), rim.copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(sx, sy),
                        radius = sr * 1.9f,
                    ),
                radius = sr * 1.9f,
                center = Offset(sx, sy),
            )
        }

        // v1.1b task 2: the body's own value, opaque, lit from above. Before this
        // the orb WAS the translucent layer below — every pixel of it let the
        // wallpaper through, so on a photo it stopped being a body at all. Glass
        // still reads, because the caustic layers and the highlight are still
        // painted over the top; what changed is that there is now something for
        // them to be painted onto.
        drawCircle(
            brush =
                Grounding.verticalBody(
                    top = Hues.bodyColor(hue, sat = 0.42f, light = 0.80f, ctx = ctx),
                    bottom = Hues.bodyColor(hue + 14f, sat = 0.55f, light = 0.44f, ctx = ctx),
                    topY = c.y - r,
                    bottomY = c.y + r,
                ),
            radius = r,
            center = c,
        )

        // Liquid-glass caustics: the layered translucent discs, now on top of an
        // opaque body rather than instead of one.
        drawCircle(
            brush =
                Brush.radialGradient(
                    listOf(
                        core.copy(alpha = 0.75f),
                        core.copy(alpha = 0.35f),
                        rim.copy(alpha = 0.15f),
                    ),
                    center = c.copy(y = c.y - r * 0.25f),
                    radius = r * 1.4f,
                ),
            radius = r,
            center = c,
        )
        // Inner wobble highlight (living caustic).
        val wob = ValueNoise.fbm2(ctx.timeSeconds * 0.4f, ctx.seed, 51)
        drawCircle(
            color = Color.White.copy(alpha = 0.35f + 0.1f * pose.breath),
            radius = r * 0.32f,
            center = Offset(c.x - r * 0.3f + wob * r * 0.12f, c.y - r * 0.38f),
        )

        // v1.1b task 2: the edge, both ways round. A single dark ring would
        // vanish on a dark wallpaper and a single light one on paper.
        with(Grounding) {
            drawTwoToneRing(center = c, radius = r, hue = hue, ctx = ctx, width = r * CONTOUR_SHARE)
        }

        // Eyes float inside the light.
        val eyeGap = r * 0.42f
        val eyeY = c.y - r * 0.05f
        val eyeR = r * 0.11f
        val open = EyeKit.openness(pose.blinkLeft, pose.lidDroop)
        val openR = EyeKit.openness(pose.blinkRight, pose.lidDroop)
        val iris = Color(0xFF20313E)
        with(EyeKit) {
            drawRoundEye(
                Offset(c.x - eyeGap, eyeY),
                eyeR,
                open,
                pose.gazeX,
                pose.gazeY,
                gazeRange = eyeR * 0.9f,
                irisColor = iris,
                happy = pose.petLean,
            )
            drawRoundEye(
                Offset(c.x + eyeGap, eyeY),
                eyeR,
                openR,
                pose.gazeX,
                pose.gazeY,
                gazeRange = eyeR * 0.9f,
                irisColor = iris,
                happy = pose.petLean,
            )
        }
    }
}
