package app.anima.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WeatherFeelingTest {
    private val hour = 60L * 60 * 1000

    private fun log(vararg pairs: Pair<Long, Float>) = pairs.map { PressureSample(it.first, it.second) }

    @Test
    fun `too little history keeps the feeling silent`() {
        assertThat(WeatherFeeling.feel(emptyList(), 0L)).isEqualTo(WeatherSense.UNKNOWN)
        val short = log(0L to 1010f, hour to 1008f)
        assertThat(WeatherFeeling.feel(short, hour)).isEqualTo(WeatherSense.UNKNOWN)
    }

    @Test
    fun `a fast three-hour fall reads as coming rain`() {
        val samples = log(0L to 1012f, 3 * hour to 1009f)
        assertThat(WeatherFeeling.feel(samples, 3 * hour)).isEqualTo(WeatherSense.FALLING)
    }

    @Test
    fun `a fast rise reads as clearing up`() {
        val samples = log(0L to 1004f, 4 * hour to 1011f)
        assertThat(WeatherFeeling.feel(samples, 4 * hour)).isEqualTo(WeatherSense.RISING)
    }

    @Test
    fun `a flat pressure stays steady and quiet`() {
        val samples = log(0L to 1010f, 2 * hour to 1010.4f, 5 * hour to 1009.8f)
        assertThat(WeatherFeeling.feel(samples, 5 * hour)).isEqualTo(WeatherSense.STEADY)
    }

    @Test
    fun `samples older than the window fall out of the trend`() {
        // A huge fall 20h ago must not haunt today's steady readings.
        val samples = log(0L to 1030f, 20 * hour to 1010f, 23 * hour to 1010.2f)
        assertThat(WeatherFeeling.feel(samples, 23 * hour)).isEqualTo(WeatherSense.STEADY)
    }

    @Test
    fun `append enforces spacing plausibility and the cap`() {
        var log = emptyList<PressureSample>()
        log = WeatherFeeling.append(log, PressureSample(0L, 1010f))
        // Too soon — dropped.
        log = WeatherFeeling.append(log, PressureSample(5L * 60 * 1000, 1011f))
        assertThat(log).hasSize(1)
        // Implausible reading — dropped.
        log = WeatherFeeling.append(log, PressureSample(hour, 42f))
        assertThat(log).hasSize(1)
        // Honest new sample lands.
        log = WeatherFeeling.append(log, PressureSample(hour, 1009f))
        assertThat(log).hasSize(2)
    }

    @Test
    fun `codec roundtrips and survives junk`() {
        val samples = log(1L to 1010.25f, hour to 1009.5f)
        val decoded = WeatherFeeling.decode(WeatherFeeling.encode(samples))
        assertThat(decoded).isEqualTo(samples)
        assertThat(WeatherFeeling.decode("garbage;1:notafloat;;")).isEmpty()
        assertThat(WeatherFeeling.decode("")).isEmpty()
    }
}
