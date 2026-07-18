package app.anima.core.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration suite (GMD). v0.5 brings the first REAL migration (1→2, time
 * capsules): [AnimaDatabase.MIGRATION_1_2] is validated against the
 * committed `schemas/2.json` and the upgraded database must serve both old
 * rows and the new table. Cipher note: migrations are plain SQL; SQLCipher
 * is transparent to them, so the framework factory is the honest choice
 * here (the cipher-specific WAL behavior is pinned by WalPoolKeyDeviceTest).
 */
@RunWith(AndroidJUnit4::class)
class SchemaMigrationDeviceTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AnimaDatabase::class.java,
        )

    @Test
    fun migrate_1_to_2_preserves_soul_and_adds_capsules() =
        runTest {
            helper.createDatabase(DB_NAME, 1).use { db ->
                db.execSQL(
                    "INSERT INTO soul_facts " +
                        "(id, category, text, source, createdAtMillis, supersededById, forgottenAtMillis) " +
                        "VALUES ('fact-1', 'preference', 'from schema 1', 'chat_confirmed', 1, NULL, NULL)",
                )
            }
            // Validates the migrated schema byte-for-byte against 2.json.
            helper
                .runMigrationsAndValidate(DB_NAME, 2, true, AnimaDatabase.MIGRATION_1_2)
                .use { db ->
                    db.execSQL(
                        "INSERT INTO time_capsules (id, text, createdAtMillis, deliverAtMillis, openedAtMillis) " +
                            "VALUES ('caps-1', 'hello future me', 1, 2, NULL)",
                    )
                }
            // The full production open path: builder + migration + DAOs.
            val room =
                Room
                    .databaseBuilder(
                        ApplicationProvider.getApplicationContext(),
                        AnimaDatabase::class.java,
                        DB_NAME,
                    ).addMigrations(AnimaDatabase.MIGRATION_1_2)
                    .allowMainThreadQueries()
                    .build()
            val facts = room.soulFactDao().allIncludingDead()
            val capsules = room.timeCapsuleDao().all()
            room.close()
            assertThat(facts.single().text).isEqualTo("from schema 1")
            assertThat(capsules.single().text).isEqualTo("hello future me")
        }

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}
