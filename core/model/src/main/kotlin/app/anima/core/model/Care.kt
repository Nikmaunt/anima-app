package app.anima.core.model

/**
 * Battery care as creature care (v0.4, research-v4 §9). Pure analysis over
 * the week's body journal; every rule is grounded in the verified findings:
 *
 * - Chronic dwell at 100% (esp. overnight) is real stress; the OS ships
 *   protection modes for exactly this (Samsung Battery protection, Pixel
 *   adaptive/80% — recommended BY NAME, we never reimplement them).
 * - Deep discharge habit burns cycle life; occasional 0% is only "tiring".
 * - Heat is enemy #1 — worse than any charging habit.
 * - Small top-ups beat full cycles (micro-cycling ~doubles lifetime); an
 *   occasional full charge keeps the fuel gauge calibrated.
 * - NEVER: memory-effect myths, doom thresholds, fake health percentages.
 *
 * Scoring is growth-only (Finch ethics): care points accumulate, nothing
 * subtracts, a bad week simply earns fewer points and one gentle advice.
 */
object CareAnalyzer {
    data class WeekInput(
        /** CHARGE_START count. */
        val charges: Int,
        /** FULLY_FED events (reached 100%) with their epoch millis. */
        val fullFeedAtMillis: List<Long>,
        /** BODY_SAMPLE battery percents seen during the week. */
        val samplePercents: List<Int>,
        /** RAN_HOT events. */
        val hotMoments: Int,
        /** Completed rest sessions this week. */
        val restSessions: Int,
        /** Local hour (0..23) resolver for night detection — injectable for tests. */
        val hourOf: (Long) -> Int,
    )

    data class CareWeek(
        val carePoints: Int,
        val gentleWeek: Boolean,
        val nightFullFeeds: Int,
        val deepDips: Int,
        val advice: CareAdvice?,
    )

    enum class CareAdvice {
        /** ≥3 night 100%-feeds: mention the OS protection mode, by name. */
        NIGHT_PROTECTION_EXISTS,

        /** ≥2 deep dips (≤10%): deep runs tire the body. */
        DEEP_DIPS_TIRE,

        /** Any RAN_HOT while charging week: heat is the real enemy. */
        HEAT_HURTS_MOST,

        /** No advice needed — celebrate the gentle week. */
        NONE,
    }

    const val DEEP_DIP_PERCENT = 10
    const val NIGHT_FROM_HOUR = 23
    const val NIGHT_TO_HOUR = 6
    const val CARE_POINTS_CAP = 100

    fun analyze(input: WeekInput): CareWeek {
        val nightFeeds =
            input.fullFeedAtMillis.count { at ->
                val h = input.hourOf(at)
                h >= NIGHT_FROM_HOUR || h < NIGHT_TO_HOUR
            }
        val dayFeeds = input.fullFeedAtMillis.size - nightFeeds
        val deepDips = input.samplePercents.count { it in 0..DEEP_DIP_PERCENT }
        val gentleCharges = (input.charges - input.fullFeedAtMillis.size).coerceAtLeast(0)

        // Growth-only: positives add, negatives simply do not add.
        val points =
            (
                gentleCharges * POINTS_GENTLE_CHARGE +
                    dayFeeds * POINTS_DAY_FULL_FEED +
                    input.restSessions * POINTS_REST_SESSION
            ).coerceAtMost(CARE_POINTS_CAP)

        // One advice at a time, most impactful first (heat > night > dips).
        val advice =
            when {
                input.hotMoments > 0 -> CareAdvice.HEAT_HURTS_MOST
                nightFeeds >= NIGHT_FEED_ADVICE_AT -> CareAdvice.NIGHT_PROTECTION_EXISTS
                deepDips >= DEEP_DIP_ADVICE_AT -> CareAdvice.DEEP_DIPS_TIRE
                else -> CareAdvice.NONE
            }

        return CareWeek(
            carePoints = points,
            gentleWeek = advice == CareAdvice.NONE && input.charges > 0,
            nightFullFeeds = nightFeeds,
            deepDips = deepDips,
            advice = advice.takeIf { it != CareAdvice.NONE },
        )
    }

    const val POINTS_GENTLE_CHARGE = 4
    const val POINTS_DAY_FULL_FEED = 1
    const val POINTS_REST_SESSION = 5
    const val NIGHT_FEED_ADVICE_AT = 3
    const val DEEP_DIP_ADVICE_AT = 2
}
