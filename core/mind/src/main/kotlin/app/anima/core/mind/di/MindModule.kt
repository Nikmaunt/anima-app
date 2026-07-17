package app.anima.core.mind.di

import app.anima.core.mind.TieredMindEngine
import app.anima.core.model.MindEngine
import app.anima.core.model.MindInventory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MindModule {
    @Binds
    abstract fun mindEngine(impl: TieredMindEngine): MindEngine

    @Binds
    abstract fun mindInventory(impl: TieredMindEngine): MindInventory
}
