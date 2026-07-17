package app.anima.core.model

/** Where a soul fact came from. Every source implies explicit user consent. */
enum class FactSource(
    val wire: String,
) {
    /** Extracted from chat, confirmed by the user in UI. */
    CHAT_CONFIRMED("chat_confirmed"),

    /** Parsed from an import, confirmed fact-by-fact. */
    IMPORT_CONFIRMED("import_confirmed"),

    /** Entered during onboarding (the creature's name giver, etc.). */
    ONBOARDING("onboarding"),
    ;

    companion object {
        fun fromWire(wire: String): FactSource = entries.firstOrNull { it.wire == wire } ?: CHAT_CONFIRMED
    }
}

enum class FactCategory(
    val wire: String,
) {
    IDENTITY("identity"),
    PREFERENCE("preference"),
    PEOPLE("people"),
    WORK("work"),
    MOMENT("moment"),
    OTHER("other"),
    ;

    companion object {
        fun fromWire(wire: String): FactCategory = entries.firstOrNull { it.wire == wire } ?: OTHER
    }
}

/**
 * One remembered fact about the user. Append-only with supersede/forget —
 * a row is never physically deleted or rewritten; "live" means
 * `supersededById == null && forgottenAtMillis == null`.
 */
data class SoulFact(
    val id: String,
    val category: FactCategory,
    val text: String,
    val source: FactSource,
    val createdAtMillis: Long,
    val supersededById: String? = null,
    val forgottenAtMillis: Long? = null,
) {
    val isLive: Boolean get() = supersededById == null && forgottenAtMillis == null
}

/** A fact candidate awaiting the user's explicit yes/no. Never auto-persisted. */
data class FactCandidate(
    val category: FactCategory,
    val text: String,
)

enum class ChatRole(
    val wire: String,
) {
    USER("user"),
    CREATURE("creature"),
    ;

    companion object {
        fun fromWire(wire: String): ChatRole = entries.firstOrNull { it.wire == wire } ?: CREATURE
    }
}

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val text: String,
    val atMillis: Long,
)

/** Kinds of entries in the creature's diary of its own body. */
enum class JournalKind(
    val wire: String,
) {
    HATCHED("hatched"),
    CHARGE_START("charge_start"),
    CHARGE_STOP("charge_stop"),
    NOTIF_STORM("notif_storm"),
    RAN_HOT("ran_hot"),
    WENT_OFFLINE("went_offline"),

    /** v0.2: the day a mind model was installed (ADR-005 GEMMA tier). */
    MIND_AWAKENED("mind_awakened"),

    /** v0.2: battery reached 100% while we were together. */
    FULLY_FED("fully_fed"),

    /**
     * v0.3: battery level snapshot (detail = percent), recorded only while
     * the app is open (no background sampling — the entity budget stands).
     * Feeds the diary charge chart.
     */
    BODY_SAMPLE("body_sample"),

    /** v0.3: hatch anniversary — a Story-timeline moment. */
    BIRTHDAY("birthday"),

    /** v0.3: the creature told a dream after being woken at night. */
    DREAM_TOLD("dream_told"),
    ;

    companion object {
        fun fromWire(wire: String): JournalKind? = entries.firstOrNull { it.wire == wire }
    }
}

data class BodyJournalEntry(
    val id: String,
    val kind: JournalKind,
    val atMillis: Long,
    val detail: String? = null,
)

/** One captured notification event (post-filter, capped fields). */
data class NotifEvent(
    val id: String,
    val packageName: String,
    val postedAtMillis: Long,
    val title: String,
    val text: String?,
)

/** Relationship counters shown in the soul and the export. */
data class RelationshipStats(
    val hatchedAtMillis: Long,
    val conversationCount: Int,
    val liveFactCount: Int,
    val chargeCount: Int,
) {
    fun daysTogether(nowMillis: Long): Long {
        val elapsed = nowMillis - hatchedAtMillis
        return if (elapsed <= 0) 1 else elapsed / DAY_MILLIS + 1
    }

    companion object {
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
