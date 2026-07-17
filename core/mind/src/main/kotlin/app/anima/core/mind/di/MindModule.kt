package app.anima.core.mind.di

import app.anima.core.mind.NanoMindEngine
import app.anima.core.model.MindEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MindModule {
    @Binds
    abstract fun mindEngine(impl: NanoMindEngine): MindEngine
}
