package app.anima.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EvolutionTest {
    private val day = RelationshipStats.DAY_MILLIS

    private fun stats(
        days: Long,
        facts: Int = 0,
        conversations: Int = 0,
    ): RelationshipStats =
        RelationshipStats(
            hatchedAtMillis = 0L,
            conversationCount = conversations,
            liveFactCount = facts,
            chargeCount = 0,
        ).also { check(it.daysTogether((days - 1) * day + 1) == days) }

    @Test
    fun `fresh creature is newborn`() {
        assertThat(Evolution.stageOf(stats(1), 1L)).isEqualTo(LifeStage.NEWBORN)
    }

    @Test
    fun `a week together makes it young even with no facts`() {
        val now = 7 * day
        assertThat(Evolution.stageOf(stats(8), now)).isEqualTo(LifeStage.YOUNG)
    }

    @Test
    fun `facts and conversations accelerate but do not skip the arc`() {
        // Day 2, 30 facts, 50 conversations: score = 2 + 10 + 5 = 17 → YOUNG.
        val now = 1 * day + 1
        val s = RelationshipStats(0L, conversationCount = 50, liveFactCount = 30, chargeCount = 0)
        assertThat(Evolution.stageOf(s, now)).isEqualTo(LifeStage.YOUNG)
    }

    @Test
    fun `long rich bond reaches wise`() {
        val now = 130 * day
        val s = RelationshipStats(0L, conversationCount = 300, liveFactCount = 80, chargeCount = 0)
        assertThat(Evolution.stageOf(s, now)).isEqualTo(LifeStage.WISE)
    }

    @Test
    fun `stages only accumulate - no decay anti-tamagotchi`() {
        // Same stats, later date: score can only grow (days grow, nothing decays).
        val s = RelationshipStats(0L, conversationCount = 10, liveFactCount = 10, chargeCount = 0)
        val early = Evolution.score(s, 10 * day)
        val late = Evolution.score(s, 200 * day)
        assertThat(late).isAtLeast(early)
    }

    @Test
    fun `growth is monotonic and bounded`() {
        var last = -1f
        for (days in 1..400 step 7) {
            val g = Evolution.growthOf(stats(days.toLong()), days * day)
            assertThat(g).isAtLeast(last)
            assertThat(g).isAtMost(1f)
            last = g
        }
    }

    @Test
    fun `timeline contains hatch first meal and day milestones in order`() {
        val journal =
            listOf(
                BodyJournalEntry("j2", JournalKind.CHARGE_START, 5 * day),
                BodyJournalEntry("j3", JournalKind.MIND_AWAKENED, 40 * day),
            )
        val moments =
            StoryTimeline.build(
                hatchedAtMillis = 0L,
                journal = journal,
                facts = emptyList(),
                conversationCount = 0,
                nowMillis = 200 * day,
            )
        // v1.1c defect D4: moments carry a kind and a number now, not a
        // finished English sentence — core/model has no res/ and never should
        // have been choosing words. The two 'days together' entries are told
        // apart by their amount, which the old string comparison did by prose.
        assertThat(moments.map { it.kind to it.amount })
            .containsExactly(
                StoryMomentKind.HATCHED to null,
                StoryMomentKind.FIRST_CHARGE to null,
                StoryMomentKind.DAYS_TOGETHER to 30,
                StoryMomentKind.MIND_AWAKENED to null,
                StoryMomentKind.DAYS_TOGETHER to 100,
            ).inOrder()
    }

    @Test
    fun `future milestones never appear`() {
        val moments =
            StoryTimeline.build(
                hatchedAtMillis = 0L,
                journal = emptyList(),
                facts = emptyList(),
                conversationCount = 0,
                nowMillis = 3 * day,
            )
        assertThat(moments.map { it.kind }).containsExactly(StoryMomentKind.HATCHED)
    }
}
