package app.anima.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CareTest {
    private fun input(
        charges: Int = 0,
        fullFeeds: List<Long> = emptyList(),
        samples: List<Int> = emptyList(),
        hot: Int = 0,
        rests: Int = 0,
        hour: Int = 12,
    ) = CareAnalyzer.WeekInput(
        charges = charges,
        fullFeedAtMillis = fullFeeds,
        samplePercents = samples,
        hotMoments = hot,
        restSessions = rests,
        hourOf = { hour },
    )

    @Test
    fun `gentle week earns points and no advice`() {
        val week = CareAnalyzer.analyze(input(charges = 5, fullFeeds = listOf(1L), samples = listOf(40, 70)))
        assertThat(week.advice).isNull()
        assertThat(week.gentleWeek).isTrue()
        assertThat(week.carePoints)
            .isEqualTo(4 * CareAnalyzer.POINTS_GENTLE_CHARGE + CareAnalyzer.POINTS_DAY_FULL_FEED)
    }

    @Test
    fun `night full feeds trigger protection advice at threshold`() {
        val night = CareAnalyzer.analyze(input(charges = 3, fullFeeds = listOf(1L, 2L, 3L), hour = 2))
        assertThat(night.nightFullFeeds).isEqualTo(3)
        assertThat(night.advice).isEqualTo(CareAnalyzer.CareAdvice.NIGHT_PROTECTION_EXISTS)
        val fewer = CareAnalyzer.analyze(input(charges = 2, fullFeeds = listOf(1L, 2L), hour = 2))
        assertThat(fewer.advice).isNull()
    }

    @Test
    fun `deep dips advise gently at two`() {
        val week = CareAnalyzer.analyze(input(charges = 1, samples = listOf(5, 8, 50)))
        assertThat(week.deepDips).isEqualTo(2)
        assertThat(week.advice).isEqualTo(CareAnalyzer.CareAdvice.DEEP_DIPS_TIRE)
    }

    @Test
    fun `heat outranks every other advice`() {
        val week =
            CareAnalyzer.analyze(
                input(charges = 3, fullFeeds = listOf(1L, 2L, 3L), samples = listOf(2, 3), hot = 1, hour = 2),
            )
        assertThat(week.advice).isEqualTo(CareAnalyzer.CareAdvice.HEAT_HURTS_MOST)
    }

    @Test
    fun `points never go negative and are capped`() {
        val bad = CareAnalyzer.analyze(input(charges = 0, samples = listOf(1, 2, 3)))
        assertThat(bad.carePoints).isEqualTo(0)
        val huge = CareAnalyzer.analyze(input(charges = 50, rests = 20))
        assertThat(huge.carePoints).isEqualTo(CareAnalyzer.CARE_POINTS_CAP)
    }

    @Test
    fun `rest sessions feed care points`() {
        val week = CareAnalyzer.analyze(input(charges = 0, rests = 2))
        assertThat(week.carePoints).isEqualTo(2 * CareAnalyzer.POINTS_REST_SESSION)
    }
}
