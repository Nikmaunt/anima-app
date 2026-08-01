package app.anima.core.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.anima.core.model.Personality
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.animaDataStore by preferencesDataStore(name = "anima_settings")

/** ADR-020: the engine switch as read by the mind module, backed by prefs. */
@Singleton
class PrefsMindEngineSwitch
    @Inject
    constructor(
        prefs: AnimaPrefs,
    ) : app.anima.core.model.MindEngineSwitch {
        override val altLocalEngine: Flow<Boolean> = prefs.litertlmEngine()
    }

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

        private val deviceSeedFallback = longPreferencesKey("device_seed_fallback")

        /**
         * v1.1c task 2.3 — the seed used when `ANDROID_ID` cannot be read.
         *
         * It used to be the FNV-1a of the constant string `"anima-fallback"`,
         * which is the default-body defect wearing different clothes and worse:
         * every phone in the world whose device id failed to read would hatch
         * the *same* creature — same body, same hue, same size, same blink rate.
         * A default body is one wrong frame; a default seed is one wrong life.
         *
         * So: roll once, remember forever. Null means "not rolled yet" — never
         * a number, because a number here would be exactly the bug.
         */
        suspend fun deviceSeedFallback(): Long? = context.animaDataStore.data.first()[deviceSeedFallback]

        /** Writes the rolled seed only if there is not one already. Returns the seed in force. */
        suspend fun rememberDeviceSeedFallback(rolled: Long): Long {
            var inForce = rolled
            context.animaDataStore.edit { prefs ->
                val existing = prefs[deviceSeedFallback]
                if (existing == null) prefs[deviceSeedFallback] = rolled else inForce = existing
            }
            return inForce
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

        private val paletteVariant = stringPreferencesKey("palette_variant")

        /** v0.4 milestones: chosen PaletteVariant wire; null = true self. */
        fun paletteVariant(): Flow<String?> = context.animaDataStore.data.map { it[paletteVariant] }

        suspend fun setPaletteVariant(wire: String) {
            context.animaDataStore.edit { it[paletteVariant] = wire }
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

        private val litertlmEngine = booleanPreferencesKey("litertlm_engine")

        /**
         * ADR-020 developer flag: serve the local model tier through the
         * LiteRT-LM runtime instead of tasks-genai. Default OFF — the shipped
         * runtime stays tasks-genai; the toggle is exposed only in debug
         * builds' developer section (default pinned by AnimaPrefsDefaultsTest).
         */
        fun litertlmEngine(): Flow<Boolean> = context.animaDataStore.data.map { it[litertlmEngine] ?: false }

        suspend fun setLitertlmEngine(value: Boolean) {
            context.animaDataStore.edit { it[litertlmEngine] = value }
        }

        private val experimentalChat = booleanPreferencesKey("experimental_chat")

        /**
         * v1.1: chat and the Mind screen are demoted, not deleted. Off by
         * default, and reachable only from Settings — Home no longer carries
         * a chat slot at all. The creature is the product; a text box that
         * needs a 1.2 GB download to say anything was competing with it for
         * the screen and losing.
         *
         * Default pinned by AnimaPrefsDefaultsTest, same as every other flag
         * whose default is a promise rather than a preference.
         */
        fun experimentalChat(): Flow<Boolean> = context.animaDataStore.data.map { it[experimentalChat] ?: false }

        suspend fun setExperimentalChat(value: Boolean) {
            context.animaDataStore.edit { it[experimentalChat] = value }
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
