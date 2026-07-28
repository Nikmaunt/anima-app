package app.anima.core.creature.engine

import app.anima.core.model.CreatureGenome
import app.anima.core.model.Mood
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

/**
 * The two motion-bible items v1.1 adds (docs/research-v11-design.md §1.6):
 * microsaccades, and a breathing squash that preserves volume.
 *
 * Both are asserted on the engine rather than on a picture, because both are
 * small enough that a golden would pass or fail them for the wrong reasons.
 */
class MicroMotionTest {
    private fun engine(mood: Mood = Mood.ALERT): CreatureEngine =
        CreatureEngine(SEED, CreatureGenome.from(SEED)).apply {
            setMood(mood)
            advance(0L)
        }

    private fun CreatureEngine.runFor(
        seconds: Float,
        stepMs: Long = 16L,
    ): List<Pair<Float, Float>> {
        val samples = mutableListOf<Pair<Float, Float>>()
        var t = 0L
        val end = (seconds * 1000).toLong()
        while (t < end) {
            t += stepMs
            advance(t * 1_000_000L)
            samples += pose.gazeX to pose.gazeY
        }
        return samples
    }

    @Test
    fun `the eye is never perfectly still while awake`() {
        val samples = engine().runFor(seconds = 6f)
        // Before v1.1 the gaze sat on exactly one value between darts. Now
        // every second brings a new microsaccade offset, so a six-second
        // window must contain several distinct positions.
        val distinctX = samples.map { "%.4f".format(it.first) }.toSet()
        assertThat(distinctX.size).isAtLeast(4)
    }

    @Test
    fun `microsaccades stay small enough to read as life, not as a twitch`() {
        val e = engine()
        val samples = e.runFor(seconds = 6f)
        // Consecutive frames inside one microsaccade interval differ only by
        // the spring settling; the jumps between intervals are what we bound.
        val maxJump =
            samples
                .zipWithNext { a, b ->
                    maxOf(abs(b.first - a.first), abs(b.second - a.second))
                }.max()
        // Well under the dart range (DART_RANGE_X = 0.6f, private to the
        // engine), which is the whole point: a microsaccade must not read as
        // a small dart.
        assertThat(maxJump).isLessThan(0.15f)
    }

    @Test
    fun `a sleeping creature does not microsaccade`() {
        // dartIntervalSeconds <= 0 marks the moods whose eyes are closed or
        // fixed; jittering a closed eye would be noise, not life.
        val samples = engine(Mood.ASLEEP).runFor(seconds = 4f)
        val distinctX = samples.map { "%.4f".format(it.first) }.toSet()
        // Only the spring settling toward its asleep target, then nothing.
        assertThat(distinctX.size).isLessThan(samples.size / 2)
    }

    @Test
    fun `breathing widens the body as it flattens it`() {
        val e = engine()
        var minBreath = Float.MAX_VALUE
        var maxBreath = -Float.MAX_VALUE
        var squashAtMin = 0f
        var squashAtMax = 0f
        var t = 0L
        repeat(400) {
            t += 16L
            e.advance(t * 1_000_000L)
            if (e.pose.breath < minBreath) {
                minBreath = e.pose.breath
                squashAtMin = e.pose.breathSquash
            }
            if (e.pose.breath > maxBreath) {
                maxBreath = e.pose.breath
                squashAtMax = e.pose.breathSquash
            }
        }
        // Volume preservation: the fullest inhale must be narrower than the
        // emptiest exhale. Same body, redistributed — not a body that grows.
        assertThat(maxBreath).isGreaterThan(minBreath)
        assertThat(squashAtMax).isLessThan(squashAtMin)
    }

    private companion object {
        const val SEED = 771_100L
    }
}
