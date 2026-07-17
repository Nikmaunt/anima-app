package app.anima.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import app.anima.core.data.dao.BodyJournalDao
import app.anima.core.data.dao.ChatDao
import app.anima.core.data.dao.MetaDao
import app.anima.core.data.dao.NotifEventDao
import app.anima.core.data.dao.SoulFactDao
import app.anima.core.data.entity.BodyJournalEntity
import app.anima.core.data.entity.ChatMessageEntity
import app.anima.core.data.entity.MetaEntity
import app.anima.core.data.entity.NotifEventEntity
import app.anima.core.data.entity.SoulFactEntity

/**
 * The soul's vault. Opened only through the SQLCipher factory with a
 * Keystore-wrapped passphrase (ADR-003); exportSchema keeps every migration
 * reviewable under version control.
 */
@Database(
    entities = [
        SoulFactEntity::class,
        ChatMessageEntity::class,
        BodyJournalEntity::class,
        NotifEventEntity::class,
        MetaEntity::class,
    ],
    version = AnimaDatabase.VERSION,
    exportSchema = true,
)
abstract class AnimaDatabase : RoomDatabase() {
    abstract fun soulFactDao(): SoulFactDao
    abstract fun chatDao(): ChatDao
    abstract fun bodyJournalDao(): BodyJournalDao
    abstract fun notifEventDao(): NotifEventDao
    abstract fun metaDao(): MetaDao

    companion object {
        const val VERSION = 1
        const val NAME = "soul.db"
    }
}
