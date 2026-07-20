package app.anima.core.data.repo

import app.anima.core.data.dao.BodyJournalDao
import app.anima.core.data.dao.ChatDao
import app.anima.core.data.dao.MetaDao
import app.anima.core.data.dao.NotifEventDao
import app.anima.core.data.dao.SoulFactDao
import app.anima.core.data.dao.TimeCapsuleDao
import app.anima.core.data.entity.BodyJournalEntity
import app.anima.core.data.entity.ChatMessageEntity
import app.anima.core.data.entity.MetaEntity
import app.anima.core.data.entity.NotifEventEntity
import app.anima.core.data.entity.SoulFactEntity
import app.anima.core.data.entity.TimeCapsuleEntity
import app.anima.core.model.BodyJournalEntry
import app.anima.core.model.ChatMessage
import app.anima.core.model.ChatRole
import app.anima.core.model.CreatureConcept
import app.anima.core.model.FactCategory
import app.anima.core.model.FactSource
import app.anima.core.model.Ids
import app.anima.core.model.JournalKind
import app.anima.core.model.RelationshipStats
import app.anima.core.model.SoulFact
import app.anima.core.model.TimeCapsule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoulRepository
    @Inject
    constructor(
        private val dao: SoulFactDao,
    ) {
        fun liveFacts(): Flow<List<SoulFact>> = dao.live().map { list -> list.map { it.toModel() } }

        fun liveCount(): Flow<Int> = dao.liveCount()

        suspend fun remember(
            category: FactCategory,
            text: String,
            source: FactSource,
            nowMillis: Long,
        ): SoulFact {
            val fact = SoulFact(Ids.new("fact"), category, text.trim(), source, nowMillis)
            dao.insert(fact.toEntity())
            return fact
        }

        suspend fun replace(
            oldId: String,
            category: FactCategory,
            text: String,
            source: FactSource,
            nowMillis: Long,
        ) {
            val replacement = SoulFact(Ids.new("fact"), category, text.trim(), source, nowMillis)
            dao.supersede(oldId, replacement.toEntity())
        }

        suspend fun forget(
            id: String,
            nowMillis: Long,
        ) {
            dao.forget(id, nowMillis)
        }

        suspend fun everything(): List<SoulFact> = dao.allIncludingDead().map { it.toModel() }

        private fun SoulFactEntity.toModel() =
            SoulFact(
                id = id,
                category = FactCategory.fromWire(category),
                text = text,
                source = FactSource.fromWire(source),
                createdAtMillis = createdAtMillis,
                supersededById = supersededById,
                forgottenAtMillis = forgottenAtMillis,
            )

        private fun SoulFact.toEntity() =
            SoulFactEntity(
                id = id,
                category = category.wire,
                text = text,
                source = source.wire,
                createdAtMillis = createdAtMillis,
                supersededById = supersededById,
                forgottenAtMillis = forgottenAtMillis,
            )
    }

@Singleton
class ChatRepository
    @Inject
    constructor(
        private val dao: ChatDao,
    ) {
        fun recent(limit: Int = 50): Flow<List<ChatMessage>> =
            dao.recent(limit).map { list ->
                list.map { ChatMessage(it.id, ChatRole.fromWire(it.role), it.text, it.atMillis) }
            }

        fun userMessageCount(): Flow<Int> = dao.userMessageCount()

        suspend fun append(
            role: ChatRole,
            text: String,
            atMillis: Long,
        ): ChatMessage {
            val message = ChatMessage(Ids.new("msg"), role, text, atMillis)
            dao.insert(ChatMessageEntity(message.id, role.wire, text, atMillis))
            return message
        }

        /** v0.6 GenAI policy: a reported reply is removed, not archived. */
        suspend fun remove(id: String) = dao.delete(id)
    }

@Singleton
class JournalRepository
    @Inject
    constructor(
        private val dao: BodyJournalDao,
    ) {
        fun recent(limit: Int = 40): Flow<List<BodyJournalEntry>> =
            dao.recent(limit).map { list ->
                list.mapNotNull { entity ->
                    JournalKind.fromWire(entity.kind)?.let {
                        BodyJournalEntry(entity.id, it, entity.atMillis, entity.detail)
                    }
                }
            }

        suspend fun record(
            kind: JournalKind,
            atMillis: Long,
            detail: String? = null,
        ) {
            dao.insert(BodyJournalEntity(Ids.new("jrnl"), kind.wire, atMillis, detail))
        }

        suspend fun countOf(kind: JournalKind): Int = dao.countOfKind(kind.wire)

        suspend fun countOfSince(
            kind: JournalKind,
            sinceMillis: Long,
        ): Int = dao.countOfKindSince(kind.wire, sinceMillis)

        /** v0.3 diary chart: one kind's entries since a moment, oldest first. */
        suspend fun ofKindSince(
            kind: JournalKind,
            sinceMillis: Long,
        ): List<BodyJournalEntry> =
            dao.ofKindSince(kind.wire, sinceMillis).map {
                BodyJournalEntry(it.id, kind, it.atMillis, it.detail)
            }

        /** v0.4 "our year": local epoch-days with any interaction at all. */
        suspend fun activeDaysSince(
            sinceMillis: Long,
            zoneOffsetMillis: Long,
        ): Set<Long> = dao.activeDaysSince(sinceMillis, zoneOffsetMillis).toSet()
    }

