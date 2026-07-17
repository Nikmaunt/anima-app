package app.anima.core.data.backup

import app.anima.core.data.dao.BodyJournalDao
import app.anima.core.data.dao.SoulFactDao
import app.anima.core.data.entity.BodyJournalEntity
import app.anima.core.data.entity.SoulFactEntity
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
    ) {
        suspend fun exportPayload(nowMillis: Long): ByteArray {
            val facts = soulFactDao.allIncludingDead()
            val journal = journalDao.recent(JOURNAL_EXPORT_CAP).first()
            val json =
                buildJsonObject {
                    put("format", FORMAT)
                    put("version", VERSION)
                    put("exportedAtMillis", nowMillis)
                    put(
                        "creature",
                        buildJsonObject {
                            put("name", identity.name() ?: "")
                            put("concept", (identity.concept() ?: CreatureConcept.SPIRIT_ORB).wire)
                            put("seed", identity.seed() ?: 0L)
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
            val concept = CreatureConcept.fromWire(creature.getValue("concept").jsonPrimitive.content)
            val seed = creature.getValue("seed").jsonPrimitive.long
            val hatchedAt = creature.getValue("hatchedAtMillis").jsonPrimitive.long
            identity.hatch(name, concept ?: CreatureConcept.SPIRIT_ORB, seed, hatchedAt)
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

            return ImportSummary(
                creatureName = name,
                factsImported = imported,
                factsSkipped = skipped,
                journalImported = journalImported,
            )
        }

        companion object {
            const val FORMAT = "anima-soul"
            const val VERSION = 1
            const val JOURNAL_EXPORT_CAP = 10_000
        }
    }
