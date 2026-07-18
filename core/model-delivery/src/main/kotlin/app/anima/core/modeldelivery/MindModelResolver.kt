package app.anima.core.modeldelivery

import android.app.ActivityManager
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.anima.core.model.InstalledMindModel
import app.anima.core.model.MindModelLocator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

private val Context.mindDeliveryDataStore by preferencesDataStore(name = "mind_delivery")

/**
 * ADR-010 RAM gate. Gemma 3 1B int4 peaks ~1.0–1.2 GB (research-v3 §A.6);
 * below ~6 GB total RAM the pack model is not loaded spontaneously — the
 * user can still force it from the Mind screen (stored override), pick the
 * cloud mind, or let the creature live without inference.
 */
@Singleton
class RamGate
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        val totalRamBytes: Long by lazy {
            val info = ActivityManager.MemoryInfo()
            context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(info)
            info.totalMem
        }

        /** ~6 GB devices report ≥5.2 GB totalMem after kernel reservations. */
        fun allowsSpontaneousGemma(): Boolean = totalRamBytes >= MIN_TOTAL_RAM_BYTES

        private companion object {
            const val MIN_TOTAL_RAM_BYTES = 52L * 1024 * 1024 * 1024 / 10
        }
    }

/**
 * The one MindModelLocator the mind sees (ADR-010, amended by ADR-017): the
 * owner's explicit SELECTION wins; otherwise a user-installed file
 * (download/SAF — an explicit choice) beats the Play pack, which stays the
 * zero-setup default behind the RAM gate + override.
 */
@Singleton
class MindModelResolver
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val store: MindModelStore,
        private val pack: PackModelSource,
        private val ramGate: RamGate,
    ) : MindModelLocator {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        private val forceGemmaKey = booleanPreferencesKey("force_gemma_below_ram_gate")
        private val selectedModelKey = stringPreferencesKey("selected_model_file")

        val forceGemma: Flow<Boolean> =
            context.mindDeliveryDataStore.data.map { it[forceGemmaKey] ?: false }

        suspend fun setForceGemma(value: Boolean) {
            context.mindDeliveryDataStore.edit { it[forceGemmaKey] = value }
        }

        /** File name of the owner's chosen model; null = automatic order. */
        val selectedModel: Flow<String?> =
            context.mindDeliveryDataStore.data.map { it[selectedModelKey] }

        suspend fun setSelectedModel(fileName: String?) {
            context.mindDeliveryDataStore.edit { prefs ->
                if (fileName == null) prefs.remove(selectedModelKey) else prefs[selectedModelKey] = fileName
            }
        }

        /**
         * Everything switchable on the Mind screen: user files plus the
         * delivered pack model (when the RAM gate or override lets it speak).
         */
        val available: Flow<List<InstalledMindModel>> =
            combine(store.models, pack.phase, forceGemma) { user, packPhase, force ->
                user + listOfNotNull(packModelIfAllowed(packPhase, force))
            }

        override val installed: StateFlow<InstalledMindModel?> =
            combine(store.models, selectedModel, pack.phase, forceGemma) { user, selected, packPhase, force ->
                val packModel = packModelIfAllowed(packPhase, force)
                val all = user + listOfNotNull(packModel)
                // A selection that no longer exists (deleted file) falls
                // through to the automatic order instead of muting the mind.
                selected?.let { name -> all.firstOrNull { it.fileName == name } }
                    ?: user.firstOrNull()
                    ?: packModel
            }.stateIn(scope, SharingStarted.Eagerly, store.models.value.firstOrNull())

        private fun packModelIfAllowed(
            phase: PackPhase,
            force: Boolean,
        ): InstalledMindModel? {
            val ready = (phase as? PackPhase.Ready)?.model ?: return null
            if (!ramGate.allowsSpontaneousGemma() && !force) return null
            return InstalledMindModel(ready.fileName, ready.sizeBytes, ready.path, fromPack = true)
        }
    }
