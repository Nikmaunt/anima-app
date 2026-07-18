package app.anima

import app.anima.core.mind.di.MindModule
import app.anima.core.model.LocalMindEngine
import app.anima.core.model.MindEngine
import app.anima.core.model.MindInventory
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Replaces the production tier ladder with [FakeMind] for the whole
 * instrumented suite: every mind-facing binding resolves to the same
 * deterministic instance.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [MindModule::class])
object TestMindModule {
    @Provides
    @Singleton
    fun fakeMind(): FakeMind = FakeMind()

    @Provides
    fun mindEngine(fake: FakeMind): MindEngine = fake

    @Provides
    fun localMindEngine(fake: FakeMind): LocalMindEngine = fake

    @Provides
    fun mindInventory(fake: FakeMind): MindInventory = fake
}
