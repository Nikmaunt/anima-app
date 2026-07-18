package app.anima.core.mind.di

import app.anima.core.mind.TieredMindEngine
import app.anima.core.model.LocalMindEngine
import app.anima.core.model.MindEngine
import app.anima.core.model.MindInventory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MindModule {
    @Binds
    abstract fun mindEngine(impl: TieredMindEngine): MindEngine

    @Binds
    abstract fun mindInventory(impl: TieredMindEngine): MindInventory

    companion object {
        /** ADR-011 data-scope guard: the cloud-free view of the tiers. */
        @Provides
        fun localMindEngine(impl: TieredMindEngine): LocalMindEngine = impl.localOnly
    }
}
