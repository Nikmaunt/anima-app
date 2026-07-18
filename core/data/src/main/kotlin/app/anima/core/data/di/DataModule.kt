package app.anima.core.data.di

import android.content.Context
import androidx.room.Room
import app.anima.core.data.AnimaDatabase
import app.anima.core.data.crypto.KeystoreSoulKeySource
import app.anima.core.data.crypto.SoulKeyHolder
import app.anima.core.data.crypto.SoulKeySource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataBindings {
    @Binds
    abstract fun soulKeySource(impl: KeystoreSoulKeySource): SoulKeySource
}

@Module
@InstallIn(SingletonComponent::class)
internal object DataModule {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
        keyHolder: SoulKeyHolder,
    ): AnimaDatabase {
        // SQLCipher native library must be loaded before the factory is used.
        System.loadLibrary("sqlcipher")
        // sqlcipher-android never zeroes the passphrase itself and aliases
        // the array we pass (re-verified against 4.17.0 bytecode, audit-v03
        // §1). The pool re-keys every new physical connection from that
        // array, so the holder keeps it alive for the process lifetime —
        // zeroing it would crash WAL pool growth (ADR-003 addendum v0.4).
        return Room
            .databaseBuilder(context, AnimaDatabase::class.java, AnimaDatabase.NAME)
            .openHelperFactory(SupportOpenHelperFactory(keyHolder.passphrase()))
            .build()
    }

    @Provides
    fun soulFactDao(db: AnimaDatabase) = db.soulFactDao()

    @Provides
    fun chatDao(db: AnimaDatabase) = db.chatDao()

    @Provides
    fun bodyJournalDao(db: AnimaDatabase) = db.bodyJournalDao()

    @Provides
    fun notifEventDao(db: AnimaDatabase) = db.notifEventDao()

    @Provides
    fun metaDao(db: AnimaDatabase) = db.metaDao()
}
