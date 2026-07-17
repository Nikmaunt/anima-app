package app.anima.core.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.anima.core.model.Personality
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.animaDataStore by preferencesDataStore(name = "anima_settings")

/** Non-secret app preferences. Anything personal lives in the encrypted DB. */
@Singleton
class AnimaPrefs
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val onboardingDone = booleanPreferencesKey("onboarding_done")
        private val calmMotion = booleanPreferencesKey("calm_motion")

        fun onboardingDone(): Flow<Boolean> = context.animaDataStore.data.map { it[onboardingDone] ?: false }

        suspend fun setOnboardingDone() {
            context.animaDataStore.edit { it[onboardingDone] = true }
        }

        /** In-app reduced-motion override on top of the system animator scale. */
        fun calmMotion(): Flow<Boolean> = context.animaDataStore.data.map { it[calmMotion] ?: false }

        suspend fun setCalmMotion(value: Boolean) {
            context.animaDataStore.edit { it[calmMotion] = value }
        }

        private val personalityWarmth = floatPreferencesKey("personality_warmth")
        private val personalityChattiness = floatPreferencesKey("personality_chattiness")

        /** null = never customised → the concept preset applies. */
        fun personality(): Flow<Personality?> =
            context.animaDataStore.data.map { prefs ->
                val warmth = prefs[personalityWarmth] ?: return@map null
                val chattiness = prefs[personalityChattiness] ?: return@map null
                Personality(warmth, chattiness).clamped()
            }

        suspend fun setPersonality(value: Personality) {
            val clamped = value.clamped()
            context.animaDataStore.edit {
                it[personalityWarmth] = clamped.warmth
                it[personalityChattiness] = clamped.chattiness
            }
        }

        suspend fun clearPersonality() {
            context.animaDataStore.edit {
                it.remove(personalityWarmth)
                it.remove(personalityChattiness)
            }
        }

        private val soulScreenshotsAllowed = booleanPreferencesKey("soul_screenshots_allowed")

        /** Default OFF: the Soul screen sets FLAG_SECURE (threat-model.md). */
        fun soulScreenshotsAllowed(): Flow<Boolean> =
            context.animaDataStore.data.map { it[soulScreenshotsAllowed] ?: false }

        suspend fun setSoulScreenshotsAllowed(value: Boolean) {
            context.animaDataStore.edit { it[soulScreenshotsAllowed] = value }
        }

        private val lastGreetingDay = longPreferencesKey("last_greeting_epoch_day")

        /**
         * Morning-greeting ritual gate: deterministic once-per-local-day
         * trigger, checked at first screen open (never in background).
         */
        suspend fun shouldGreetToday(epochDay: Long): Boolean {
            val last =
                context.animaDataStore.data
                    .map { it[lastGreetingDay] ?: 0L }
                    .first()
            return epochDay > last
        }

        suspend fun markGreetedToday(epochDay: Long) {
            context.animaDataStore.edit { it[lastGreetingDay] = epochDay }
        }

        private val lastDreamNight = longPreferencesKey("last_dream_night_key")

        /** v0.3 dreams: one per night; the key is the day the night began. */
        suspend fun shouldDreamTonight(nightKey: Long): Boolean =
            context.animaDataStore.data
                .map { it[lastDreamNight] ?: Long.MIN_VALUE }
                .first() < nightKey

        suspend fun markDreamTold(nightKey: Long) {
            context.animaDataStore.edit { it[lastDreamNight] = nightKey }
        }

        private val lastBirthdayYear = longPreferencesKey("last_birthday_year")

        /** v0.3: hatch-anniversary celebration, once per calendar year. */
        suspend fun shouldCelebrateBirthday(year: Long): Boolean =
            context.animaDataStore.data
                .map { it[lastBirthdayYear] ?: 0L }
                .first() < year

        suspend fun markBirthdayCelebrated(year: Long) {
            context.animaDataStore.edit { it[lastBirthdayYear] = year }
        }
    }
