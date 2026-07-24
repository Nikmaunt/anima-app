package app.anima.core.mind.di

import app.anima.core.mind.AltLocalEngine
import app.anima.core.mind.LiteRtLmMindEngine
import app.anima.core.model.MindEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * ADR-020: satisfies MindModule's @BindsOptionalOf in DEBUG builds only —
 * this source set does not exist in release, so the release graph resolves
 * the Optional empty and the LiteRT-LM runtime never ships.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LiteRtLmDebugModule {
    @Binds
    @AltLocalEngine
    abstract fun altLocalEngine(impl: LiteRtLmMindEngine): MindEngine
}
