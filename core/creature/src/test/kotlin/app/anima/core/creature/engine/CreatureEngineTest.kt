package app.anima.core.creature.engine

import app.anima.core.model.CreatureGenome
import app.anima.core.model.Mood
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The engine is a pure function of (seed, events, frame times) — these tests
 * drive it with synthetic frames and read the pose. No Robolectric needed.
 */
class CreatureEngineTest {
    private fun engine(seed: Long = 42L) = CreatureEngine(seed, CreatureGenome.from(seed))

    private fun CreatureEngine.run(
        seconds: Float,
        fps: Int = 60,
    ) {
        val frames = (seconds * fps).toInt()
        val stepNanos = 1_000_000_000L / fps
        var t = 1_000_000_000L
        advance(t) // anchor frame
        repeat(frames) {
            t += stepNanos
            advance(t)
        }
    }

    @Test
    fun `breath oscillates within bounds`() {
        val e = engine()
        var min = Float.MAX_VALUE
        var max = Float.MIN_VALUE
        var t = 1_000_000_000L
        e.advance(t)
        repeat(600) {
            t += 16_666_667L
            e.advance(t)
            min = minOf(min, e.pose.breath)
            max = maxOf(max, e.pose.breath)
        }
        assertThat(min).isAtLeast(0f)
        assertThat(max).isAtMost(1.4f)
        assertThat(max - min).isGreaterThan(0.5f) // it actually breathes
    }

    @Test
    fun `blinks happen within six seconds and eyes reopen`() {
        val e = engine()
        var sawClosed = false
        var t = 1_000_000_000L
        e.advance(t)
        repeat(6 * 60) {
            t += 16_666_667L
            e.advance(t)
            if (e.pose.blinkLeft > 0.9f) sawClosed = true
        }
        assertThat(sawClosed).isTrue()
        e.run(1f)
        assertThat(e.pose.blinkLeft).isLessThan(0.5f)
    }

    @Test
    fun `same seed same trajectory - determinism`() {
        val a = engine(7L)
        val b = engine(7L)
        a.run(3.7f)
        b.run(3.7f)
        assertThat(a.pose.offsetX).isEqualTo(b.pose.offsetX)
        assertThat(a.pose.gazeX).isEqualTo(b.pose.gazeX)
        assertThat(a.pose.breath).isEqualTo(b.pose.breath)
    }

    @Test
    fun `different seeds diverge`() {
        val a = engine(1L)
        val b = engine(2L)
        a.run(3f)
        b.run(3f)
        assertThat(a.pose.offsetX).isNotEqualTo(b.pose.offsetX)
    }

    @Test
    fun `asleep closes eyes and slows everything`() {
        val e = engine()
        e.setMood(Mood.ASLEEP)
        e.run(4f)
        assertThat(e.pose.blinkLeft).isEqualTo(1f)
        assertThat(e.pose.blinkRight).isEqualTo(1f)
        assertThat(e.lowPower).isTrue()
    }

    @Test
    fun `tap while asleep rouses one eye then re-sleeps`() {
        val e = engine()
        e.setMood(Mood.ASLEEP)
        e.run(2f)
        e.onTap()
        e.run(0.5f)
        assertThat(e.pose.blinkLeft).isEqualTo(0.5f)
        assertThat(e.pose.blinkRight).isEqualTo(1f)
        e.run(5f) // rouse window over
        assertThat(e.pose.blinkLeft).isEqualTo(1f)
    }

    @Test
    fun `tap produces a squash episode that settles`() {
        val e = engine()
        e.run(1f)
        e.onTap()
        var maxAbs = 0f
        var t = 2_000_000_000L
        repeat(120) {
            t += 16_666_667L
            e.advance(t)
            maxAbs = maxOf(maxAbs, kotlin.math.abs(e.pose.squash))
        }
        assertThat(maxAbs).isGreaterThan(0.05f) // visibly reacted
        e.run(4f)
        assertThat(kotlin.math.abs(e.pose.squash)).isLessThan(0.01f) // settled
    }

    @Test
    fun `gaze follows touch target`() {
        val e = engine()
        e.run(0.5f)
        e.onLookAt(0.8f, -0.4f)
        e.run(1f)
        assertThat(e.pose.gazeX).isWithin(0.15f).of(0.8f)
        assertThat(e.pose.gazeY).isWithin(0.15f).of(-0.4f)
    }

    @Test
    fun `reduced motion is calm statics with zero wander`() {
        val e = engine()
        e.setReducedMotion(true)
        e.run(5f)
        assertThat(e.pose.offsetX).isEqualTo(0f)
        assertThat(e.pose.offsetY).isEqualTo(0f)
        assertThat(e.pose.squash).isEqualTo(0f)
        assertThat(e.pose.breath).isEqualTo(0.35f)
    }

    @Test
    fun `reduced motion still blinks occasionally`() {
        val e = engine()
        e.setReducedMotion(true)
        var sawBlink = false
        var t = 1_000_000_000L
        e.advance(t)
        repeat(8 * 60) {
            t += 16_666_667L
            e.advance(t)
            if (e.pose.blinkLeft > 0.5f) sawBlink = true
        }
        assertThat(sawBlink).isTrue()
    }

    @Test
    fun `frame gap is capped - no teleport after backgrounding`() {
        val e = engine()
        e.run(1f)
        val before = e.timeSeconds
        // A 10-minute gap between frames must advance internal time ≤ 100 ms.
        e.advance(700_000_000_000L)
        assertThat(e.timeSeconds - before).isAtMost(0.11f)
    }

    @Test
    fun `mood transition eases energy not jumps`() {
        val e = engine()
        e.run(2f)
        val alertEnergy = e.pose.energy
        e.setMood(Mood.ASLEEP)
        // One frame later energy must not have jumped to the asleep value.
        e.run(0.05f)
        assertThat(e.pose.energy).isGreaterThan(0.5f)
        assertThat(alertEnergy).isGreaterThan(0.9f)
        e.run(6f)
        assertThat(e.pose.energy).isLessThan(0.35f)
    }
}
