package app.anima.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.anima.core.data.entity.BodyJournalEntity
import app.anima.core.data.entity.ChatMessageEntity
import app.anima.core.data.entity.MetaEntity
import app.anima.core.data.entity.NotifEventEntity
import app.anima.core.data.entity.SoulFactEntity
import kotlinx.coroutines.flow.Flow

/**
 * soul_facts is append-only BY TYPE: this DAO exposes no @Delete and no
 * full-row @Update. Supersede is insert-new + a marker-only UPDATE guarded by
 * `supersededById IS NULL`; forget is a tombstone.
 */
@Dao
abstract class SoulFactDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(fact: SoulFactEntity)

    @Query("UPDATE soul_facts SET supersededById = :newId WHERE id = :oldId AND supersededById IS NULL")
    protected abstract suspend fun markSuperseded(oldId: String, newId: String): Int

    @Transaction
    open suspend fun supersede(oldId: String, replacement: SoulFactEntity) {
        insert(replacement)
        markSuperseded(oldId, replacement.id)
    }

    @Query("UPDATE soul_facts SET forgottenAtMillis = :atMillis WHERE id = :id AND forgottenAtMillis IS NULL")
    abstract suspend fun forget(id: String, atMillis: Long): Int

    @Query(
        "SELECT * FROM soul_facts WHERE supersededById IS NULL AND forgottenAtMillis IS NULL " +
            "ORDER BY createdAtMillis DESC",
    )
    abstract fun live(): Flow<List<SoulFactEntity>>

    @Query(
        "SELECT COUNT(*) FROM soul_facts WHERE supersededById IS NULL AND forgottenAtMillis IS NULL",
    )
    abstract fun liveCount(): Flow<Int>

    @Query("SELECT * FROM soul_facts ORDER BY createdAtMillis ASC")
    abstract suspend fun allIncludingDead(): List<SoulFactEntity>
}

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(message: ChatMessageEntity)

    @Query("SELECT * FROM (SELECT * FROM chat_messages ORDER BY atMillis DESC LIMIT :limit) ORDER BY atMillis ASC")
    fun recent(limit: Int): Flow<List<ChatMessageEntity>>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE role = 'user'")
    fun userMessageCount(): Flow<Int>
}

@Dao
interface BodyJournalDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: BodyJournalEntity)

    @Query("SELECT * FROM body_journal ORDER BY atMillis DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<BodyJournalEntity>>

    @Query("SELECT COUNT(*) FROM body_journal WHERE kind = :kind")
    suspend fun countOfKind(kind: String): Int

    @Query("SELECT COUNT(*) FROM body_journal WHERE kind = :kind AND atMillis >= :sinceMillis")
    suspend fun countOfKindSince(kind: String, sinceMillis: Long): Int
}

@Dao
interface NotifEventDao {
    /** IGNORE: reposts carry the same stable id and dedup silently. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: NotifEventEntity): Long

    @Query("SELECT COUNT(*) FROM notif_events WHERE postedAtMillis >= :sinceMillis")
    fun countSince(sinceMillis: Long): Flow<Int>

    @Query("SELECT * FROM notif_events WHERE postedAtMillis >= :sinceMillis ORDER BY postedAtMillis DESC")
    suspend fun since(sinceMillis: Long): List<NotifEventEntity>

    @Query(
        "SELECT packageName, COUNT(*) AS count FROM notif_events WHERE postedAtMillis >= :sinceMillis " +
            "GROUP BY packageName ORDER BY count DESC",
    )
    suspend fun perAppSince(sinceMillis: Long): List<PackageCount>

    /** Privacy cap, not an edit: events older than the horizon are erased. */
    @Query("DELETE FROM notif_events WHERE postedAtMillis < :beforeMillis")
    suspend fun pruneBefore(beforeMillis: Long): Int

    data class PackageCount(val packageName: String, val count: Int)
}

@Dao
interface MetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: MetaEntity)

    @Query("SELECT value FROM meta WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Query("SELECT value FROM meta WHERE `key` = :key")
    fun observe(key: String): Flow<String?>
}
