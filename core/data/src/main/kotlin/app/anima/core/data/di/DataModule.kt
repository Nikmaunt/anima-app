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
        // sqlcipher-android never zeroes the passphrase itself (verified
        // against the 4.6.1 bytecode: the factory retains the array as-is;
        // the boolean ctor param is WAL, not clearPassphrase). The holder
        // keeps the one reference; AnimaApp zeroes it right after the eager
        // first open. ADR-003 addendum records the native-side residue.
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
