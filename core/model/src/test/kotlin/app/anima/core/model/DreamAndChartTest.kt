package app.anima.core.model

import app.anima.core.model.ChargeChart.Sample
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DreamAndChartTest {
    private val quiet = DreamWeaver.DayEcho(charges = 0, storms = 0, ranHot = 0, fullyFed = false)

    @Test
    fun `dreams are deterministic - same night, same dream`() {
        val a = DreamWeaver.weave(seed = 42L, epochDay = 20_000L, echo = quiet)
        val b = DreamWeaver.weave(seed = 42L, epochDay = 20_000L, echo = quiet)
        assertThat(a).isEqualTo(b)
        assertThat(a).isNotEmpty()
    }

    @Test
    fun `a new night weaves a different dream eventually`() {
        val dreams = (0L..5L).map { DreamWeaver.weave(42L, 20_000L + it, quiet) }.toSet()
        assertThat(dreams.size).isGreaterThan(1)
    }

    @Test
    fun `the day's events color the dream`() {
        val stormy = DreamWeaver.weave(1L, 1L, quiet.copy(storms = 3))
        val feast = DreamWeaver.weave(1L, 1L, quiet.copy(charges = 4))
        assertThat(stormy).isNotEqualTo(feast)
    }

    @Test
    fun `full belly earns its closing line`() {
        val fed = DreamWeaver.weave(7L, 7L, quiet.copy(fullyFed = true))
        assertThat(fed.contains("full") || fed.contains("fed")).isTrue()
    }

    // --- ChargeChart ---

    @Test
    fun `segments split on gaps wider than the honesty threshold`() {
        val h = ChargeChart.HOUR_MILLIS
        val samples =
            listOf(
                Sample(0, 90),
                Sample(h / 4, 85),
                // 3h silence — we were apart; the line must break.
                Sample(3 * h, 70),
                Sample(3 * h + h / 4, 68),
            )
        val segments = ChargeChart.segments(samples)
        assertThat(segments).hasSize(2)
        assertThat(segments[0].map { it.percent }).containsExactly(90, 85).inOrder()
    }

    @Test
    fun `storm drain ratio needs enough data on both sides`() {
        val h = ChargeChart.HOUR_MILLIS
        val sparse = listOf(Sample(0, 90), Sample(h / 2, 80))
        assertThat(ChargeChart.stormDrainRatio(sparse, listOf(0L))).isNull()
    }

    @Test
    fun `storm drain ratio sees faster storm-hour discharge`() {
        val q = ChargeChart.HOUR_MILLIS / 2 // 30-min steps
        val samples = mutableListOf<Sample>()
        // Quiet hours: 1% per 30 min (2%/h) — intervals 0..5.
        var pct = 100
        for (i in 0..5) {
            samples += Sample(i * q, pct)
            pct -= 1
        }
        // Storm hours: 3% per 30 min (6%/h) — a storm sits inside each interval.
        val stormStart = 6 * q
        val storms = mutableListOf<Long>()
        for (i in 0..5) {
            samples += Sample(stormStart + i * q, pct)
            storms += stormStart + i * q
            pct -= 3
        }
        val ratio = ChargeChart.stormDrainRatio(samples, storms)
        assertThat(ratio).isNotNull()
        assertThat(ratio!!).isGreaterThan(2.0)
    }

    @Test
    fun `charging intervals never count as discharge`() {
        val q = ChargeChart.HOUR_MILLIS / 2
        val samples =
            (0..8).map { i -> Sample(i * q, 50 + i * 5) } // pure charging
        assertThat(ChargeChart.stormDrainRatio(samples, emptyList())).isNull()
    }
}
