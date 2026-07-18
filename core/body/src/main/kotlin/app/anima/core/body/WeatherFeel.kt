package app.anima.core.body

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.anima.core.model.PressureSample
import app.anima.core.model.WeatherFeeling
import app.anima.core.model.WeatherSense
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

private val Context.weatherDataStore by preferencesDataStore(name = "weather_feel")

/**
 * The barometer sense (ideation-v5 №2). Follows the module's own law — life
 * only on screen: a sample is taken when the flow is collected (i.e. the
 * creature is visible) and every half hour while it stays visible. The
 * sparse log persists across visits; the trend speaks only when honest
 * hours of history exist (WeatherFeeling). No sensor → the sense is NONE,
 * silently — a budget phone simply has no bones that ache.
 */
@Singleton
class WeatherFeel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val logKey = stringPreferencesKey("pressure_log")

        fun weather(): Flow<WeatherSense> =
            flow {
                val sm = context.getSystemService(SensorManager::class.java)
                val sensor = sm?.getDefaultSensor(Sensor.TYPE_PRESSURE)
                if (sensor == null) {
                    emit(WeatherSense.NONE)
                    return@flow
                }
                while (true) {
                    val now = System.currentTimeMillis()
                    val stored =
                        WeatherFeeling.decode(
                            context.weatherDataStore.data
                                .first()[logKey]
                                .orEmpty(),
                        )
                    val hPa = readOnce(sm, sensor)
                    val log =
                        if (hPa != null) {
                            WeatherFeeling.append(stored, PressureSample(now, hPa)).also { updated ->
                                context.weatherDataStore.edit { it[logKey] = WeatherFeeling.encode(updated) }
                            }
                        } else {
                            stored
                        }
                    emit(WeatherFeeling.feel(log, now))
                    delay(RESAMPLE_MILLIS)
                }
            }.distinctUntilChanged()

        /** Median of a few events — one breath of the barometer, then stop. */
        private suspend fun readOnce(
            sm: SensorManager,
            sensor: Sensor,
        ): Float? =
            withTimeoutOrNull(READ_TIMEOUT_MILLIS) {
                val values =
                    pressureEvents(sm, sensor)
                        .take(EVENTS_PER_READ)
                        .toList()
                values.sorted().getOrNull(values.size / 2)
            }

        private fun pressureEvents(
            sm: SensorManager,
            sensor: Sensor,
        ): Flow<Float> =
            callbackFlow {
                val listener =
                    object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent) {
                            event.values.firstOrNull()?.let(::trySend)
                        }

                        override fun onAccuracyChanged(
                            s: Sensor?,
                            accuracy: Int,
                        ) = Unit
                    }
                sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                awaitClose { sm.unregisterListener(listener) }
            }

        private companion object {
            const val RESAMPLE_MILLIS = 30L * 60 * 1000
            const val READ_TIMEOUT_MILLIS = 3_000L
            const val EVENTS_PER_READ = 5
        }
    }
