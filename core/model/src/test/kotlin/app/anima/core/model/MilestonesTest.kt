package app.anima.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MilestonesTest {
    private val day = RelationshipStats.DAY_MILLIS

    private fun stats(
        days: Long = 1,
        facts: Int = 0,
        rests: Int = 0,
        conversations: Int = 0,
    ) = RelationshipStats(
        hatchedAtMillis = 0L,
        conversationCount = conversations,
        liveFactCount = facts,
        chargeCount = 0,
        restSessionCount = rests,
    ) to (days - 1) * day

    @Test
    fun `true self is always unlocked and the default`() {
        val (s, now) = stats()
        assertThat(Milestones.isUnlocked(PaletteVariant.TRUE_SELF, s, now)).isTrue()
        assertThat(PaletteVariant.fromWire(null)).isEqualTo(PaletteVariant.TRUE_SELF)
        assertThat(PaletteVariant.fromWire("garbage")).isEqualTo(PaletteVariant.TRUE_SELF)
    }

    @Test
    fun `unlocks are monotonic in their counters`() {
        val (young, nowYoung) = stats(days = 6)
        assertThat(Milestones.isUnlocked(PaletteVariant.DAWN, young, nowYoung)).isFalse()
        val (week, nowWeek) = stats(days = 7)
        assertThat(Milestones.isUnlocked(PaletteVariant.DAWN, week, nowWeek)).isTrue()
        val (rested, nowR) = stats(rests = 10)
        assertThat(Milestones.isUnlocked(PaletteVariant.MOONLIT, rested, nowR)).isTrue()
        val (learned, nowL) = stats(facts = 100)
        assertThat(Milestones.isUnlocked(PaletteVariant.DEEP_SEA, learned, nowL)).isTrue()
    }

    @Test
    fun `locked selection falls back to zero shift honestly`() {
        val (s, now) = stats(days = 1)
        assertThat(Milestones.effectiveShiftDeg(PaletteVariant.AURORA.wire, s, now)).isEqualTo(0f)
        val (grown, nowG) = stats(days = 40)
        assertThat(Milestones.effectiveShiftDeg(PaletteVariant.AURORA.wire, grown, nowG))
            .isEqualTo(PaletteVariant.AURORA.shiftDeg)
    }

    @Test
    fun `board lists every variant exactly once with honest conditions`() {
        val (s, now) = stats()
        val board = Milestones.board(s, now)
        assertThat(board.map { it.variant }).containsExactlyElementsIn(PaletteVariant.entries)
        // v1.1c defect D4: `condition` was an English sentence built in a JVM
        // module with no res/. What is left is the shape and the threshold, and
        // the honesty claim the old string check stood for is now checkable:
        // every condition that has a number carries a real one, so no shade can
        // advertise itself as "opens at 0".
        board.forEach { unlock ->
            if (unlock.kind != Milestones.UnlockKind.ALWAYS) {
                assertThat(unlock.threshold).isGreaterThan(0)
            }
        }
    }
}
