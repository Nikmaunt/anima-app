package app.anima.core.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.anima.core.data.entity.SoulFactEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * Regression pin for the v0.3 pool-growth crash (ADR-003 addendum v0.4):
 * sqlcipher re-keys every NEW physical connection from the aliased
 * passphrase array, so the array must stay intact for the pool's lifetime.
 * The v0.2/v0.3 "zero after first open" ritual made any non-primary
 * connection open die with SQLiteNotADatabaseException. This test forces
 * exactly that pool growth: a write transaction holds the primary
 * connection on this thread while a concurrent reader thread needs a
 * second physical connection — which sqlcipher keys from the same array.
 */
@RunWith(AndroidJUnit4::class)
class WalPoolKeyDeviceTest {
    private lateinit var context: Context
    private val passphrase = "wal-pool-test-passphrase-32-byte".toByteArray()

    @Before
    fun setUp() {
        System.loadLibrary("sqlcipher")
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DB)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DB)
    }

    private fun fact(id: String) =
        SoulFactEntity(
            id = id,
            category = "preference",
            text = "t",
            source = "chat_confirmed",
            createdAtMillis = 1L,
            supersededById = null,
            forgottenAtMillis = null,
        )

    @Test
    fun concurrent_read_during_held_write_transaction_opens_second_connection_and_succeeds() {
        val db =
            Room
                .databaseBuilder(context, AnimaDatabase::class.java, DB)
                .openHelperFactory(SupportOpenHelperFactory(passphrase.copyOf()))
                // Production runs AUTOMATIC (= WAL on real devices); pinned
                // explicitly here so the test forces pool growth everywhere.
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .allowMainThreadQueries()
                .build()
        try {
            runBlocking { db.soulFactDao().insert(fact("seed")) }

            val writable = db.openHelper.writableDatabase
            check(writable.isWriteAheadLoggingEnabled) {
                "WAL must be on for this test to force a non-primary connection"
            }

            // Mimic v0.3's post-warm state: DB open, pool at one connection.
            // (Pre-fix, the passphrase array was zeroed at this point, and
            // the concurrent reader below crashed the process.)
            writable.beginTransaction()
            try {
                val failure = AtomicReference<Throwable?>()
                val done = CountDownLatch(1)
                thread(name = "wal-pool-reader") {
                    try {
                        // Needs its own physical connection: the primary is
                        // bound to the held transaction on the test thread.
                        val rows = runBlocking { db.soulFactDao().allIncludingDead() }
                        check(rows.isNotEmpty())
                    } catch (t: Throwable) {
                        failure.set(t)
                    } finally {
                        done.countDown()
                    }
                }
                assertThat(done.await(30, TimeUnit.SECONDS)).isTrue()
                failure.get()?.let { throw AssertionError("concurrent read failed", it) }
            } finally {
                writable.endTransaction()
            }
        } finally {
            db.close()
        }
    }

    private companion object {
        const val DB = "wal-pool-test.db"
    }
}
