package app.anima.core.data.di

import android.content.Context
import androidx.room.Room
import app.anima.core.data.AnimaDatabase
import app.anima.core.data.crypto.KeystoreSoulKeySource
import app.anima.core.data.crypto.SoulKeySource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

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
        keySource: SoulKeySource,
    ): AnimaDatabase {
        // SQLCipher native library must be loaded before the factory is used.
        System.loadLibrary("sqlcipher")
        val passphrase = keySource.passphrase()
        return Room.databaseBuilder(context, AnimaDatabase::class.java, AnimaDatabase.NAME)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()
    }
}
