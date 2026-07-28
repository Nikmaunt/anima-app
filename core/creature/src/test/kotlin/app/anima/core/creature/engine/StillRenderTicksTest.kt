package app.anima.core.creature.engine

import app.anima.core.model.CreatureGenome
import app.anima.core.model.Mood
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Audit-v10 finding 0.3-A, as a failing build.
 *
 * `CreatureEngine.advance` treats `lastFrameNanos == Long.MIN_VALUE` as "first
 * frame ever": it anchors the clock and returns without simulating. Because
 * `StillRender` called `onResume()` (which sets exactly that) and then a
 * single `advance`, the static frame ran zero ticks — so `advanceReduced`,
 * the only place that writes `pose.energy`, `pose.lidDroop` and `pose.flush`,
 * never ran, and every widget frame carried the default ALERT pose.
 *
 * This tests the engine contract directly rather than through a bitmap, so it
 * says WHY the goldens changed instead of merely that they did.
 */
class StillRenderTicksTest {
    private fun engineFor(mood: Mood): CreatureEngine {
        val genome = CreatureGenome.from(SEED)
        return CreatureEngine(SEED, genome).apply {
            setReducedMotion(true)
            setMood(mood)
            onResume()
        }
    }

    @Test
    fun `a single advance after onResume simulates nothing`() {
        // The old StillRender recipe, preserved so the bug cannot come back
        // unnoticed: one advance straight after onResume is a no-op.
        val asleep = engineFor(Mood.ASLEEP)
        asleep.advance(TICK)
        val alert = engineFor(Mood.ALERT)
        alert.advance(TICK)
        assertThat(asleep.pose.lidDroop).isEqualTo(alert.pose.lidDroop)
    }

    @Test
    fun `anchor then tick makes the pose follow the mood`() {
        val asleep = engineFor(Mood.ASLEEP)
        asleep.advance(0L)
        asleep.advance(TICK)

        val alert = engineFor(Mood.ALERT)
        alert.advance(0L)
        alert.advance(TICK)

        // A sleeping creature's eyes are more closed than an alert one's.
        // This is the observable the audit called out: ember-asleep-night.png
        // had its eyes open.
        assertThat(asleep.pose.lidDroop).isGreaterThan(alert.pose.lidDroop)
    }

    @Test
    fun `energy separates the moods once a tick actually runs`() {
        val poses =
            Mood.entries.associateWith { mood ->
                engineFor(mood).run {
                    advance(0L)
                    advance(TICK)
                    pose.energy
                }
            }
        // Not all seven need to differ, but a static frame that cannot tell
        // SLEEPY from ALERT is the defect this fix exists for.
        assertThat(poses[Mood.SLEEPY]).isLessThan(poses[Mood.ALERT])
    }

    @Test
    fun `hot is the only mood that flushes`() {
        Mood.entries.forEach { mood ->
            val flush =
                engineFor(mood).run {
                    advance(0L)
                    advance(TICK)
                    pose.flush
                }
            if (mood == Mood.HOT) {
                assertThat(flush).isEqualTo(1f)
            } else {
                assertThat(flush).isEqualTo(0f)
            }
        }
    }

    private companion object {
        const val SEED = 424_242L
        const val TICK = 16_000_000L
    }
}
