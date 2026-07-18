package app.anima.feature.wallpaper

/**
 * ADR-012 budget, as decidable code. v0.4 is the strictest form: 0 fps
 * steady state, no transition episodes at all — a redraw is a single frame,
 * and this object decides whether one is allowed.
 */
object WallpaperBudget {
    /** State-driven redraws: at most one per this interval. */
    const val MIN_STATE_REDRAW_INTERVAL_MS = 30_000L

    enum class Reason {
        /** Surface created/changed or became visible — always allowed. */
        SURFACE,

        /** Battery/mood state changed — debounced. */
        STATE,
    }

    /**
     * @param lastStateDrawMs epoch millis of the previous STATE draw, or
     *   null if none happened yet on this engine.
     */
    fun allowsDraw(
        reason: Reason,
        visible: Boolean,
        nowMs: Long,
        lastStateDrawMs: Long?,
    ): Boolean {
        if (!visible) return false
        return when (reason) {
            Reason.SURFACE -> true
            Reason.STATE ->
                lastStateDrawMs == null || nowMs - lastStateDrawMs >= MIN_STATE_REDRAW_INTERVAL_MS
        }
    }
}
