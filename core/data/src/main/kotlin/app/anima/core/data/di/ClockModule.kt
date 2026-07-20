package app.anima.core.data.di

import app.anima.core.model.AnimaClock
import app.anima.core.model.SystemAnimaClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * v0.6: [AnimaClock] rides its own module so tests can @TestInstallIn-replace
 * just the clock (the day-in-life E2E pins an evening) without touching the
 * rest of the data graph.
 */
@Module
@InstallIn(SingletonComponent::class)
object ClockModule {
    @Provides
    @Singleton
    fun animaClock(): AnimaClock = SystemAnimaClock()
}