@Singleton
class NotifEventsRepository
    @Inject
    constructor(
        private val dao: NotifEventDao,
    ) {
        /** Storm signal for the mood engine. */
        fun countInWindow(nowMillis: Long): Flow<Int> = dao.countSince(nowMillis - STORM_WINDOW_MILLIS)

        suspend fun eventsToday(startOfDayMillis: Long): List<app.anima.core.model.NotifEvent> =
            dao.since(startOfDayMillis).map {
                app.anima.core.model
                    .NotifEvent(it.id, it.packageName, it.postedAtMillis, it.title, it.text)
            }

        suspend fun perAppToday(startOfDayMillis: Long): Map<String, Int> =
            dao.perAppSince(startOfDayMillis).associate { it.packageName to it.count }

        /** v2 weekly trend (bounded by the 7-day retention horizon anyway). */
        suspend fun perAppSince(sinceMillis: Long): Map<String, Int> =
            dao.perAppSince(sinceMillis).associate { it.packageName to it.count }

        suspend fun insert(
            id: String,
            packageName: String,
            postedAtMillis: Long,
            title: String,
            text: String?,
        ) {
            dao.insert(NotifEventEntity(id, packageName, postedAtMillis, title, text))
        }

        /** Privacy horizon: nothing older than 7 days is kept. */
        suspend fun prune(nowMillis: Long) {
            dao.pruneBefore(nowMillis - RETENTION_MILLIS)
        }

        companion object {
            const val STORM_WINDOW_MILLIS = 15L * 60 * 1000
            const val RETENTION_MILLIS = 7L * 24 * 60 * 60 * 1000
        }
    }

/** Creature identity + relationship counters, stored as meta rows. */
@Singleton
class IdentityRepository
    @Inject
    constructor(
        private val metaDao: MetaDao,
        private val chatDao: ChatDao,
        private val soulFactDao: SoulFactDao,
        private val journalDao: BodyJournalDao,
    ) {
        fun observeName(): Flow<String?> = metaDao.observe(KEY_NAME)

        fun observeConcept(): Flow<CreatureConcept?> =
            metaDao.observe(KEY_CONCEPT).map { it?.let(CreatureConcept::fromWire) }

        suspend fun name(): String? = metaDao.get(KEY_NAME)

        suspend fun concept(): CreatureConcept? = metaDao.get(KEY_CONCEPT)?.let(CreatureConcept::fromWire)

        suspend fun seed(): Long? = metaDao.get(KEY_SEED)?.toLongOrNull()

        suspend fun hatchedAtMillis(): Long? = metaDao.get(KEY_HATCHED_AT)?.toLongOrNull()

        suspend fun hatch(
            name: String,
            concept: CreatureConcept,
            seed: Long,
            nowMillis: Long,
        ) {
            metaDao.put(MetaEntity(KEY_NAME, name.trim()))
            metaDao.put(MetaEntity(KEY_CONCEPT, concept.wire))
            metaDao.put(MetaEntity(KEY_SEED, seed.toString()))
            if (metaDao.get(KEY_HATCHED_AT) == null) {
                metaDao.put(MetaEntity(KEY_HATCHED_AT, nowMillis.toString()))
            }
        }

        suspend fun rename(name: String) {
            metaDao.put(MetaEntity(KEY_NAME, name.trim()))
        }

        /**
         * Soul migration only: the imported soul keeps its ORIGINAL hatch
         * date — the relationship's age travels with it (hatch() deliberately
         * refuses to overwrite an existing date; this is the sanctioned path).
         */
        suspend fun restoreHatchedAt(hatchedAtMillis: Long) {
            metaDao.put(MetaEntity(KEY_HATCHED_AT, hatchedAtMillis.toString()))
        }

        suspend fun switchConcept(concept: CreatureConcept) {
            metaDao.put(MetaEntity(KEY_CONCEPT, concept.wire))
        }

        suspend fun stats(nowMillis: Long): RelationshipStats {
            val hatched = hatchedAtMillis() ?: nowMillis
            val conversations = chatDao.userMessageCount().first()
            val facts = soulFactDao.liveCount().first()
            val charges = journalDao.countOfKind(JournalKind.CHARGE_START.wire)
            val rests = journalDao.countOfKind(JournalKind.REST_SESSION.wire)
            return RelationshipStats(hatched, conversations, facts, charges, rests)
        }

        private companion object {
            const val KEY_NAME = "creature_name"
            const val KEY_CONCEPT = "creature_concept"
            const val KEY_SEED = "creature_seed"
            const val KEY_HATCHED_AT = "hatched_at"
        }
    }

@Singleton
class TimeCapsuleRepository
    @Inject
    constructor(
        private val dao: TimeCapsuleDao,
    ) {
        suspend fun write(
            text: String,
            deliverAtMillis: Long,
            nowMillis: Long,
        ): TimeCapsule {
            val capsule =
                TimeCapsule(
                    id = Ids.new("caps"),
                    text = text.take(TimeCapsule.MAX_TEXT_CHARS),
                    createdAtMillis = nowMillis,
                    deliverAtMillis = deliverAtMillis,
                    openedAtMillis = null,
                )
            dao.insert(capsule.toEntity())
            return capsule
        }

        /** Capsules the creature is ready to hand over right now. */
        suspend fun due(nowMillis: Long): List<TimeCapsule> = dao.due(nowMillis).map { it.toModel() }

        /** How many letters the creature is still holding (not yet due). */
        fun heldCount(nowMillis: Long): Flow<Int> = dao.heldCount(nowMillis)

        suspend fun markOpened(
            id: String,
            nowMillis: Long,
        ) = dao.markOpened(id, nowMillis)

        suspend fun all(): List<TimeCapsule> = dao.all().map { it.toModel() }

        private fun TimeCapsuleEntity.toModel() =
            TimeCapsule(id, text, createdAtMillis, deliverAtMillis, openedAtMillis)

        private fun TimeCapsule.toEntity() =
            TimeCapsuleEntity(id, text, createdAtMillis, deliverAtMillis, openedAtMillis)
    }
