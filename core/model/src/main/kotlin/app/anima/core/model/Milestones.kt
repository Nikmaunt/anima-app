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
 * - locked variants show their condition honestly (30 days together),
 *   never a countdown.
 *
 * v1.1c defect D4: the `label` was an English string literal in a JVM module
 * with no `res/`, so all six locales showed "True self", "Dawn", "Aurora"…
 * The name of a shade is wording, and wording belongs to the module that owns
 * `res/` — this enum keeps only what is a fact: the wire value and the degrees.
 */
enum class PaletteVariant(
    val wire: String,
    val shiftDeg: Float,
) {
    TRUE_SELF("true_self", 0f),
    DAWN("dawn", 38f),
    AURORA("aurora", 120f),
    DEEP_SEA("deep_sea", -120f),
    MOONLIT("moonlit", 75f),
    EMBERWISE("emberwise", -45f),
    ;

    companion object {
        fun fromWire(wire: String?): PaletteVariant = entries.firstOrNull { it.wire == wire } ?: TRUE_SELF
    }
}

object Milestones {
    /**
     * v1.1c defect D4: `condition` used to be an English sentence built here
     * ("7 days together", "100 remembered facts"). It is now the *shape* of the
     * condition plus its threshold; the screen writes the sentence.
     */
    enum class UnlockKind {
        ALWAYS,
        DAYS_TOGETHER,
        REMEMBERED_FACTS,
        RESTS_TOGETHER,
        WISE_STAGE,
    }

    data class Unlock(
        val variant: PaletteVariant,
        val kind: UnlockKind,
        val threshold: Int,
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
            Unlock(PaletteVariant.TRUE_SELF, UnlockKind.ALWAYS, 0, achieved = true),
            Unlock(
                PaletteVariant.DAWN,
                UnlockKind.DAYS_TOGETHER,
                DAWN_AT_DAYS.toInt(),
                days >= DAWN_AT_DAYS,
            ),
            Unlock(
                PaletteVariant.AURORA,
                UnlockKind.DAYS_TOGETHER,
                AURORA_AT_DAYS.toInt(),
                days >= AURORA_AT_DAYS,
            ),
            Unlock(
                PaletteVariant.DEEP_SEA,
                UnlockKind.REMEMBERED_FACTS,
                DEEP_SEA_AT_FACTS,
                stats.liveFactCount >= DEEP_SEA_AT_FACTS,
            ),
            Unlock(
                PaletteVariant.MOONLIT,
                UnlockKind.RESTS_TOGETHER,
                MOONLIT_AT_RESTS,
                stats.restSessionCount >= MOONLIT_AT_RESTS,
            ),
            Unlock(
                PaletteVariant.EMBERWISE,
                UnlockKind.WISE_STAGE,
                EMBERWISE_AT_SCORE.toInt(),
                score >= EMBERWISE_AT_SCORE,
            ),
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
