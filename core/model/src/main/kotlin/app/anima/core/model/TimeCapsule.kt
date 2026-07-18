package app.anima.core.model

/**
 * v0.5 time capsule (ideation-v5 №3): a letter to the future self that the
 * creature holds. Purely local, no alarms — delivery happens on the first
 * visit after the due moment, which is exactly the tempo of the product:
 * the creature waits, it never pings.
 */
data class TimeCapsule(
    val id: String,
    val text: String,
    val createdAtMillis: Long,
    val deliverAtMillis: Long,
    val openedAtMillis: Long?,
) {
    val isOpened: Boolean get() = openedAtMillis != null

    fun isDue(nowMillis: Long): Boolean = !isOpened && deliverAtMillis <= nowMillis

    companion object {
        const val MAX_TEXT_CHARS = 2000

        /** Offered delivery horizons, in days — a week, a month, a season. */
        val HORIZON_DAYS = listOf(7, 30, 90)
    }
}
