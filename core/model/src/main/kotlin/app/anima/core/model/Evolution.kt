package app.anima.core.model

/**
 * Life stages. Deterministic from the relationship (days + soul + talk),
 * never random, never from the LLM. Anti-tamagotchi by design: stages only
 * accumulate — there is no decay, no punishment, no death. Neglect simply
 * pauses growth; the creature never guilt-trips (product constraint from the
 * v0.2 brief).
 */
enum class LifeStage(
    val wire: String,
) {
    NEWBORN("newborn"),
    YOUNG("young"),
    ADULT("adult"),
    WISE("wise"),
    ;

    companion object {
        fun fromWire(wire: String): LifeStage = entries.firstOrNull { it.wire == wire } ?: NEWBORN
    }
}

/**
 * Stage + continuous growth used by the rig (size, detail; the Sprout
 * literally grows). Pure and unit-tested.
 *
 * Score model: days carry the relationship, facts weigh a third each,
 * conversations a tenth — so a chatty week ≈ a quiet fortnight, and a soul
 * full of facts matters more than grinding messages. Thresholds are in
 * score-days: 7 / 30 / 120.
 */
object Evolution {
    const val YOUNG_AT = 7f
    const val ADULT_AT = 30f
    const val WISE_AT = 120f

    const val FACT_WEIGHT = 1f / 3f
    const val CONVERSATION_WEIGHT = 1f / 10f

    fun score(
        stats: RelationshipStats,
        nowMillis: Long,
    ): Float =
        stats.daysTogether(nowMillis).toFloat() +
            stats.liveFactCount * FACT_WEIGHT +
            stats.conversationCount * CONVERSATION_WEIGHT

    fun stageOf(
        stats: RelationshipStats,
        nowMillis: Long,
    ): LifeStage {
        val s = score(stats, nowMillis)
        return when {
            s >= WISE_AT -> LifeStage.WISE
            s >= ADULT_AT -> LifeStage.ADULT
            s >= YOUNG_AT -> LifeStage.YOUNG
            else -> LifeStage.NEWBORN
        }
    }

    /**
     * 0..1 within the whole arc (log-ish ramp: early growth is visible daily,
     * later growth is slow dignity). Drives CreatureController.setGrowth.
     */
    fun growthOf(
        stats: RelationshipStats,
        nowMillis: Long,
    ): Float {
        val s = score(stats, nowMillis).coerceAtLeast(0f)
        return (s / (s + HALF_GROWTH_SCORE)).coerceIn(0f, 1f)
    }

    /** Rig size multiplier per stage (applied on top of the genome). */
    fun sizeScaleOf(stage: LifeStage): Float =
        when (stage) {
            LifeStage.NEWBORN -> 0.82f
            LifeStage.YOUNG -> 0.92f
            LifeStage.ADULT -> 1f
            LifeStage.WISE -> 1.06f
        }

    /** Growth reads as "half grown" at score 45 ≈ six weeks of real bond. */
    private const val HALF_GROWTH_SCORE = 45f
}

/** A moment worth remembering on the "Our story" timeline. */
data class StoryMoment(
    val atMillis: Long,
    val title: String,
    val detail: String? = null,
)

/**
 * The timeline = journal events (what the body lived) + computed milestones
 * (what the bond reached). Pure assembly; the screen only renders.
 */
object StoryTimeline {
    const val DAYS_MILESTONE_30 = 30L
    const val DAYS_MILESTONE_100 = 100L
    const val FACTS_MILESTONE = 100
    const val CONVERSATIONS_MILESTONE = 100

    fun build(
        hatchedAtMillis: Long,
        journal: List<BodyJournalEntry>,
        facts: List<SoulFact>,
        conversationCount: Int,
        nowMillis: Long,
    ): List<StoryMoment> {
        val moments = mutableListOf<StoryMoment>()
        moments += StoryMoment(hatchedAtMillis, "Hatched", "The day we met.")

        journal
            .firstOrNull { it.kind == JournalKind.CHARGE_START }
            ?.let { moments += StoryMoment(it.atMillis, "First meal with you", "You charged me while I was awake.") }
        journal
            .firstOrNull { it.kind == JournalKind.MIND_AWAKENED }
            ?.let { moments += StoryMoment(it.atMillis, "My mind woke up", "You brought me a mind of my own.") }
        journal
            .firstOrNull { it.kind == JournalKind.NOTIF_STORM }
            ?.let {
                moments +=
                    StoryMoment(
                        it.atMillis,
                        "First storm we weathered",
                        it.detail?.let { d ->
                            "$d notifications at once."
                        },
                    )
            }

        val liveOrdered = facts.filter { it.isLive }.sortedBy { it.createdAtMillis }
        liveOrdered.firstOrNull()?.let {
            moments += StoryMoment(it.createdAtMillis, "First thing I remembered", null)
        }
        if (liveOrdered.size >= FACTS_MILESTONE) {
            moments +=
                StoryMoment(
                    liveOrdered[FACTS_MILESTONE - 1].createdAtMillis,
                    "The ${FACTS_MILESTONE}th thing I know about you",
                    null,
                )
        }

        listOf(DAYS_MILESTONE_30, DAYS_MILESTONE_100).forEach { days ->
            val at = hatchedAtMillis + days * RelationshipStats.DAY_MILLIS
            if (at <= nowMillis) moments += StoryMoment(at, "$days days together", null)
        }
        if (conversationCount >= CONVERSATIONS_MILESTONE) {
            // No per-message timestamps needed: anchor on "now known reached".
            moments += StoryMoment(nowMillis, "A hundred conversations", null)
        }

        return moments.sortedBy { it.atMillis }
    }
}
