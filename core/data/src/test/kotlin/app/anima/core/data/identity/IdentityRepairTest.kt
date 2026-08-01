package app.anima.core.data.identity

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.anima.core.data.AnimaDatabase
import app.anima.core.data.entity.MetaEntity
import app.anima.core.data.prefs.AnimaPrefs
import app.anima.core.data.repo.IdentityRepository
import app.anima.core.data.repo.JournalRepository
import app.anima.core.data.repo.SoulRepository
import app.anima.core.model.CreatureConcept
import app.anima.core.model.FactCategory
import app.anima.core.model.FactSource
import app.anima.core.model.JournalKind
import app.anima.core.model.assignedTo
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * v1.1c task 3.4 — the real scenario, on a soul that has been lived in.
 *
 * Every test here plants milestones, memories and a diary before the repair and
 * checks them again afterwards, because the failure mode this code could have
 * is not "the body stays wrong" — it is "the body is fixed and the seven months
 * are gone".
 */
@RunWith(RobolectricTestRunner::class)
class IdentityRepairTest {
    private lateinit var db: AnimaDatabase
    private lateinit var identity: IdentityRepository
    private lateinit var journal: JournalRepository
    private lateinit var soul: SoulRepository
    private lateinit var repair: IdentityRepair
    private lateinit var context: Context

