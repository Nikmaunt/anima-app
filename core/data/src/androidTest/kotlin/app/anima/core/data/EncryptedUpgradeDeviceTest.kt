package app.anima.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * v0.6 Phase 0.2: the upgrade path a real v0.5 install takes, end to end,
 * on a CIPHERED database. [SchemaMigrationDeviceTest] validates MIGRATION_1_2
 * against 2.json on a plain database; this test is the complement it can't
 * be: a v1 file created by SQLCipher itself, filled with a lived-in soul
 * (facts incl. superseded/forgotten chains, a conversation, a body diary
 * with rests, notification history, meta), then opened through the EXACT
 * production builder shape from DataModule — SupportOpenHelperFactory over
 * the same passphrase + addMigrations(MIGRATION_1_2). Every row must
 * survive, the capsule table must be writable, and the file on disk must
 * remain ciphertext.
 */
@RunWith(AndroidJUnit4::class)
class EncryptedUpgradeDeviceTest {
    private lateinit var context: Context
    private lateinit var dbFile: File
    private val passphrase = "upgrade-test-passphrase-32-bytes".toByteArray()

    @Before
    fun setUp() {
        System.loadLibrary("sqlcipher")
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DB)
        dbFile = context.getDatabasePath(DB)
        dbFile.parentFile?.mkdirs()
        createLivedInV1Database()
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DB)
    }

    /**
     * Recreates what a v0.5 device actually has on disk: schema 1 DDL
     * verbatim from `schemas/1.json` (incl. room_master_table carrying
     * v1's identity hash) written by SQLCipher with the app's passphrase.
     */
    private fun createLivedInV1Database() {
        SQLiteDatabase
            .openOrCreateDatabase(dbFile, passphrase.copyOf(), null, null)
            .use { db ->
                // --- schema 1, verbatim from schemas/1.json ---
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `soul_facts` (`id` TEXT NOT NULL, " +
                        "`category` TEXT NOT NULL, `text` TEXT NOT NULL, `source` TEXT NOT NULL, " +
                        "`createdAtMillis` INTEGER NOT NULL, `supersededById` TEXT, " +
                        "`forgottenAtMillis` INTEGER, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `chat_messages` (`id` TEXT NOT NULL, " +
                        "`role` TEXT NOT NULL, `text` TEXT NOT NULL, `atMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_chat_messages_atMillis` " +
                        "ON `chat_messages` (`atMillis`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `body_journal` (`id` TEXT NOT NULL, " +
                        "`kind` TEXT NOT NULL, `atMillis` INTEGER NOT NULL, `detail` TEXT, " +
                        "PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_body_journal_atMillis` " +
                        "ON `body_journal` (`atMillis`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notif_events` (`id` TEXT NOT NULL, " +
                        "`packageName` TEXT NOT NULL, `postedAtMillis` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `text` TEXT, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_notif_events_postedAtMillis` " +
                        "ON `notif_events` (`postedAtMillis`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_notif_events_packageName` " +
                        "ON `notif_events` (`packageName`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `meta` (`key` TEXT NOT NULL, " +
                        "`value` TEXT NOT NULL, PRIMARY KEY(`key`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS room_master_table " +
                        "(id INTEGER PRIMARY KEY,identity_hash TEXT)",
                )
                db.execSQL(
                    "INSERT OR REPLACE INTO room_master_table (id,identity_hash) " +
                        "VALUES(42, '$V1_IDENTITY_HASH')",
                )
                db.version = 1

                // --- a lived-in soul: wire values match core:model enums ---
                // Facts: onboarding identity, a superseded preference chain,
                // a forgotten moment, an import-confirmed person.
                fact(db, "fact-name", "identity", "Their name is Nika", "onboarding", 1_000, null, null)
                fact(
                    db,
                    "fact-tea-old",
                    "preference",
                    "Drinks black tea",
                    "chat_confirmed",
                    2_000,
                    "fact-tea-new",
                    null,
                )
                fact(db, "fact-tea-new", "preference", "Switched to green tea", "chat_confirmed", 9_000, null, null)
                fact(db, "fact-gone", "moment", "A moment asked to be forgotten", "chat_confirmed", 3_000, null, 8_000)
                fact(db, "fact-sis", "people", "Sister is called Ola", "import_confirmed", 4_000, null, null)
                // A short conversation, both roles, multilingual text.
                msg(db, "m1", "user", "привет, как ты?", 10_000)
                msg(db, "m2", "creature", "мурлычу. батарейка тёплая ☺", 11_000)
                msg(db, "m3", "user", "завтра сложный день", 12_000)
                msg(db, "m4", "creature", "я рядом. отдохнём вместе?", 13_000)
                // Body diary: hatched, charge edges, two rests, mind woke up.
                journal(db, "j1", "hatched", 500, null)
                journal(db, "j2", "charge_start", 20_000, null)
                journal(db, "j3", "charge_stop", 30_000, null)
                journal(db, "j4", "rest_session", 40_000, "300")
                journal(db, "j5", "rest_session", 50_000, "600")
                journal(db, "j6", "mind_awakened", 60_000, "gemma")
                // Notification pressure history + meta the creature relies on.
                db.execSQL(
                    "INSERT INTO notif_events (id, packageName, postedAtMillis, title, text) " +
                        "VALUES ('n1', 'org.example.chatapp', 70000, 'ping', 'storm member')",
                )
                db.execSQL("INSERT INTO meta (`key`, value) VALUES ('creature_concept', 'sprout')")
                db.execSQL("INSERT INTO meta (`key`, value) VALUES ('creature_name', 'Iskra')")
            }
    }

    private fun fact(
        db: SQLiteDatabase,
        id: String,
        category: String,
        text: String,
        source: String,
        at: Long,
        supersededBy: String?,
        forgottenAt: Long?,
    ) = db.execSQL(
        "INSERT INTO soul_facts (id, category, text, source, createdAtMillis, supersededById, forgottenAtMillis) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
        arrayOf(id, category, text, source, at, supersededBy, forgottenAt),
    )

    private fun msg(
        db: SQLiteDatabase,
        id: String,
        role: String,
        text: String,
        at: Long,
    ) = db.execSQL(
        "INSERT INTO chat_messages (id, role, text, atMillis) VALUES (?, ?, ?, ?)",
        arrayOf(id, role, text, at),
    )

    private fun journal(
        db: SQLiteDatabase,
        id: String,
        kind: String,
        at: Long,
        detail: String?,
    ) = db.execSQL(
        "INSERT INTO body_journal (id, kind, atMillis, detail) VALUES (?, ?, ?, ?)",
        arrayOf(id, kind, at, detail),
    )

    @Test
    fun v1_encrypted_soul_survives_upgrade_to_v2_intact() {
        // Production open path, byte for byte the DataModule shape.
        val room =
            Room
                .databaseBuilder(context, AnimaDatabase::class.java, DB)
                .openHelperFactory(SupportOpenHelperFactory(passphrase.copyOf()))
                .addMigrations(AnimaDatabase.MIGRATION_1_2)
                .build()
        try {
            // Migration ran and Room accepted the schema (identity check
            // happens on first open — reaching a query at all proves it).
            assertThat(room.openHelper.readableDatabase.version).isEqualTo(2)

            val facts = runBlocking { room.soulFactDao().allIncludingDead() }
            assertThat(facts).hasSize(5)
            assertThat(facts.first { it.id == "fact-tea-old" }.supersededById)
                .isEqualTo("fact-tea-new")
            assertThat(facts.first { it.id == "fact-gone" }.forgottenAtMillis)
                .isEqualTo(8_000)

            val liveFacts = runBlocking { room.soulFactDao().live().first() }
            assertThat(liveFacts.map { it.id })
                .containsExactly("fact-name", "fact-tea-new", "fact-sis")

            val chat = runBlocking { room.chatDao().recent(10).first() }
            assertThat(chat).hasSize(4)
            assertThat(chat.map { it.role }.toSet()).containsExactly("user", "creature")
            assertThat(chat.first { it.id == "m2" }.text).contains("батарейка")

            runBlocking {
                assertThat(room.bodyJournalDao().countOfKind("rest_session")).isEqualTo(2)
                assertThat(room.bodyJournalDao().countOfKind("hatched")).isEqualTo(1)
                assertThat(room.metaDao().get("creature_name")).isEqualTo("Iskra")
                assertThat(room.notifEventDao().since(0L)).hasSize(1)
            }

            // The new v2 surface is genuinely usable on the upgraded file.
            runBlocking {
                room.timeCapsuleDao().insert(
                    app.anima.core.data.entity.TimeCapsuleEntity(
                        id = "caps-1",
                        text = "письмо себе в будущее",
                        createdAtMillis = 90_000,
                        deliverAtMillis = 100_000,
                        openedAtMillis = null,
                    ),
                )
                assertThat(
                    room
                        .timeCapsuleDao()
                        .due(100_001)
                        .single()
                        .text,
                ).isEqualTo("письмо себе в будущее")
            }

            // SQLite's own verdict on the upgraded, ciphered file.
            room.openHelper.readableDatabase.query("PRAGMA integrity_check").use { c ->
                assertThat(c.moveToFirst()).isTrue()
                assertThat(c.getString(0)).isEqualTo("ok")
            }
        } finally {
            room.close()
        }

        // The upgrade must not have decrypted the file: a plain SQLite file
        // starts with the magic "SQLite format 3 "; ciphertext must not.
        val header = ByteArray(16)
        dbFile.inputStream().use { it.read(header) }
        assertThat(String(header, Charsets.US_ASCII)).isNotEqualTo("SQLite format 3 ")
    }

    private companion object {
        const val DB = "encrypted-upgrade-test.db"

        /** identityHash of schemas/1.json — what a real v0.5 install carries. */
        const val V1_IDENTITY_HASH = "8290384a9ae04d4fe8f1ffe57fe9659f"
    }
}
