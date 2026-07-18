package app.anima.core.model

/**
 * Rest-together sessions (v0.4 core feature). Pure rules, unit-tested:
 *
 * - The user picks a duration; time accrues only while the rest screen is
 *   visible. Leaving the app PAUSES silently — no guilt, no penalty, the
 *   creature "keeps your place".
 * - Only COMPLETED sessions are recorded (journal kind REST_SESSION);
 *   an abandoned session leaves no trace at all (Finch-ethics, exceeded:
 *   nothing can break because nothing negative is ever written).
 * - Counters shown to the user are derived from the journal and only grow.
 */
object RestSessions {
    /** Offered durations; short first — resting at all beats resting long. */
    val DURATIONS_MIN = listOf(5, 10, 15, 25)

    /** Journal detail codec: "planned:actual" in whole minutes. */
    fun detail(
        plannedMin: Int,
        actualMin: Int,
    ): String = "$plannedMin:$actualMin"

    fun parseDetail(detail: String?): Pair<Int, Int>? {
        val parts = detail?.split(':') ?: return null
        if (parts.size != 2) return null
        val planned = parts[0].toIntOrNull() ?: return null
        val actual = parts[1].toIntOrNull() ?: return null
        if (planned <= 0 || actual < 0) return null
        return planned to actual
    }
}

/**
 * The session state machine, time-explicit so every transition is testable.
 * Ticks come from the UI while it is RESUMED; there is no timer entity
 * anywhere else (entity budget: rest adds NO background work).
 */
sealed interface RestPhase {
    data object Idle : RestPhase

    data class Running(
        val plannedMin: Int,
        val accumulatedMs: Long,
        val anchorMs: Long,
    ) : RestPhase {
        fun elapsedMs(nowMs: Long): Long = accumulatedMs + (nowMs - anchorMs).coerceAtLeast(0)

        fun remainingMs(nowMs: Long): Long = plannedMin * MS_PER_MIN - elapsedMs(nowMs)
    }

    /** The app went away mid-session; the creature waits, nothing is lost. */
    data class Waiting(
        val plannedMin: Int,
        val accumulatedMs: Long,
    ) : RestPhase

    data class Completed(
        val plannedMin: Int,
    ) : RestPhase

    companion object {
        const val MS_PER_MIN = 60_000L
    }
}

/** Pure transitions; the manager just holds state and forwards clock reads. */
object RestClock {
    fun start(
        plannedMin: Int,
        nowMs: Long,
    ): RestPhase.Running = RestPhase.Running(plannedMin, accumulatedMs = 0L, anchorMs = nowMs)

    /** UI tick; returns Completed exactly once when the planned time is met. */
    fun tick(
        phase: RestPhase,
        nowMs: Long,
    ): RestPhase =
        when (phase) {
            is RestPhase.Running ->
                if (phase.remainingMs(nowMs) <= 0) RestPhase.Completed(phase.plannedMin) else phase
            else -> phase
        }

    /** Screen left RESUMED: freeze the accumulated time, wait patiently. */
    fun pause(
        phase: RestPhase,
        nowMs: Long,
    ): RestPhase =
        when (phase) {
            is RestPhase.Running -> RestPhase.Waiting(phase.plannedMin, phase.elapsedMs(nowMs))
            else -> phase
        }

    /** Back on screen: continue from the frozen time, new anchor. */
    fun resume(
        phase: RestPhase,
        nowMs: Long,
    ): RestPhase =
        when (phase) {
            is RestPhase.Waiting -> RestPhase.Running(phase.plannedMin, phase.accumulatedMs, nowMs)
            else -> phase
        }

    /** User walked away for good: no trace, no drama. */
    fun abandon(): RestPhase = RestPhase.Idle
}