    /** A seed whose assigned body is FOX_KIT, found rather than assumed. */
    private val foxSeed: Long =
        generateSequence(1L) { it + 1 }.first { CreatureConcept.assignedTo(it) == CreatureConcept.FOX_KIT }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db =
            Room
                .inMemoryDatabaseBuilder(context, AnimaDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        identity =
            IdentityRepository(
                metaDao = db.metaDao(),
                chatDao = db.chatDao(),
                soulFactDao = db.soulFactDao(),
                journalDao = db.bodyJournalDao(),
            )
        journal = JournalRepository(db.bodyJournalDao())
        soul = SoulRepository(db.soulFactDao())
        repair = IdentityRepair(context, identity, DeviceSeed(context, AnimaPrefs(context)))
        File(context.filesDir, "identity-backup").deleteRecursively()
        File(context.filesDir, "crash").deleteRecursively()
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** Milestones, memories, a diary and a hatch date, all of them irreplaceable. */
    private suspend fun liveALife() {
        journal.record(JournalKind.HATCHED, HATCHED_AT, "hatched as spirit_orb")
        journal.record(JournalKind.CHARGE_START, HATCHED_AT + DAY, null)
        journal.record(JournalKind.REST_SESSION, HATCHED_AT + 2 * DAY, "12")
        soul.remember(FactCategory.PREFERENCE, "prefers tea to coffee", FactSource.CHAT_CONFIRMED, HATCHED_AT + DAY)
        soul.remember(
            FactCategory.MOMENT,
            "walks by the river on Sundays",
            FactSource.CHAT_CONFIRMED,
            HATCHED_AT + 3 * DAY,
        )
    }

    private suspend fun assertLifeIntact() {
        assertWithMessage("the diary must survive a repair of the body")
            .that(db.bodyJournalDao().countOfKind(JournalKind.HATCHED.wire))
            .isEqualTo(1)
        assertThat(db.bodyJournalDao().countOfKind(JournalKind.CHARGE_START.wire)).isEqualTo(1)
        assertThat(db.bodyJournalDao().countOfKind(JournalKind.REST_SESSION.wire)).isEqualTo(1)
        assertWithMessage("remembered facts are not derivable and must never be touched")
            .that(soul.everything().map { it.text })
            .containsExactly("prefers tea to coffee", "walks by the river on Sundays")
        assertWithMessage("the age of the relationship is the one thing that cannot be recomputed")
            .that(identity.hatchedAtMillis())
            .isEqualTo(HATCHED_AT)
        assertThat(identity.name()).isEqualTo("Iskra")
    }

    /**
     * The scenario in the task, verbatim: a soul carrying `spirit_orb` on a seed
     * that says `fox_kit`, hatched by a build that derives the body.
     */
    @Test
    fun `a wrong body on an assigned soul becomes the seed's body, and the life survives`() =
        runTest {
            identity.hatch("Iskra", CreatureConcept.SPIRIT_ORB, foxSeed, HATCHED_AT)
            identity.markIdentityAssigned()
            liveALife()

            val outcome = repair.run()

            assertThat(outcome).isInstanceOf(IdentityRepair.Outcome.Repaired::class.java)
            val repaired = outcome as IdentityRepair.Outcome.Repaired
            assertThat(repaired.findings).hasSize(1)
            assertThat(repaired.findings.single()).contains("spirit_orb")
            assertThat(repaired.findings.single()).contains("fox_kit")

            assertThat(identity.concept()).isEqualTo(CreatureConcept.FOX_KIT)
            assertThat(identity.seed()).isEqualTo(foxSeed)
            assertLifeIntact()
        }

    /** 3.2 — the backup exists, at a path that can be read out to the owner. */
    @Test
    fun `the state before the repair is written down first`() =
        runTest {
            identity.hatch("Iskra", CreatureConcept.SPIRIT_ORB, foxSeed, HATCHED_AT)
            identity.markIdentityAssigned()
            liveALife()

            val repaired = repair.run() as IdentityRepair.Outcome.Repaired
            val backup = File(repaired.backupPath)

            assertWithMessage("a path we cannot show the owner is not a backup")
                .that(backup.exists())
                .isTrue()
            assertThat(backup.parentFile?.name).isEqualTo("identity-backup")
            val text = backup.readText()
            assertWithMessage("the backup must carry the body as it was, or it restores nothing")
                .that(text)
                .contains("spirit_orb")
            assertThat(text).contains(foxSeed.toString())
            assertThat(text).contains("Iskra")
            assertThat(text).contains(HATCHED_AT.toString())
        }

    /** 3.3 — a local note, on the device, in a file of its own. */
    @Test
    fun `the repair says on the device what it did`() =
        runTest {
            identity.hatch("Iskra", CreatureConcept.SPIRIT_ORB, foxSeed, HATCHED_AT)
            identity.markIdentityAssigned()

            repair.run()

            val log = File(File(context.filesDir, "crash"), "identity-repair.txt")
            assertWithMessage("a repair nobody can find out about is indistinguishable from a bug")
                .that(log.exists())
                .isTrue()
            val text = log.readText()
            assertThat(text).contains("identity repair")
            assertThat(text).contains("fox_kit")
            assertWithMessage("it must not be the crash file, which AnimaApp overwrites wholesale")
                .that(log.name)
                .isNotEqualTo("last-crash.txt")
        }

    /**
     * The decision that separates a repair from a deletion. A soul hatched by a
     * build that offered a gallery of eight bodies made a CHOICE, and roughly
     * seven in eight of those choices differ from what the seed would say.
     */
    @Test
    fun `a body the owner chose is reported and left exactly as it is`() =
        runTest {
            identity.hatch("Iskra", CreatureConcept.SPIRIT_ORB, foxSeed, HATCHED_AT)
            // No origin row at all: every soul older than v1.1c looks like this.
            liveALife()

            val outcome = repair.run()

            assertThat(outcome).isInstanceOf(IdentityRepair.Outcome.LeftAlone::class.java)
            assertWithMessage("the owner's chosen body must survive the migration untouched")
                .that(identity.concept())
                .isEqualTo(CreatureConcept.SPIRIT_ORB)
            assertThat((outcome as IdentityRepair.Outcome.LeftAlone).reason).contains("chosen")
            assertLifeIntact()
            assertWithMessage("nothing was rewritten, so nothing needed backing up")
                .that(File(context.filesDir, "identity-backup").exists())
                .isFalse()
        }

    @Test
    fun `a transferred soul keeps the body it arrived with`() =
        runTest {
            identity.hatch("Iskra", CreatureConcept.SPIRIT_ORB, foxSeed, HATCHED_AT)
            identity.markIdentityTransferred()

            val outcome = repair.run()

            assertThat(outcome).isInstanceOf(IdentityRepair.Outcome.LeftAlone::class.java)
            assertThat((outcome as IdentityRepair.Outcome.LeftAlone).reason).contains("soul file")
            assertThat(identity.concept()).isEqualTo(CreatureConcept.SPIRIT_ORB)
        }

    @Test
    fun `a seed of zero is not a seed`() =
        runTest {
            identity.hatch("Iskra", CreatureConcept.SPIRIT_ORB, 0L, HATCHED_AT)
            identity.markIdentityAssigned()
            liveALife()

            val outcome = repair.run() as IdentityRepair.Outcome.Repaired

            assertThat(outcome.findings.any { "seed was 0" in it }).isTrue()
            assertWithMessage("a repaired seed must be this device's, not another default")
                .that(identity.seed())
                .isNotEqualTo(0L)
            assertThat(identity.concept()).isEqualTo(CreatureConcept.assignedTo(identity.seed()!!))
            assertLifeIntact()
        }

    @Test
    fun `a body this build does not know is recomputed rather than left broken`() =
        runTest {
            identity.hatch("Iskra", CreatureConcept.FOX_KIT, foxSeed, HATCHED_AT)
            identity.markIdentityAssigned()
            // A ninth body from a future release, written straight into the row.
            db.metaDao().put(MetaEntity("creature_concept", "lantern"))
            liveALife()

            val outcome = repair.run() as IdentityRepair.Outcome.Repaired

            assertThat(outcome.findings.single()).contains("lantern")
            assertThat(identity.concept()).isEqualTo(CreatureConcept.FOX_KIT)
            assertLifeIntact()
        }

    @Test
    fun `a healthy assigned soul is not touched and leaves no trace`() =
        runTest {
            identity.hatch("Iskra", CreatureConcept.assignedTo(foxSeed), foxSeed, HATCHED_AT)
            identity.markIdentityAssigned()
            liveALife()

            assertThat(repair.run()).isEqualTo(IdentityRepair.Outcome.Healthy)
            assertThat(File(context.filesDir, "identity-backup").exists()).isFalse()
            assertLifeIntact()
        }

    @Test
    fun `a phone that has not hatched yet is left alone`() =
        runTest {
            assertThat(repair.run()).isEqualTo(IdentityRepair.Outcome.NoIdentity)
        }

    private companion object {
        const val HATCHED_AT = 1_700_000_000_000L
        const val DAY = 24L * 60 * 60 * 1000
    }
}
