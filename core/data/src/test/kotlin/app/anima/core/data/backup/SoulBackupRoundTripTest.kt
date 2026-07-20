package app.anima.core.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.anima.core.data.AnimaDatabase
import app.anima.core.data.entity.BodyJournalEntity
import app.anima.core.data.entity.SoulFactEntity
import app.anima.core.data.entity.TimeCapsuleEntity
import app.anima.core.data.repo.IdentityRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * v0.6 (audit-v05 D5): the payload path had zero tests despite the handoff
 * claiming "v1 imports fine". Three contracts pinned here on real DAOs:
 * a full v2 round trip into a fresh database, a handcrafted VERSION-1
 * fixture (no timeCapsules key) importing tolerantly, and the future-file
 * rejection. Re-import idempotency rides the round-trip test.
 */
@RunWith(RobolectricTestRunner::class)
class SoulBackupRoundTripTest {
    private lateinit var source: AnimaDatabase
    private lateinit var target: AnimaDatabase

    private fun db(): AnimaDatabase =
        Room
            .inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext<Context>(),
                AnimaDatabase::class.java,
            ).allowMainThreadQueries()
            .build()

    private fun backup(db: AnimaDatabase) =
        SoulBackup(
            identity = IdentityRepository(db.metaDao(), db.chatDao(), db.soulFactDao(), db.bodyJournalDao()),
            soulFactDao = db.soulFactDao(),
            journalDao = db.bodyJournalDao(),
            capsuleDao = db.timeCapsuleDao(),
        )

    @Before
    fun setUp() {
        source = db()
        target = db()
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    @Test
    fun `v2 payload round-trips the whole soul into a fresh database`() =
        runTest {
            val sourceBackup = backup(source)
            IdentityRepository(source.metaDao(), source.chatDao(), source.soulFactDao(), source.bodyJournalDao())
                .hatch("Iskra", app.anima.core.model.CreatureConcept.SPIRIT_ORB, 77L, 1_000L)
            source.soulFactDao().insert(
                SoulFactEntity("f1", "identity", "name is Nika", "onboarding", 1_000, null, null),
            )
            source.soulFactDao().insert(
                SoulFactEntity("f2", "preference", "black tea", "chat_confirmed", 2_000, "f3", null),
            )
            source.soulFactDao().insert(
                SoulFactEntity("f3", "preference", "green tea", "chat_confirmed", 3_000, null, null),
            )
            source.soulFactDao().insert(
                SoulFactEntity("f4", "moment", "let this go", "chat_confirmed", 4_000, null, 9_000),
            )
            source.bodyJournalDao().insert(BodyJournalEntity("j1", "hatched", 1_000, null))
            source.bodyJournalDao().insert(BodyJournalEntity("j2", "rest_session", 5_000, "300"))
            source.timeCapsuleDao().insert(TimeCapsuleEntity("c1", "hello future", 6_000, 700_000, null))
            source.timeCapsuleDao().insert(TimeCapsuleEntity("c2", "opened one", 1_000, 2_000, 3_000))

            val payload = sourceBackup.exportPayload(nowMillis = 10_000)
            val summary = backup(target).importPayload(payload)

            assertThat(summary.creatureName).isEqualTo("Iskra")
            assertThat(summary.factsImported).isEqualTo(4)
            assertThat(summary.factsSkipped).isEqualTo(0)
            assertThat(summary.journalImported).isEqualTo(2)

            val identity =
                IdentityRepository(target.metaDao(), target.chatDao(), target.soulFactDao(), target.bodyJournalDao())
            assertThat(identity.name()).isEqualTo("Iskra")
            // The relationship's age travels: the ORIGINAL hatch date wins.
            assertThat(identity.hatchedAtMillis()).isEqualTo(1_000L)

            val facts = target.soulFactDao().allIncludingDead().associateBy { it.id }
            assertThat(facts.keys).containsExactly("f1", "f2", "f3", "f4")
            assertThat(facts.getValue("f2").supersededById).isEqualTo("f3")
            assertThat(facts.getValue("f4").forgottenAtMillis).isEqualTo(9_000L)

            val capsules = target.timeCapsuleDao().all().associateBy { it.id }
            assertThat(capsules.keys).containsExactly("c1", "c2")
            assertThat(capsules.getValue("c1").openedAtMillis).isNull()
            assertThat(capsules.getValue("c2").openedAtMillis).isEqualTo(3_000L)

            // Re-importing your own backup is a no-op, not a crash or a dupe.
            val again = backup(target).importPayload(payload)
            assertThat(again.factsImported).isEqualTo(0)
            assertThat(again.factsSkipped).isEqualTo(4)
            assertThat(target.soulFactDao().allIncludingDead()).hasSize(4)
            assertThat(target.timeCapsuleDao().all()).hasSize(2)
        }

    @Test
    fun `version 1 fixture without timeCapsules imports tolerantly`() =
        runTest {
            // A faithful v0.4-era file: version 1, no timeCapsules key.
            val v1 =
                """
                {"format":"anima-soul","version":1,"exportedAtMillis":5000,
                 "creature":{"name":"Kiki","concept":"spirit_orb","seed":42,"hatchedAtMillis":100},
                 "facts":[{"id":"f1","category":"identity","text":"old friend","source":"onboarding","createdAtMillis":200}],
                 "journal":[{"id":"j1","kind":"hatched","atMillis":100}]}
                """.trimIndent().toByteArray()

            val summary = backup(target).importPayload(v1)

            assertThat(summary.creatureName).isEqualTo("Kiki")
            assertThat(summary.factsImported).isEqualTo(1)
            assertThat(summary.journalImported).isEqualTo(1)
            assertThat(target.timeCapsuleDao().all()).isEmpty()
        }

    @Test
    fun `a file from a newer app is rejected whole`() =
        runTest {
            val v3 =
                (
                    """{"format":"anima-soul","version":3,""" +
                        """"creature":{"name":"X","concept":"spirit_orb","seed":1,"hatchedAtMillis":1},""" +
                        """"facts":[],"journal":[]}"""
                ).toByteArray()
            assertThrows(IllegalArgumentException::class.java) {
                kotlinx.coroutines.runBlocking { backup(target).importPayload(v3) }
            }
            assertThat(target.soulFactDao().allIncludingDead()).isEmpty()
        }
}
