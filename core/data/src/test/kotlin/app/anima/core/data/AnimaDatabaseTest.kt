package app.anima.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.anima.core.data.entity.NotifEventEntity
import app.anima.core.data.entity.SoulFactEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * DAO contract tests on the JVM (Robolectric, native SQLite). The DAO layer
 * is factory-agnostic: these tests use plain in-memory Room; the SQLCipher
 * factory + Keystore path is device-verified (manual checklist) because
 * Robolectric can load neither the native cipher nor AndroidKeyStore.
 */
@RunWith(RobolectricTestRunner::class)
class AnimaDatabaseTest {

    private lateinit var db: AnimaDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AnimaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun fact(id: String, text: String = "t") = SoulFactEntity(
        id = id, category = "preference", text = text, source = "chat_confirmed",
        createdAtMillis = 1L, supersededById = null, forgottenAtMillis = null,
    )

    @Test
    fun `supersede keeps old row but hides it from live`() = runTest {
        val dao = db.soulFactDao()
        dao.insert(fact("fact-1", "old"))
        dao.supersede("fact-1", fact("fact-2", "new"))

        val live = dao.live().first()
        assertThat(live.map { it.id }).containsExactly("fact-2")
        val all = dao.allIncludingDead()
        assertThat(all).hasSize(2)
        assertThat(all.first { it.id == "fact-1" }.supersededById).isEqualTo("fact-2")
    }

    @Test
    fun `double supersede is a no-op - no marker overwrite, no orphan`() = runTest {
        val dao = db.soulFactDao()
        dao.insert(fact("fact-1"))
        dao.supersede("fact-1", fact("fact-2"))
        dao.supersede("fact-1", fact("fact-3")) // must not stick NOR insert

        val original = dao.allIncludingDead().first { it.id == "fact-1" }
        assertThat(original.supersededById).isEqualTo("fact-2")
        assertThat(dao.live().first().map { it.id }).containsExactly("fact-2")
        assertThat(dao.allIncludingDead().map { it.id }).containsExactly("fact-1", "fact-2")
    }

    @Test
    fun `forget is a tombstone not a delete`() = runTest {
        val dao = db.soulFactDao()
        dao.insert(fact("fact-1"))
        dao.forget("fact-1", atMillis = 99L)

        assertThat(dao.live().first()).isEmpty()
        assertThat(dao.allIncludingDead().single().forgottenAtMillis).isEqualTo(99L)
    }

    @Test
    fun `notif repost with same id dedups`() = runTest {
        val dao = db.notifEventDao()
        val event = NotifEventEntity("com.x:1:abc", "com.x", 1000L, "T", null)
        dao.insert(event)
        dao.insert(event.copy(title = "T2"))

        assertThat(dao.since(0L)).hasSize(1)
        assertThat(dao.since(0L).single().title).isEqualTo("T")
    }

    @Test
    fun `notif prune erases old events only`() = runTest {
        val dao = db.notifEventDao()
        dao.insert(NotifEventEntity("a", "com.x", 1000L, "old", null))
        dao.insert(NotifEventEntity("b", "com.x", 5000L, "new", null))
        dao.pruneBefore(2000L)

        assertThat(dao.since(0L).map { it.id }).containsExactly("b")
    }

    @Test
    fun `unknown enum wire degrades safely through repository mapping`() = runTest {
        val dao = db.soulFactDao()
        dao.insert(fact("fact-1").copy(category = "category_from_the_future"))
        val repo = app.anima.core.data.repo.SoulRepository(dao)
        val live = repo.liveFacts().first()
        assertThat(live.single().category).isEqualTo(app.anima.core.model.FactCategory.OTHER)
    }

    @Test
    fun `meta put get roundtrip`() = runTest {
        val dao = db.metaDao()
        dao.put(app.anima.core.data.entity.MetaEntity("creature_name", "Люмик"))
        dao.put(app.anima.core.data.entity.MetaEntity("creature_name", "Мия"))
        assertThat(dao.get("creature_name")).isEqualTo("Мия")
    }
}
