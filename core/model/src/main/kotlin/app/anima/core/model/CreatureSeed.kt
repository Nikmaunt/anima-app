package app.anima.core.model

/**
 * Deterministic PRNG (SplitMix64). Same seed → same creature on every launch;
 * different phones → different creatures. Kept tiny and dependency-free so the
 * motion core stays pure.
 */
class SplitMix64(seed: Long) {
    private var state = seed

    fun nextLong(): Long {
        state += -0x61c8864680b583ebL
        var z = state
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }

    /** Uniform in [0, 1). */
    fun nextFloat(): Float = ((nextLong() ushr 40).toFloat() / (1L shl 24).toFloat())

    /** Uniform in [min, max). */
    fun nextFloat(min: Float, max: Float): Float = min + nextFloat() * (max - min)

    fun nextInt(bound: Int): Int = ((nextLong() ushr 33) % bound).toInt()
}

/**
 * The inherited part of a creature's appearance, derived once from the device
 * seed. Every parameter is bounded so no roll can produce an unreadable
 * creature — the genome individualises, the concept guarantees legibility.
 *
 * @property hueShiftDeg palette rotation, ±18° around the concept's base hue.
 * @property sizeScale body scale 0.92..1.08.
 * @property roundness 0..1, lerps the body silhouette between angular and soft.
 * @property blinkRateScale 0.8..1.3, multiplies the motion-bible blink interval.
 * @property curiosity 0..1, gaze-follow speed and eye-dart frequency.
 * @property patternVariant small integer selecting a marking/pattern variant.
 */
data class CreatureGenome(
    val hueShiftDeg: Float,
    val sizeScale: Float,
    val roundness: Float,
    val blinkRateScale: Float,
    val curiosity: Float,
    val patternVariant: Int,
) {
    companion object {
        const val PATTERN_VARIANTS = 4

        fun from(seed: Long): CreatureGenome {
            val rng = SplitMix64(seed)
            return CreatureGenome(
                hueShiftDeg = rng.nextFloat(-18f, 18f),
                sizeScale = rng.nextFloat(0.92f, 1.08f),
                roundness = rng.nextFloat(),
                blinkRateScale = rng.nextFloat(0.8f, 1.3f),
                curiosity = rng.nextFloat(),
                patternVariant = rng.nextInt(PATTERN_VARIANTS),
            )
        }
    }
}
