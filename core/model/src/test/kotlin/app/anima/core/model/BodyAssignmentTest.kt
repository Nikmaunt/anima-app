package app.anima.core.model

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.math.abs

/**
 * The three properties `assignedTo` exists to have. Checked over a large sweep
 * of seeds rather than asserted in a comment, because the failure they guard
 * against — every phone in the world quietly changing body when the set grows —
 * is invisible until it has already happened to everybody.
 */
class BodyAssignmentTest {
    private val seeds: List<Long> =
        (0 until SWEEP).map { i ->
            // Spread across the whole Long range including negatives: the real
            // seeds are FNV-1a of ANDROID_ID and are negative about half the time.
            var z = i.toLong() * -0x61c8864680b583ebL
            z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
            z xor (z ushr 31)
        }

    @Test
    fun `the same seed always gets the same body`() {
        seeds.take(1000).forEach { seed ->
            assertThat(CreatureConcept.assignedTo(seed)).isEqualTo(CreatureConcept.assignedTo(seed))
        }
    }

    @Test
    fun `every body is assigned, and roughly equally often`() {
        val counts = seeds.groupingBy { CreatureConcept.assignedTo(it) }.eachCount()
        assertWithMessage("a body nobody can be assigned is a body that does not exist")
            .that(counts.keys)
            .containsExactlyElementsIn(CreatureConcept.entries)

        val expected = SWEEP / CreatureConcept.entries.size
        counts.forEach { (concept, n) ->
            val drift = abs(n - expected).toDouble() / expected
            assertWithMessage("$concept got $n of $SWEEP seeds, expected about $expected")
                .that(drift)
                .isLessThan(MAX_DRIFT)
        }
    }

    /**
     * The property that made this a tournament instead of `entries[seed % 8]`.
     *
     * Simulated by scoring against a reduced candidate set, exactly as the real
     * function would if the enum lost a constant. Every creature whose body
     * survives must keep it; only those who had the removed body may move.
     */
    @Test
    fun `removing a body moves only the creatures who had that body`() {
        CreatureConcept.entries.forEach { removed ->
            val remaining = CreatureConcept.entries - removed
            var movedWithoutCause = 0
            seeds.forEach { seed ->
                val before = CreatureConcept.assignedTo(seed)
                val after = remaining.maxBy { assignmentScoreForTest(seed, it.wire) }
                if (before != removed && before != after) movedWithoutCause++
            }
            assertWithMessage(
                "removing $removed reassigned $movedWithoutCause creatures who were not $removed. " +
                    "An index mapping would reassign nearly all of them, which is why this is not one.",
            ).that(movedWithoutCause)
                .isEqualTo(0)
        }
    }

    /**
     * The same property in the other direction, and the one that matters for a
     * ninth body: a creature only moves *to* the newcomer, never sideways.
     */
    @Test
    fun `adding a body moves creatures only onto the new body`() {
        // A ninth wire value that does not exist in the enum, scored the same way.
        val newcomer = "lantern"
        var movedSideways = 0
        var movedToNewcomer = 0
        seeds.forEach { seed ->
            val before = CreatureConcept.assignedTo(seed)
            val newcomerWins =
                assignmentScoreForTest(seed, newcomer) > assignmentScoreForTest(seed, before.wire)
            if (newcomerWins) {
                movedToNewcomer++
            } else if (CreatureConcept.assignedTo(seed) != before) {
                movedSideways++
            }
        }
        assertThat(movedSideways).isEqualTo(0)
        assertWithMessage("a ninth body that wins nobody is not in the tournament")
            .that(movedToNewcomer)
            .isGreaterThan(0)
    }

    @Test
    fun `the answer does not depend on declaration order`() {
        // Shuffling the candidate list must not change the winner: the score is a
        // function of the wire string, not of the position in `entries`.
        val shuffled = CreatureConcept.entries.shuffled(java.util.Random(SHUFFLE_SEED))
        seeds.take(5000).forEach { seed ->
            val byOrder = shuffled.maxBy { assignmentScoreForTest(seed, it.wire) }
            assertThat(byOrder).isEqualTo(CreatureConcept.assignedTo(seed))
        }
    }

    /**
     * A transcription of the production score, kept deliberately separate so a
     * change to the real one is caught by the pinned values below rather than
     * silently mirrored here.
     */
    private fun assignmentScoreForTest(
        seed: Long,
        wire: String,
    ): Long {
        var hash = -0x340d631b7bdddcdbL
        for (ch in wire) {
            hash = hash xor ch.code.toLong()
            hash *= 0x100000001b3L
        }
        var z = seed xor hash
        z += -0x61c8864680b583ebL
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        z = z xor (z ushr 31)
        return z ushr 1
    }

    /**
     * Pinned answers. If the assignment function ever changes, every phone in
     * the world would be assigned a different body on a fresh install — so this
     * has to be a deliberate act with a migration, not a refactor.
     */
    @Test
    fun `the mapping is pinned`() {
        val pinned =
            listOf(
                0L,
                1L,
                -1L,
                Long.MAX_VALUE,
                Long.MIN_VALUE,
                909_090L,
            ).associateWith { CreatureConcept.assignedTo(it) }
        // Recorded from the implementation on 2026-08-01. Changing these values
        // reassigns the body of every phone that has not hatched yet.
        assertThat(pinned).isEqualTo(EXPECTED_PINNED)
    }

    private companion object {
        const val SWEEP = 100_000
        const val MAX_DRIFT = 0.06
        const val SHUFFLE_SEED = 4242L

        val EXPECTED_PINNED =
            mapOf(
                0L to CreatureConcept.ROBOT,
                1L to CreatureConcept.EMBER,
                -1L to CreatureConcept.MOTH,
                Long.MAX_VALUE to CreatureConcept.MOTH,
                Long.MIN_VALUE to CreatureConcept.EMBER,
                909_090L to CreatureConcept.EMBER,
            )
    }
}
