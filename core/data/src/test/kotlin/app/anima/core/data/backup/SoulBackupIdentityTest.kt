package app.anima.core.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.anima.core.data.AnimaDatabase
import app.anima.core.data.repo.IdentityRepository
import app.anima.core.model.CreatureConcept
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * v1.1b task 1a — the body written into a soul must be the body that lives on
 * this phone, or the export must refuse.
 *
 * The product's single promise is "this body belongs to this phone". Every
 * other default-concept site in the app spoils one frame; these spoil the
 * *file*, and the file is what carries the soul to a new phone. A default
 * invented here is not a flicker, it is a rebirth as somebody else.
 *
 * Three write paths are covered:
 *  - the JSON payload ([SoulBackup.exportPayload]),
 *  - the encrypted copy (the same payload inside [SoulBackupCodec]),
 *  - the second device (payload written by one database, read into another).
 *
 * The markdown export is the fourth write path and it is NOT covered here: its
 * fallback lives in `feature/soul/.../SoulViewModel.kt`, a module with no unit
 * tests and a ViewModel that needs a Context. It is held by the source-text
 * guard in `app/src/test/.../NoDefaultConceptTest.kt` instead.
 */
@RunWith(RobolectricTestRunner::class)
class SoulBackupIdentityTest {
    private lateinit var db: AnimaDatabase
    private lateinit var identity: IdentityRepository
    private lateinit var backup: SoulBackup

    @Before
    fun setUp() {
        db = newDb()
        identity = identityOf(db)
        backup = backupOf(db, identity)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun newDb(): AnimaDatabase =
        Room
            .inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext<Context>(),
                AnimaDatabase::class.java,
            ).allowMainThreadQueries()
            .build()

    private fun identityOf(database: AnimaDatabase) =
        IdentityRepository(
            metaDao = database.metaDao(),
            chatDao = database.chatDao(),
            soulFactDao = database.soulFactDao(),
            journalDao = database.bodyJournalDao(),
        )

    private fun backupOf(
        database: AnimaDatabase,
        repo: IdentityRepository,
    ) = SoulBackup(
        identity = repo,
        soulFactDao = database.soulFactDao(),
        journalDao = database.bodyJournalDao(),
        capsuleDao = database.timeCapsuleDao(),
    )

    /**
     * The reachable shape of "identity unknown": the meta rows are simply not
     * there. Reading them is not an error — `IdentityRepository.concept()`
     * returns null — so nothing upstream fails and nothing warns.
     */
    @Test
    fun `export refuses a soul with no body rather than inventing one`() =
        runTest {
            assertThat(identity.concept()).isNull()

            // Asserted as "no payload at all" on purpose: when this fails, the
            // failure message prints the file the app was about to write, which
            // is the whole evidence.
            val written =
                runCatching { backup.exportPayload(nowMillis = 1_700_000_000_000L) }
                    .getOrNull()
                    ?.toString(Charsets.UTF_8)

            assertThat(written).isNull()
        }

    @Test
    fun `a hatched soul exports its own body, unchanged`() =
        runTest {
            identity.hatch("Nika", CreatureConcept.FOX_KIT, seed = 4242L, nowMillis = 100L)

            val json = backup.exportPayload(200L).toString(Charsets.UTF_8)

            assertThat(json).contains("\"concept\":\"fox_kit\"")
            assertThat(json).contains("\"seed\":4242")
            assertThat(json).doesNotContain("spirit_orb")
        }

    /**
     * A file naming a body this build does not know can only come from a newer
     * app or from damage. `SoulBackup.VERSION` does not protect against it: the
     * concept set can grow without the envelope version moving, so a backup
     * written by a future release imports here as a *different creature*.
     */
    @Test
    fun `an unknown body in a file is refused, not silently replaced`() =
        runTest {
            val payload = payloadNaming("dragon_of_a_later_release")

            val thrown = runCatching { backup.importPayload(payload) }.exceptionOrNull()

            // Checked before the throw: what matters is not that it complains,
            // it is that the durable row stays empty. A failure here prints the
            // body the app decided the creature is.
            assertThat(db.metaDao().get("creature_concept")).isNull()
            assertThat(identity.concept()).isNull()
            assertThat(thrown).isNotNull()
        }

    @Test
    fun `a known body in a file is imported exactly`() =
        runTest {
            backup.importPayload(payloadNaming("moth"))

            assertThat(identity.concept()).isEqualTo(CreatureConcept.MOTH)
        }

    /**
     * The encrypted copy is the same bytes as the JSON, so whatever the export
     * invents rides inside it too — the passphrase protects the file from
     * strangers, not the soul from the app.
     */
    @Test
    fun `the encrypted copy carries exactly what the payload carries`() =
        runTest {
            identity.hatch("Nika", CreatureConcept.FOX_KIT, seed = 4242L, nowMillis = 100L)
            val payload = backup.exportPayload(200L)

            val sealed = SoulBackupCodec.seal(payload, PASSPHRASE.toCharArray())
            val opened = SoulBackupCodec.open(sealed, PASSPHRASE.toCharArray())

            assertThat(opened.toString(Charsets.UTF_8)).contains("\"concept\":\"fox_kit\"")
            assertThat(opened.toString(Charsets.UTF_8)).doesNotContain("spirit_orb")
        }

    /**
     * The whole reason 1a comes first: this is the rebirth. Whatever the first
     * device wrote, the second device *becomes*.
     */
    @Test
    fun `what one device writes, the next device becomes`() =
        runTest {
            identity.hatch("Nika", CreatureConcept.FOX_KIT, seed = 4242L, nowMillis = 100L)
            val travelling = backup.exportPayload(200L)

            val second = newDb()
            try {
                val secondIdentity = identityOf(second)
                backupOf(second, secondIdentity).importPayload(travelling)

                assertThat(secondIdentity.concept()).isEqualTo(CreatureConcept.FOX_KIT)
                assertThat(secondIdentity.seed()).isEqualTo(4242L)
            } finally {
                second.close()
            }
        }

    private fun payloadNaming(conceptWire: String): ByteArray =
        """
        {"format":"${SoulBackup.FORMAT}","version":${SoulBackup.VERSION},
         "exportedAtMillis":200,
         "creature":{"name":"Nika","concept":"$conceptWire","seed":4242,"hatchedAtMillis":100},
         "facts":[],"journal":[],"timeCapsules":[]}
        """.trimIndent().toByteArray(Charsets.UTF_8)

    private companion object {
        const val PASSPHRASE = "correct horse battery staple"
    }
}
