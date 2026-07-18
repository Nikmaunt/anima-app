package app.anima.core.model

/**
 * v0.5 weather feel (ideation-v5 №2): the creature "feels rain in its
 * bones" from the barometer trend — strictly offline, no permissions, and
 * honestly ABSENT on devices without TYPE_PRESSURE. This is a feeling, not
 * a forecast: only a clear multi-hour trend speaks, everything else stays
 * quiet (Zambretti-class trend reading, research-v5 §D).
 */
enum class WeatherSense {
    /** No barometer on this body — the sense does not exist. */
    NONE,

    /** Not enough history yet (needs hours of visits) — silent. */
    UNKNOWN,

    /** Pressure falling fast: bones ache, rain is probably coming. */
    FALLING,

    /** No strong trend — silent. */
    STEADY,

    /** Pressure rising fast: it will likely clear up. */
    RISING,
}

data class PressureSample(
    val atMillis: Long,
    val hPa: Float,
)

/**
 * Pure trend logic + the tiny persistence codec. Sampling happens only while
 * the UI is on screen (core/body), so the log is sparse by design — the
 * feeling only speaks when two samples ≥2 h apart exist inside the window.
 */
object WeatherFeeling {
    const val MIN_SPACING_MILLIS = 20L * 60 * 1000
    const val WINDOW_MILLIS = 12L * 60 * 60 * 1000
    const val MIN_TREND_SPAN_MILLIS = 2L * 60 * 60 * 1000
    const val MAX_SAMPLES = 48

    /** ±1.6 hPa over 3 h is the classic "noticeable change" threshold. */
    const val TREND_HPA_PER_3H = 1.6f

    /** Sanity band: real sea-level-ish readings; junk values are dropped. */
    private const val MIN_PLAUSIBLE_HPA = 850f
    private const val MAX_PLAUSIBLE_HPA = 1100f
    private const val THREE_HOURS_MILLIS = 3f * 60 * 60 * 1000

    fun append(
        log: List<PressureSample>,
        sample: PressureSample,
    ): List<PressureSample> {
        if (sample.hPa !in MIN_PLAUSIBLE_HPA..MAX_PLAUSIBLE_HPA) return prune(log, sample.atMillis)
        val last = log.lastOrNull()
        if (last != null && sample.atMillis - last.atMillis < MIN_SPACING_MILLIS) {
            return prune(log, sample.atMillis)
        }
        return prune(log + sample, sample.atMillis)
    }

    fun feel(
        log: List<PressureSample>,
        nowMillis: Long,
    ): WeatherSense {
        val window = log.filter { nowMillis - it.atMillis <= WINDOW_MILLIS }.sortedBy { it.atMillis }
        val first = window.firstOrNull() ?: return WeatherSense.UNKNOWN
        val lastSample = window.last()
        val span = lastSample.atMillis - first.atMillis
        if (span < MIN_TREND_SPAN_MILLIS) return WeatherSense.UNKNOWN
        val ratePer3h = (lastSample.hPa - first.hPa) / span * THREE_HOURS_MILLIS
        return when {
            ratePer3h <= -TREND_HPA_PER_3H -> WeatherSense.FALLING
            ratePer3h >= TREND_HPA_PER_3H -> WeatherSense.RISING
            else -> WeatherSense.STEADY
        }
    }

    fun encode(log: List<PressureSample>): String = log.joinToString(";") { "${it.atMillis}:${it.hPa}" }

    fun decode(raw: String): List<PressureSample> =
        raw
            .split(';')
            .mapNotNull { chunk ->
                val at = chunk.substringBefore(':').toLongOrNull() ?: return@mapNotNull null
                val hPa = chunk.substringAfter(':', "").toFloatOrNull() ?: return@mapNotNull null
                PressureSample(at, hPa)
            }.sortedBy { it.atMillis }

    private fun prune(
        log: List<PressureSample>,
        nowMillis: Long,
    ): List<PressureSample> =
        log
            .filter { nowMillis - it.atMillis <= WINDOW_MILLIS }
            .sortedBy { it.atMillis }
            .takeLast(MAX_SAMPLES)
}
