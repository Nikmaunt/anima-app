package app.anima.core.model

/**
 * Deterministic mood derivation. Pure function of signals + interaction
 * recency; no clock reads, no randomness — fully table-testable.
 *
 * Precedence (first match wins), documented and locked by tests:
 *  1. HOT      — the body is throttling; overrides everything.
 *  2. EATING   — charging is always visible (the creature wakes up to eat).
 *  3. SLEEPY   — battery critically low and not charging.
 *  4. ANXIOUS  — disk squeeze, offline, or a notification storm.
 *  5. ASLEEP   — local night.
 *  6. BORED    — awake but ignored for a while.
 *  7. ALERT    — otherwise.
 */
object MoodEngine {
    const val SLEEPY_BATTERY_MAX = 20
    const val ANXIOUS_DISK_FREE_FRACTION = 0.10f
    const val STORM_NOTIF_COUNT = 8
    const val NIGHT_START_MINUTE = 23 * 60
    const val NIGHT_END_MINUTE = 7 * 60
    const val BORED_AFTER_MILLIS = 2L * 60 * 1000

    fun derive(signals: BodySignals, interactionIdleMillis: Long): Mood = when {
        signals.thermal >= ThermalSense.HOT -> Mood.HOT
        signals.charging -> Mood.EATING
        signals.batteryPercent <= SLEEPY_BATTERY_MAX -> Mood.SLEEPY
        isAnxious(signals) -> Mood.ANXIOUS
        isNight(signals.minuteOfDay) -> Mood.ASLEEP
        interactionIdleMillis >= BORED_AFTER_MILLIS -> Mood.BORED
        else -> Mood.ALERT
    }

    fun isNight(minuteOfDay: Int): Boolean =
        minuteOfDay >= NIGHT_START_MINUTE || minuteOfDay < NIGHT_END_MINUTE

    private fun isAnxious(signals: BodySignals): Boolean =
        signals.diskFreeFraction < ANXIOUS_DISK_FREE_FRACTION ||
            signals.net == NetSense.OFFLINE ||
            signals.notifRecentCount >= STORM_NOTIF_COUNT ||
            signals.lowMemory
}
