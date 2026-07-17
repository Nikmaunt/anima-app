package app.anima.core.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.animaDataStore by preferencesDataStore(name = "anima_settings")

/** Non-secret app preferences. Anything personal lives in the encrypted DB. */
@Singleton
class AnimaPrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val onboardingDone = booleanPreferencesKey("onboarding_done")
    private val calmMotion = booleanPreferencesKey("calm_motion")

    fun onboardingDone(): Flow<Boolean> =
        context.animaDataStore.data.map { it[onboardingDone] ?: false }

    suspend fun setOnboardingDone() {
        context.animaDataStore.edit { it[onboardingDone] = true }
    }

    /** In-app reduced-motion override on top of the system animator scale. */
    fun calmMotion(): Flow<Boolean> =
        context.animaDataStore.data.map { it[calmMotion] ?: false }

    suspend fun setCalmMotion(value: Boolean) {
        context.animaDataStore.edit { it[calmMotion] = value }
    }
}
