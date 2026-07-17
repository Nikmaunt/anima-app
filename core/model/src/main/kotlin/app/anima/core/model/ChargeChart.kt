package app.anima.core.model

/**
 * v0.3 diary chart math — pure and table-testable. Samples are sparse
 * (foreground-only, ~10 min apart while the app is open); the chart draws
 * honest gaps instead of pretending continuity, and the storm↔drain
 * correlation only speaks when both sides have enough data.
 */
object ChargeChart {
    data class Sample(
        val atMillis: Long,
        val percent: Int,
    )

    /** A polyline segment; a gap wider than [MAX_GAP_MILLIS] starts a new one. */
    fun segments(samples: List<Sample>): List<List<Sample>> {
        if (samples.isEmpty()) return emptyList()
        val sorted = samples.sortedBy { it.atMillis }
        val result = mutableListOf<MutableList<Sample>>(mutableListOf(sorted.first()))
        for (sample in sorted.drop(1)) {
            if (sample.atMillis - result.last().last().atMillis > MAX_GAP_MILLIS) {
                result += mutableListOf(sample)
            } else {
                result.last() += sample
            }
        }
        return result
    }

    /**
     * Ratio of mean discharge speed (%/h) inside storm windows vs outside;
     * null when either side lacks [MIN_INTERVALS] discharge intervals.
     * Charging intervals (percent rising) are excluded from both sides.
     */
    fun stormDrainRatio(
        samples: List<Sample>,
        stormAtMillis: List<Long>,
    ): Double? {
        val sorted = samples.sortedBy { it.atMillis }
        var stormSum = 0.0
        var stormN = 0
        var quietSum = 0.0
        var quietN = 0
        for (i in 1 until sorted.size) {
            val a = sorted[i - 1]
            val b = sorted[i]
            val dtHours = (b.atMillis - a.atMillis) / HOUR_MILLIS.toDouble()
            val drop = a.percent - b.percent
            // Only true discharge intervals inside the honesty gap count —
            // charging/flat stretches belong to neither side.
            val usable = dtHours > 0.0 && b.atMillis - a.atMillis <= MAX_GAP_MILLIS && drop > 0
            if (usable) {
                val rate = drop / dtHours
                val inStorm =
                    stormAtMillis.any { storm ->
                        a.atMillis <= storm + STORM_WINDOW_MILLIS && b.atMillis >= storm
                    }
                if (inStorm) {
                    stormSum += rate
                    stormN++
                } else {
                    quietSum += rate
                    quietN++
                }
            }
        }
        if (stormN < MIN_INTERVALS || quietN < MIN_INTERVALS) return null
        val quietMean = quietSum / quietN
        if (quietMean <= 0.0) return null
        return (stormSum / stormN) / quietMean
    }

    const val MAX_GAP_MILLIS = 45L * 60 * 1000
    const val STORM_WINDOW_MILLIS = 30L * 60 * 1000
    const val HOUR_MILLIS = 60L * 60 * 1000
    const val MIN_INTERVALS = 3
}
