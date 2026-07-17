package app.anima.core.model

/**
 * The creature's bodily state. Every concept must play every mood readably.
 * Derivation is deterministic (MoodEngine) — never from the LLM, never random.
 */
enum class Mood {
    /** Bright, curious, following the world. The default day state. */
    ALERT,

    /** Nothing has happened for a while; heavy lids, slow drift, sighs. */
    BORED,

    /** Battery low and not charging: dim, droopy, conserving itself. */
    SLEEPY,

    /** Charging: visibly feeding — gulps of energy, contentment. */
    EATING,

    /** Disk squeeze, offline, or a notification storm: jitter, darting eyes. */
    ANXIOUS,

    /** Local night: curled up, slow breath. A tap rouses it briefly. */
    ASLEEP,

    /** Thermal throttling: flushed, panting, wants to be left alone. */
    HOT,
}

/** Signals + derived mood, the single input to the renderer and the mind. */
data class BodyState(
    val signals: BodySignals,
    val mood: Mood,
) {
    companion object {
        val Resting = BodyState(BodySignals.Resting, Mood.ALERT)
    }
}
