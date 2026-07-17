package app.anima.core.modeldelivery.di

import app.anima.core.model.MindModelLocator
import app.anima.core.modeldelivery.MindModelStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ModelDeliveryModule {
    /** :core:mind consumes the locator interface; only this module knows the store. */
    @Binds
    abstract fun mindModelLocator(impl: MindModelStore): MindModelLocator
}
