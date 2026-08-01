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

    /** v0.4: a completed rest session weighs like a fifth of a day together. */
    const val REST_WEIGHT = 1f / 5f

    fun score(
        stats: RelationshipStats,
        nowMillis: Long,
    ): Float =
        stats.daysTogether(nowMillis).toFloat() +
            stats.liveFactCount * FACT_WEIGHT +
            stats.conversationCount * CONVERSATION_WEIGHT +
            stats.restSessionCount * REST_WEIGHT

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

/**
 * v1.1c defect D4 — what a moment IS, not what it says.
 *
 * `core/model` is a JVM module: it has no `res/`, it cannot have one, and it
 * was carrying twelve user-visible English sentences in Kotlin string literals.
 * They reached the screen untranslated in all six locales — visible in
 * `docs/design/v11/before/15-story.png`. The fix is not to add resources here
 * (impossible) but to stop deciding wording here at all: a moment is a KIND and
 * a number, and the module that owns strings turns it into a sentence.
 */
enum class StoryMomentKind {
    HATCHED,
    FIRST_CHARGE,
    MIND_AWAKENED,
    FIRST_STORM,
    FIRST_MEMORY,
    NTH_MEMORY,
    DAYS_TOGETHER,
    HUNDRED_CONVERSATIONS,
}

/**
 * A moment worth remembering on the "Our story" timeline.
 *
 * @property amount the number the sentence needs — notifications in the storm,
 *   days together, the n-th remembered thing. Null where the kind needs none.
 */
data class StoryMoment(
    val atMillis: Long,
    val kind: StoryMomentKind,
    val amount: Int? = null,
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
        moments += StoryMoment(hatchedAtMillis, StoryMomentKind.HATCHED)

        journal
            .firstOrNull { it.kind == JournalKind.CHARGE_START }
            ?.let { moments += StoryMoment(it.atMillis, StoryMomentKind.FIRST_CHARGE) }
        journal
            .firstOrNull { it.kind == JournalKind.MIND_AWAKENED }
            ?.let { moments += StoryMoment(it.atMillis, StoryMomentKind.MIND_AWAKENED) }
        journal
            .firstOrNull { it.kind == JournalKind.NOTIF_STORM }
            ?.let {
                moments +=
                    StoryMoment(
                        it.atMillis,
                        StoryMomentKind.FIRST_STORM,
                        it.detail?.toIntOrNull(),
                    )
            }

        val liveOrdered = facts.filter { it.isLive }.sortedBy { it.createdAtMillis }
        liveOrdered.firstOrNull()?.let {
            moments += StoryMoment(it.createdAtMillis, StoryMomentKind.FIRST_MEMORY)
        }
        if (liveOrdered.size >= FACTS_MILESTONE) {
            moments +=
                StoryMoment(
                    liveOrdered[FACTS_MILESTONE - 1].createdAtMillis,
                    StoryMomentKind.NTH_MEMORY,
                    FACTS_MILESTONE,
                )
        }

        listOf(DAYS_MILESTONE_30, DAYS_MILESTONE_100).forEach { days ->
            val at = hatchedAtMillis + days * RelationshipStats.DAY_MILLIS
            if (at <= nowMillis) {
                moments += StoryMoment(at, StoryMomentKind.DAYS_TOGETHER, days.toInt())
            }
        }
        if (conversationCount >= CONVERSATIONS_MILESTONE) {
            // No per-message timestamps needed: anchor on "now known reached".
            moments +=
                StoryMoment(nowMillis, StoryMomentKind.HUNDRED_CONVERSATIONS, CONVERSATIONS_MILESTONE)
        }

        return moments.sortedBy { it.atMillis }
    }
}
