package app.anima

import app.anima.core.data.di.ClockModule
import app.anima.core.model.AnimaClock
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Singleton

/**
 * v0.6: pins the creature's clock to EVENING (21:30, inside the goodnight
 * window 21:00–23:00 and outside MoodEngine night 23:00–07:00) so the
 * day-in-life E2E can walk the farewell ritual and capsule delivery at any
 * wall-clock hour. Still *advances* in real time — equal timestamps would
 * scramble chat ordering.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [ClockModule::class])
object TestClockModule {
    val ZONE: ZoneId = ZoneId.of("UTC")

    /** 21:30 UTC "today" (date taken from the real clock at class load). */
    val EVENING_BASE_MILLIS: Long =
        ZonedDateTime
            .now(ZONE)
            .withHour(21)
            .withMinute(30)
            .withSecond(0)
            .withNano(0)
            .toInstant()
            .toEpochMilli()

    private val realStart = System.currentTimeMillis()

    @Provides
    @Singleton
    fun eveningClock(): AnimaClock =
        object : AnimaClock {
            override fun nowMillis(): Long = EVENING_BASE_MILLIS + (System.currentTimeMillis() - realStart)

            override fun zone(): ZoneId = ZONE
        }
}
