package app.anima.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.anima.core.data.dao.BodyJournalDao
import app.anima.core.data.dao.ChatDao
import app.anima.core.data.dao.MetaDao
import app.anima.core.data.dao.NotifEventDao
import app.anima.core.data.dao.SoulFactDao
import app.anima.core.data.dao.TimeCapsuleDao
import app.anima.core.data.entity.BodyJournalEntity
import app.anima.core.data.entity.ChatMessageEntity
import app.anima.core.data.entity.MetaEntity
import app.anima.core.data.entity.NotifEventEntity
import app.anima.core.data.entity.SoulFactEntity
import app.anima.core.data.entity.TimeCapsuleEntity

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
        TimeCapsuleEntity::class,
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

    abstract fun timeCapsuleDao(): TimeCapsuleDao

    companion object {
        const val VERSION = 2
        const val NAME = "soul.db"

        /** v0.5: time capsules (ideation-v5 №3). Additive — nothing touched. */
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `time_capsules` (" +
                            "`id` TEXT NOT NULL, `text` TEXT NOT NULL, " +
                            "`createdAtMillis` INTEGER NOT NULL, " +
                            "`deliverAtMillis` INTEGER NOT NULL, " +
                            "`openedAtMillis` INTEGER, PRIMARY KEY(`id`))",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_time_capsules_deliverAtMillis` " +
                            "ON `time_capsules` (`deliverAtMillis`)",
                    )
                }
            }
    }
}
