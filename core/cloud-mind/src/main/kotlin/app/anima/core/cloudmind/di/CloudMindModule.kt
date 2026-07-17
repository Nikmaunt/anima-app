package app.anima.core.cloudmind.di

import app.anima.core.cloudmind.CloudMindEngine
import app.anima.core.cloudmind.KeystoreSecretCipher
import app.anima.core.cloudmind.SecretCipher
import app.anima.core.model.CloudMindBackend
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CloudMindModule {
    /** :core:mind consumes the backend interface; only this module is networked. */
    @Binds
    abstract fun cloudMindBackend(impl: CloudMindEngine): CloudMindBackend

    @Binds
    abstract fun secretCipher(impl: KeystoreSecretCipher): SecretCipher
}
