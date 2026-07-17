package app.anima.core.modeldelivery.di

import app.anima.core.model.MindModelLocator
import app.anima.core.modeldelivery.MindModelResolver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ModelDeliveryModule {
    /**
     * :core:mind consumes the locator interface. v0.3 (ADR-010): the
     * resolver merges the user-installed file with the Play pack; only this
     * module knows either exists.
     */
    @Binds
    abstract fun mindModelLocator(impl: MindModelResolver): MindModelLocator
}
