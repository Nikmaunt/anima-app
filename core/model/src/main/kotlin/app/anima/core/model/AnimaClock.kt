package app.anima.core.model

import java.time.ZoneId

/**
 * v0.6: the creature's sense of time, as an injectable seam. Time-of-day
 * rituals (the goodnight moon, capsule delivery) branch on wall-clock hour,
 * which made them untestable in the day-in-life E2E — the run happens at
 * whatever hour CI grants it. Production binds [SystemAnimaClock]; the E2E
 * binds an offset clock pinned to an evening. Only *ritual gating* reads
 * this clock; persisted timestamps stay honest wherever the distinction
 * matters to stored data.
 */
interface AnimaClock {
    fun nowMillis(): Long

    fun zone(): ZoneId
}

/** The one true clock outside tests. */
class SystemAnimaClock : AnimaClock {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun zone(): ZoneId = ZoneId.systemDefault()
}
