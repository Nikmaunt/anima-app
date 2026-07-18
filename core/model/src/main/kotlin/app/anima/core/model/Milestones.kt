package app.anima.core.model

/**
 * Milestones and discoveries (v0.4). Palette variants unlock from the
 * relationship's only-growing counters — days together, живые facts,
 * completed rest sessions. Ethics by construction:
 *
 * - no payments, no timers, no FOMO: an unlock is a pure function of
 *   monotonic counters, so nothing can ever re-lock or expire;
 * - the soul carries its hatch date through export/import, so unlocks
 *   travel with the creature;
 * - locked variants show their condition honestly ("30 days together"),
 *   never a countdown.
 */
enum class PaletteVariant(
    val wire: String,
    val shiftDeg: Float,
    val label: String,
) {
    TRUE_SELF("true_self", 0f, "True self"),
    DAWN("dawn", 38f, "Dawn"),
    AURORA("aurora", 120f, "Aurora"),
    DEEP_SEA("deep_sea", -120f, "Deep sea"),
    MOONLIT("moonlit", 75f, "Moonlit"),
    EMBERWISE("emberwise", -45f, "Emberwise"),
    ;

    companion object {
        fun fromWire(wire: String?): PaletteVariant = entries.firstOrNull { it.wire == wire } ?: TRUE_SELF
    }
}

object Milestones {
    data class Unlock(
        val variant: PaletteVariant,
        val condition: String,
        val achieved: Boolean,
    )

    const val DAWN_AT_DAYS = 7L
    const val AURORA_AT_DAYS = 30L
    const val DEEP_SEA_AT_FACTS = 100
    const val MOONLIT_AT_RESTS = 10
    const val EMBERWISE_AT_SCORE = 120f

    /** The full board, achieved flags included — drives the Wardrobe UI. */
    fun board(
        stats: RelationshipStats,
        nowMillis: Long,
    ): List<Unlock> {
        val days = stats.daysTogether(nowMillis)
        val score = Evolution.score(stats, nowMillis)
        return listOf(
            Unlock(PaletteVariant.TRUE_SELF, "always yours", achieved = true),
            Unlock(PaletteVariant.DAWN, "$DAWN_AT_DAYS days together", days >= DAWN_AT_DAYS),
            Unlock(PaletteVariant.AURORA, "$AURORA_AT_DAYS days together", days >= AURORA_AT_DAYS),
            Unlock(
                PaletteVariant.DEEP_SEA,
                "$DEEP_SEA_AT_FACTS remembered facts",
                stats.liveFactCount >= DEEP_SEA_AT_FACTS,
            ),
            Unlock(
                PaletteVariant.MOONLIT,
                "$MOONLIT_AT_RESTS rests together",
                stats.restSessionCount >= MOONLIT_AT_RESTS,
            ),
            Unlock(PaletteVariant.EMBERWISE, "the wise stage", score >= EMBERWISE_AT_SCORE),
        )
    }

    fun isUnlocked(
        variant: PaletteVariant,
        stats: RelationshipStats,
        nowMillis: Long,
    ): Boolean = board(stats, nowMillis).first { it.variant == variant }.achieved

    /** The shift actually applied: a locked selection falls back honestly. */
    fun effectiveShiftDeg(
        selectedWire: String?,
        stats: RelationshipStats,
        nowMillis: Long,
    ): Float {
        val selected = PaletteVariant.fromWire(selectedWire)
        return if (isUnlocked(selected, stats, nowMillis)) selected.shiftDeg else 0f
    }
}
