package app.anima.core.data.backup

import app.anima.core.data.dao.BodyJournalDao
import app.anima.core.data.dao.SoulFactDao
import app.anima.core.data.dao.TimeCapsuleDao
import app.anima.core.data.entity.BodyJournalEntity
import app.anima.core.data.entity.SoulFactEntity
import app.anima.core.data.entity.TimeCapsuleEntity
import app.anima.core.data.repo.IdentityRepository
import app.anima.core.model.CreatureConcept
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Soul migration (v0.2): the WHOLE soul — identity, every fact including
 * superseded/forgotten rows (append-only history travels intact), and the
 * body journal — as one JSON payload inside the [SoulBackupCodec] envelope.
 *
 * Import is additive and id-faithful: rows whose ids already exist are
 * skipped silently (re-importing your own backup is a no-op, not a crash).
 */
@Singleton
class SoulBackup
    @Inject
    constructor(
        private val identity: IdentityRepository,
        private val soulFactDao: SoulFactDao,
        private val journalDao: BodyJournalDao,
        private val capsuleDao: TimeCapsuleDao,
    ) {
        /**
         * v1.1b task 1a: the export has no body to write yet. Refusing is the
         * only non-corrupting answer — a soul file is what carries the creature
         * to the next phone, so a body invented here would be a rebirth as
         * somebody else, not a missing label.
         */
        class NoBodyToExport : Exception("the creature has no assigned body yet")

        /**
         * v1.1b task 1a: the file names a body this build does not know.
         * Deliberately carries no detail — the wire value comes from a file and
         * untrusted text does not travel into the interface.
         */
        class UnknownBodyInFile : Exception("the backup names a body this version does not know")

        suspend fun exportPayload(nowMillis: Long): ByteArray {
            // Read identity FIRST and refuse before touching anything else: a
            // half-identified soul must not produce a file at all.
            val concept = identity.concept() ?: throw NoBodyToExport()
            val seed = identity.seed() ?: throw NoBodyToExport()
            val facts = soulFactDao.allIncludingDead()
            val journal = journalDao.recent(JOURNAL_EXPORT_CAP).first()
            val capsules = capsuleDao.all()
            val json =
                buildJsonObject {
                    put("format", FORMAT)
                    put("version", VERSION)
                    put("exportedAtMillis", nowMillis)
                    put(
                        "creature",
                        buildJsonObject {
                            put("name", identity.name() ?: "")
                            put("concept", concept.wire)
                            put("seed", seed)
                            put("hatchedAtMillis", identity.hatchedAtMillis() ?: nowMillis)
                        },
                    )
                    put(
                        "facts",
                        buildJsonArray {
                            facts.forEach { fact ->
                                add(
                                    buildJsonObject {
                                        put("id", fact.id)
                                        put("category", fact.category)
                                        put("text", fact.text)
                                        put("source", fact.source)
                                        put("createdAtMillis", fact.createdAtMillis)
                                        fact.supersededById?.let { put("supersededById", it) }
                                        fact.forgottenAtMillis?.let { put("forgottenAtMillis", it) }
                                    },
                                )
                            }
                        },
                    )
                    put(
                        "journal",
                        buildJsonArray {
                            journal.forEach { entry ->
                                add(
                                    buildJsonObject {
                                        put("id", entry.id)
                                        put("kind", entry.kind)
                                        put("atMillis", entry.atMillis)
                                        entry.detail?.let { put("detail", it) }
                                    },
                                )
                            }
                        },
                    )
                    // v0.5 (backup version 2): the letters the creature holds
                    // are part of the soul — they travel with it.
                    put(
                        "timeCapsules",
                        buildJsonArray {
                            capsules.forEach { capsule ->
                                add(
                                    buildJsonObject {
                                        put("id", capsule.id)
                                        put("text", capsule.text)
                                        put("createdAtMillis", capsule.createdAtMillis)
                                        put("deliverAtMillis", capsule.deliverAtMillis)
                                        capsule.openedAtMillis?.let { put("openedAtMillis", it) }
                                    },
                                )
                            }
                        },
                    )
                }
            return json.toString().toByteArray(Charsets.UTF_8)
        }

        data class ImportSummary(
            val creatureName: String,
            val factsImported: Int,
            val factsSkipped: Int,
            val journalImported: Int,
        )

        suspend fun importPayload(payload: ByteArray): ImportSummary {
            val root = Json.parseToJsonElement(payload.toString(Charsets.UTF_8)).jsonObject
            require(root["format"]?.jsonPrimitive?.content == FORMAT) { "not a soul backup" }
            require((root["version"]?.jsonPrimitive?.int ?: 0) <= VERSION) { "backup from a newer app" }

            val creature = root.getValue("creature").jsonObject
            val name = creature.getValue("name").jsonPrimitive.content
            // Unknown body: refuse before the first write. Everything below this
            // line touches durable data, so the check has to be above it.
            val concept =
                CreatureConcept.fromWire(creature.getValue("concept").jsonPrimitive.content)
                    ?: throw UnknownBodyInFile()
            val seed = creature.getValue("seed").jsonPrimitive.long
            val hatchedAt = creature.getValue("hatchedAtMillis").jsonPrimitive.long
            identity.hatch(name, concept, seed, hatchedAt)
            identity.restoreHatchedAt(hatchedAt)

            var imported = 0
            var skipped = 0
            root.getValue("facts").jsonArray.forEach { element ->
                val obj = element.jsonObject
                val entity =
                    SoulFactEntity(
                        id = obj.getValue("id").jsonPrimitive.content,
                        category = obj.getValue("category").jsonPrimitive.content,
                        text = obj.getValue("text").jsonPrimitive.content,
                        source = obj.getValue("source").jsonPrimitive.content,
                        createdAtMillis = obj.getValue("createdAtMillis").jsonPrimitive.long,
                        supersededById = obj["supersededById"]?.jsonPrimitive?.content,
                        forgottenAtMillis = obj["forgottenAtMillis"]?.jsonPrimitive?.longOrNull,
                    )
                runCatching { soulFactDao.insert(entity) }
                    .onSuccess { imported++ }
                    .onFailure { skipped++ }
            }

            var journalImported = 0
            root.getValue("journal").jsonArray.forEach { element ->
                val obj = element.jsonObject
                val entity =
                    BodyJournalEntity(
                        id = obj.getValue("id").jsonPrimitive.content,
                        kind = obj.getValue("kind").jsonPrimitive.content,
                        atMillis = obj.getValue("atMillis").jsonPrimitive.long,
                        detail = obj["detail"]?.jsonPrimitive?.content,
                    )
                runCatching { journalDao.insert(entity) }.onSuccess { journalImported++ }
            }

            // Version-1 files simply have no capsules — tolerant absence.
            root["timeCapsules"]?.jsonArray?.forEach { element ->
                val obj = element.jsonObject
                val entity =
                    TimeCapsuleEntity(
                        id = obj.getValue("id").jsonPrimitive.content,
                        text = obj.getValue("text").jsonPrimitive.content,
                        createdAtMillis = obj.getValue("createdAtMillis").jsonPrimitive.long,
                        deliverAtMillis = obj.getValue("deliverAtMillis").jsonPrimitive.long,
                        openedAtMillis = obj["openedAtMillis"]?.jsonPrimitive?.longOrNull,
                    )
                runCatching { capsuleDao.insert(entity) }
            }

            return ImportSummary(
                creatureName = name,
                factsImported = imported,
                factsSkipped = skipped,
                journalImported = journalImported,
            )
        }

        companion object {
            const val FORMAT = "anima-soul"

            /** 2 = +timeCapsules (v0.5). Older readers reject newer files. */
            const val VERSION = 2
            const val JOURNAL_EXPORT_CAP = 10_000
        }
    }
