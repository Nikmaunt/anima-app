package app.anima.core.modeldelivery

import android.app.ActivityManager
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
 * The one MindModelLocator the mind sees (ADR-010): a user-installed file
 * (download/SAF — an explicit choice) always wins; the Play pack is the
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

        val forceGemma: Flow<Boolean> =
            context.mindDeliveryDataStore.data.map { it[forceGemmaKey] ?: false }

        suspend fun setForceGemma(value: Boolean) {
            context.mindDeliveryDataStore.edit { it[forceGemmaKey] = value }
        }

        override val installed: StateFlow<InstalledMindModel?> =
            combine(store.installed, pack.phase, forceGemma) { user, packPhase, force ->
                user ?: packModelIfAllowed(packPhase, force)
            }.stateIn(scope, SharingStarted.Eagerly, store.installed.value)

        private fun packModelIfAllowed(
            phase: PackPhase,
            force: Boolean,
        ): InstalledMindModel? {
            val ready = (phase as? PackPhase.Ready)?.model ?: return null
            if (!ramGate.allowsSpontaneousGemma() && !force) return null
            return InstalledMindModel(ready.fileName, ready.sizeBytes, ready.path, fromPack = true)
        }
    }
