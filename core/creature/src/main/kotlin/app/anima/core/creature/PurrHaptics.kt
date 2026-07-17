package app.anima.core.creature

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Petting haptics. PRIMITIVE_LOW_TICK trains are the "purr" (research §B4);
 * the mandatory support check runs once — an unsupported primitive would make
 * the whole composition silent, and the design rule is silence over a
 * wrong-feeling buzz. Tap feedback uses view-level haptic constants instead
 * (no permission path); this class covers only the composed purr.
 */
class PurrHaptics(
    context: Context,
) {
    private val vibrator: Vibrator =
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator

    private val lowTickSupported: Boolean =
        Build.VERSION.SDK_INT >= 31 &&
            vibrator.hasVibrator() &&
            vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)

    // v0.3 haptic map (product-research §3): every rich moment gates on its
    // OWN primitives — silence over a wrong-feeling fallback buzz.
    private val celebrateSupported: Boolean =
        Build.VERSION.SDK_INT >= 31 &&
            vibrator.hasVibrator() &&
            vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                VibrationEffect.Composition.PRIMITIVE_CLICK,
            )

    private val thudSupported: Boolean =
        Build.VERSION.SDK_INT >= 31 &&
            vibrator.hasVibrator() &&
            vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_THUD)

    private var lastPurrAtMillis = 0L

    /**
     * One soft purr burst (3 low ticks, sinusoidal scales). Rate-limited so a
     * continuous drag purrs as a texture, not a rattle.
     */
    fun purr(
        nowMillis: Long,
        intensity: Float,
    ) {
        if (!lowTickSupported) return
        if (nowMillis - lastPurrAtMillis < PURR_MIN_GAP_MILLIS) return
        lastPurrAtMillis = nowMillis
        val base = (0.2f + 0.2f * intensity.coerceIn(0f, 1f))
        val effect =
            VibrationEffect
                .startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, base, 0)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, base * 1.4f, 50)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, base, 50)
                .compose()
        runCatching { vibrator.vibrate(effect) }
    }

    /** Charge festival / birthday: a rise and a bright tap. */
    fun celebrate() {
        if (!celebrateSupported) return
        val effect =
            VibrationEffect
                .startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.6f, 0)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f, 80)
                .compose()
        runCatching { vibrator.vibrate(effect) }
    }

    /** Notification-storm startle: one dull thud, like a flinch. */
    fun startle() {
        if (!thudSupported) return
        val effect =
            VibrationEffect
                .startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.7f, 0)
                .compose()
        runCatching { vibrator.vibrate(effect) }
    }

    private companion object {
        const val PURR_MIN_GAP_MILLIS = 160L
    }
}
