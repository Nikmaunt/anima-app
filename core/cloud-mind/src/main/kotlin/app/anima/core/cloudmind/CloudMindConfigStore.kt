package app.anima.core.cloudmind

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.anima.core.model.CloudMindConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.cloudMindDataStore by preferencesDataStore(name = "cloud_mind")

/**
 * Non-secret cloud-mind settings (ADR-011). The API key itself NEVER enters
 * DataStore — it lives Keystore-wrapped in [CloudKeyVault]; this store only
 * reflects whether one exists. Endpoint host and model name are the user's
 * own configuration, not telemetry.
 */
@Singleton
class CloudMindConfigStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val vault: CloudKeyVault,
    ) {
        private val enabled = booleanPreferencesKey("cloud_enabled")
        private val baseUrl = stringPreferencesKey("cloud_base_url")
        private val model = stringPreferencesKey("cloud_model")

        val config: Flow<CloudMindConfig> =
            context.cloudMindDataStore.data.map { prefs ->
                CloudMindConfig(
                    enabled = prefs[enabled] ?: false,
                    baseUrl = prefs[baseUrl].orEmpty(),
                    model = prefs[model].orEmpty(),
                    hasKey = vault.hasKey(),
                )
            }

        suspend fun current(): CloudMindConfig = config.first()

        suspend fun setEnabled(value: Boolean) {
            context.cloudMindDataStore.edit { it[enabled] = value }
        }

        suspend fun setEndpoint(
            url: String,
            modelName: String,
        ) {
            context.cloudMindDataStore.edit {
                it[baseUrl] = url.trim().trimEnd('/')
                it[model] = modelName.trim()
            }
        }

        /** Stores the key wrapped; the passed array is zeroed by the vault. */
        fun storeKey(key: ByteArray) = vault.store(key)

        suspend fun clearKeyAndDisable() {
            vault.clear()
            setEnabled(false)
        }
    }
