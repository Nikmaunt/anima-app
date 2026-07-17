package app.anima.core.creature.render

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * The AGSL "skin" layer (ADR-001): an organic glow with a noise-wobbled rim,
 * used by the orb and the ember. Runtime-gated to API 33+; below that (and in
 * previews) an equivalent radial gradient keeps every concept production-
 * looking. The shader is tiny and creature-bounded — cost scales with the
 * filled rect, not the screen.
 */
class GlowSkin {

    private val shader: RuntimeShader? =
        if (Build.VERSION.SDK_INT >= 33) RuntimeShader(AGSL_SOURCE) else null
    private var brush: ShaderBrush? = null

    /** Call once at surface start to hide first-compile cost (research §A2). */
    fun warmUp(sizePx: Float) {
        shader?.let {
            it.setFloatUniform("uTime", 0f)
            it.setFloatUniform("uCenter", sizePx / 2f, sizePx / 2f)
            it.setFloatUniform("uRadius", sizePx / 4f)
            it.setFloatUniform("uColor", 1f, 1f, 1f, 0f)
        }
    }

    fun DrawScope.drawGlow(
        center: Offset,
        radius: Float,
        color: Color,
        intensity: Float,
        time: Float,
    ) {
        val s = shader
        if (s != null) {
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
            drawRect(
                brush = b,
                topLeft = Offset(center.x - radius * 2.2f, center.y - radius * 2.2f),
                size = androidx.compose.ui.geometry.Size(radius * 4.4f, radius * 4.4f),
            )
        } else {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.55f * intensity),
                        color.copy(alpha = 0.18f * intensity),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius * 2.2f,
                ),
                radius = radius * 2.2f,
                center = center,
            )
        }
    }

    private companion object {
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
