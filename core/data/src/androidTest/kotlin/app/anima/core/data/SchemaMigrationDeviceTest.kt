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
 * Migration scaffold (GMD). Today the DB is at version 1, so there is no
 * migration to run — this suite pins the committed schema JSON to reality:
 * a database created from `schemas/1.json` must open under the current
 * entities with no validation error. When version 2 lands, its Migration
 * object gets exercised here (helper.runMigrationsAndValidate) BEFORE any
 * release. Cipher note: migrations are plain SQL; SQLCipher is transparent
 * to them, so the framework factory is the honest choice here.
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
    fun schema_1_database_opens_under_current_entities() =
        runTest {
            helper.createDatabase(DB_NAME, 1).use { db ->
                db.execSQL(
                    "INSERT INTO soul_facts " +
                        "(id, category, text, source, createdAtMillis, supersededById, forgottenAtMillis) " +
                        "VALUES ('fact-1', 'preference', 'from schema 1', 'chat_confirmed', 1, NULL, NULL)",
                )
            }
            val room =
                Room
                    .databaseBuilder(
                        ApplicationProvider.getApplicationContext(),
                        AnimaDatabase::class.java,
                        DB_NAME,
                    ).allowMainThreadQueries()
                    .build()
            // Room validates the on-disk schema against the entities on first
            // access; a drifted 1.json would throw IllegalStateException here.
            val all = room.soulFactDao().allIncludingDead()
            room.close()
            assertThat(all.single().text).isEqualTo("from schema 1")
        }

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}
