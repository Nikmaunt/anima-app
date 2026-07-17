package app.anima.core.creature.engine

import app.anima.core.model.Mood

/**
 * The mood → parameter table from the motion bible. One place, engine-level;
 * concepts inherit these numbers and add only silhouette-specific flavour.
 *
 * @property energy master gain on every amplitude.
 * @property breathsPerMinute breathing rate (research.md §B2).
 * @property breathDepth amplitude multiplier for the breath channel.
 * @property lidClosure 0 = wide open, 1 = closed.
 * @property dartIntervalSeconds mean seconds between eye darts (0 = none).
 * @property wanderAmplitude idle drift as a fraction of body size.
 * @property jitter extra high-frequency noise gain (anxiety shiver).
 */
data class MoodProfile(
    val energy: Float,
    val breathsPerMinute: Float,
    val breathDepth: Float,
    val lidClosure: Float,
    val dartIntervalSeconds: Float,
    val wanderAmplitude: Float,
    val jitter: Float,
    val blinkRateScale: Float,
) {
    val breathCycleSeconds: Float get() = 60f / breathsPerMinute

    companion object {
        fun of(mood: Mood): MoodProfile = when (mood) {
            Mood.ALERT -> MoodProfile(
                energy = 1f, breathsPerMinute = 12f, breathDepth = 1f,
                lidClosure = 0f, dartIntervalSeconds = 3.5f,
                wanderAmplitude = 0.015f, jitter = 0f, blinkRateScale = 1f,
            )
            Mood.BORED -> MoodProfile(
                energy = 0.55f, breathsPerMinute = 10f, breathDepth = 1.1f,
                lidClosure = 0.3f, dartIntervalSeconds = 7f,
                wanderAmplitude = 0.01f, jitter = 0f, blinkRateScale = 0.8f,
            )
            Mood.SLEEPY -> MoodProfile(
                energy = 0.35f, breathsPerMinute = 9f, breathDepth = 1.15f,
                lidClosure = 0.6f, dartIntervalSeconds = 10f,
                wanderAmplitude = 0.007f, jitter = 0f, blinkRateScale = 0.6f,
            )
            Mood.EATING -> MoodProfile(
                energy = 0.9f, breathsPerMinute = 14f, breathDepth = 1.05f,
                lidClosure = 0.25f, dartIntervalSeconds = 5f,
                wanderAmplitude = 0.012f, jitter = 0f, blinkRateScale = 0.9f,
            )
            Mood.ANXIOUS -> MoodProfile(
                energy = 1.2f, breathsPerMinute = 24f, breathDepth = 0.6f,
                lidClosure = 0f, dartIntervalSeconds = 1.6f,
                wanderAmplitude = 0.02f, jitter = 0.6f, blinkRateScale = 1.4f,
            )
            Mood.ASLEEP -> MoodProfile(
                energy = 0.2f, breathsPerMinute = 8f, breathDepth = 1.3f,
                lidClosure = 1f, dartIntervalSeconds = 0f,
                wanderAmplitude = 0.004f, jitter = 0f, blinkRateScale = 0f,
            )
            Mood.HOT -> MoodProfile(
                energy = 0.7f, breathsPerMinute = 26f, breathDepth = 0.5f,
                lidClosure = 0.45f, dartIntervalSeconds = 6f,
                wanderAmplitude = 0.008f, jitter = 0.25f, blinkRateScale = 0.7f,
            )
        }
    }
}
