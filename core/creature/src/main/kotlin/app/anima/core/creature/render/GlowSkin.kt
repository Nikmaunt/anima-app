package app.anima.core.creature.render

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas

/**
 * The AGSL "skin" layer (ADR-001): an organic glow with a noise-wobbled rim,
 * used by the orb and the ember. Runtime-gated to API 33+; below that (and in
 * previews) an equivalent radial gradient keeps every concept production-
 * looking. The shader is tiny and creature-bounded — cost scales with the
 * filled rect, not the screen.
 */
class GlowSkin {
    private val shader: RuntimeShader? =
        if (Build.VERSION.SDK_INT >= AGSL_MIN_SDK) RuntimeShader(AGSL_SOURCE) else null
    private var brush: ShaderBrush? = null

    /** Call once at surface start to hide first-compile cost (research §A2). */
    fun warmUp(sizePx: Float) {
        val s = shader
        if (Build.VERSION.SDK_INT < AGSL_MIN_SDK || s == null) return
        warmUpShader(s, sizePx)
    }

    @RequiresApi(AGSL_MIN_SDK)
    private fun warmUpShader(
        s: RuntimeShader,
        sizePx: Float,
    ) {
        s.setFloatUniform("uTime", 0f)
        s.setFloatUniform("uCenter", sizePx / 2f, sizePx / 2f)
        s.setFloatUniform("uRadius", sizePx / 4f)
        s.setFloatUniform("uColor", 1f, 1f, 1f, 0f)
    }

    fun DrawScope.drawGlow(
        center: Offset,
        radius: Float,
        color: Color,
        intensity: Float,
        time: Float,
    ) {
        // AGSL needs a hardware canvas. The widget snapshot (and any
        // offscreen Bitmap render) draws on a SOFTWARE canvas where a
        // RuntimeShader brush throws — found by the v0.3 GMD suite; the
        // API-level gate alone was never enough.
        //
        // The shader call lives in its own @RequiresApi function because
        // lint cannot carry an SDK proof through a nullable field: it wants
        // the version check and the call in one lexical scope.
        val hardware = drawContext.canvas.nativeCanvas.isHardwareAccelerated
        val s = shader
        // v1.1, second half of D5. Widening the rect fixed the shader being
        // cropped by its own rect — but once the bodies were scaled up to a
        // common size (RigScale), the glow started overflowing the FRAME
        // instead, and the frame edge cropped it into exactly the same hard
        // rectangle. Caught on the contact sheet, not by a test.
        //
        // So the glow is frame-bounded: whatever radius the body asks for,
        // the halo is shrunk until it fades out inside the drawing area. A
        // large body simply gets less room for a halo, which is also what it
        // should look like.
        val maxHalfExtent = size.minDimension * 0.5f
        val hwRadius = minOf(radius, maxHalfExtent / HALF_SIDE)
        val swRadius = minOf(radius, maxHalfExtent / SOFTWARE_FALLOFF_RADII)
        if (Build.VERSION.SDK_INT >= AGSL_MIN_SDK && hardware && s != null) {
            drawShaderGlow(s, center, hwRadius, color, intensity, time)
        } else {
            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors =
                            listOf(
                                color.copy(alpha = 0.55f * intensity),
                                color.copy(alpha = 0.18f * intensity),
                                Color.Transparent,
                            ),
                        center = center,
                        radius = swRadius * SOFTWARE_FALLOFF_RADII,
                    ),
                radius = swRadius * SOFTWARE_FALLOFF_RADII,
                center = center,
            )
        }
    }

    @RequiresApi(AGSL_MIN_SDK)
    private fun DrawScope.drawShaderGlow(
        s: RuntimeShader,
        center: Offset,
        radius: Float,
        color: Color,
        intensity: Float,
        time: Float,
    ) {
        s.setFloatUniform("uTime", time)
        s.setFloatUniform("uCenter", center.x, center.y)
        s.setFloatUniform("uRadius", radius)
        s.setFloatUniform(
            "uColor",
            color.red,
            color.green,
            color.blue,
            (color.alpha * intensity).coerceIn(0f, 1f),
        )
        val b = brush ?: ShaderBrush(s).also { brush = it }
        // v1.1 (defect D5): the rect must be wide enough that the shader has
        // already faded to zero alpha before its own edge, otherwise the crop
        // IS the edge and the creature sits on a hard-edged square. The
        // shader's outer falloff is edge*2 where edge = uRadius*(1.35 +
        // ripple) and ripple <= RIPPLE_MAX, so alpha survives out to
        // 2*(1.35 + 0.24) = 3.18 radii — while the corner of the old
        // 2.2-radius half-side rect sat at only 2.2*sqrt(2) = 3.11. That rect
        // clipped a live gradient on all four sides. HALF_SIDE clears the
        // diagonal with margin; GlowSkinFalloffTest proves it.
        drawRect(
            brush = b,
            topLeft = Offset(center.x - radius * HALF_SIDE, center.y - radius * HALF_SIDE),
            size =
                androidx.compose.ui.geometry
                    .Size(radius * HALF_SIDE * 2f, radius * HALF_SIDE * 2f),
        )
    }

    internal companion object {
        /** AGSL arrived in Android 13; below it the gradient fallback runs. */
        const val AGSL_MIN_SDK = 33

        /** Upper bound of the shader's `ripple` term; mirrored in AGSL below. */
        const val RIPPLE_MAX = 0.24f

        /** Where the shader's alpha reaches zero, in radii: 2*(1.35 + ripple). */
        const val FALLOFF_RADII = 2f * (1.35f + RIPPLE_MAX)

        /**
         * Half-side of the rect the shader is painted into, in radii. Must
         * exceed FALLOFF_RADII so the fade completes before the crop; the
         * corner of the rect is HALF_SIDE*sqrt(2) away, which is further
         * still. Proven by GlowSkinFalloffTest.
         */
        const val HALF_SIDE = 3.4f

        /** Where the software fallback's radial gradient reaches Transparent. */
        const val SOFTWARE_FALLOFF_RADII = 2.2f

        /**
         * Radial glow with a breathing, noise-rippled rim. Cheap: one hash
         * noise sample per pixel inside the glow rect only.
         */
        const val AGSL_SOURCE = """
            uniform float uTime;
            uniform float2 uCenter;
            uniform float uRadius;
            uniform float4 uColor;

            float hash(float2 p) {
                return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
            }

            float noise(float2 p) {
                float2 i = floor(p);
                float2 f = fract(p);
                float2 u = f * f * (3.0 - 2.0 * f);
                return mix(
                    mix(hash(i), hash(i + float2(1.0, 0.0)), u.x),
                    mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x),
                    u.y);
            }

            half4 main(float2 fragCoord) {
                float2 d = fragCoord - uCenter;
                float dist = length(d);
                float angle = atan(d.y, d.x);
                float ripple = noise(float2(angle * 2.2 + uTime * 0.35, uTime * 0.22)) * 0.24;
                float edge = uRadius * (1.35 + ripple);
                float fall = 1.0 - smoothstep(uRadius * 0.25, edge * 2.0, dist);
                float core = 1.0 - smoothstep(0.0, uRadius * 0.75, dist);
                float a = clamp(fall * 0.5 + core * 0.6, 0.0, 1.0) * uColor.a;
                return half4(uColor.rgb * a, a);
            }
        """
    }
}
