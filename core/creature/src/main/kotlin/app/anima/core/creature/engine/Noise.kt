package app.anima.core.creature.engine

import kotlin.math.floor

/**
 * Seeded 1D value noise: smooth, deterministic, allocation-free. All ambient
 * wander derives from this — a pure function of (time, seed, channel), so the
 * same creature drifts the same way on every device and in every test.
 */
object ValueNoise {
    /** Smooth noise in [-1, 1] at coordinate t (typically seconds). */
    fun noise(
        t: Float,
        seed: Long,
        channel: Int = 0,
    ): Float {
        val cell = floor(t)
        val frac = t - cell
        val a = lattice(cell.toLong(), seed, channel)
        val b = lattice(cell.toLong() + 1, seed, channel)
        val eased = frac * frac * (3f - 2f * frac)
        return a + (b - a) * eased
    }

    /** Two octaves: base wander + faster shimmer at half amplitude. */
    fun fbm2(
        t: Float,
        seed: Long,
        channel: Int = 0,
    ): Float = (noise(t, seed, channel) + 0.5f * noise(t * 2.17f, seed, channel + 101)) / 1.5f

    /** Deterministic lattice value in [-1, 1]. */
    private fun lattice(
        cell: Long,
        seed: Long,
        channel: Int,
    ): Float {
        var h = seed xor (cell * -0x61c8864680b583ebL) xor (channel.toLong() * 0x9E3779B97F4A7C15UL.toLong())
        h = (h xor (h ushr 30)) * -0x40a7b892e31b1a47L
        h = (h xor (h ushr 27)) * -0x6b2fb644ecceee15L
        h = h xor (h ushr 31)
        return ((h ushr 40).toFloat() / (1L shl 23).toFloat()) - 1f
    }

    /** Stable per-index hash in [0, 1) — for scheduled impulses (blinks, darts). */
    fun hash01(
        index: Long,
        seed: Long,
        channel: Int = 0,
    ): Float {
        var h = seed xor (index * 0x9E3779B97F4A7C15UL.toLong()) xor (channel.toLong() shl 17)
        h = (h xor (h ushr 33)) * -0xae502812aa7333L
        h = (h xor (h ushr 28)) * -0x3b314601e57a13adL
        h = h xor (h ushr 32)
        return (h ushr 40).toFloat() / (1L shl 24).toFloat()
    }
}
