package app.anima.core.voice

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.voiceDataStore by preferencesDataStore(name = "voice_prefs")

/**
 * Voice settings (ADR-013). Default OFF — the creature is mute until the
 * owner flips the toggle, and the first enable runs the offline-voice check.
 */
@Singleton
class VoiceConfigStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val enabledKey = booleanPreferencesKey("voice_enabled")

        val enabled: Flow<Boolean> = context.voiceDataStore.data.map { it[enabledKey] ?: false }

        suspend fun setEnabled(value: Boolean) {
            context.voiceDataStore.edit { it[enabledKey] = value }
        }
    }
