package app.anima.core.model

import kotlinx.coroutines.flow.StateFlow

/**
 * ADR-005/ADR-011: which backend is speaking. CLOUD = user-keyed remote
 * endpoint, strictly opt-in (never chosen silently). NANO = ML Kit Prompt
 * API via AICore (system-managed). GEMMA = local Gemma 3 1B through
 * MediaPipe, model file owned by :core:model-delivery. NONE = the mind
 * sleeps; never faked.
 */
enum class MindTier { CLOUD, NANO, GEMMA, NONE }

/** A validated local model file for the GEMMA tier. */
data class InstalledMindModel(
    val fileName: String,
    val sizeBytes: Long,
    val path: String,
    /** True when Play's asset pack delivered it (ADR-010) — not deletable in-app. */
    val fromPack: Boolean = false,
)

/**
 * Where the GEMMA tier finds its model. Implemented by :core:model-delivery
 * (the only networked module); consumed by :core:mind, which stays
 * network-free by depending on this interface instead of the module.
 */
interface MindModelLocator {
    val installed: StateFlow<InstalledMindModel?>
}

/** What the Mind screen shows: every tier's honest state at once. */
data class MindSnapshot(
    val activeTier: MindTier,
    val nano: MindStatus,
    val gemmaModel: InstalledMindModel?,
    val cloud: CloudMindConfig = CloudMindConfig.Disabled,
)

/**
 * ADR-011: the optional user-keyed cloud mind's configuration, as shown to
 * UI and the tier selector. The API key itself never appears here — only
 * the fact that one is stored.
 */
data class CloudMindConfig(
    val enabled: Boolean,
    val baseUrl: String,
    val model: String,
    val hasKey: Boolean,
) {
    /** Fully configured AND user-enabled — the only state that may speak. */
    val usable: Boolean get() = enabled && hasKey && baseUrl.isNotBlank() && model.isNotBlank()

    companion object {
        val Disabled = CloudMindConfig(enabled = false, baseUrl = "", model = "", hasKey = false)
    }
}

/**
 * Implemented by :core:cloud-mind; consumed by :core:mind through this
 * interface so the tier selector never depends on the networked module.
 */
interface CloudMindBackend : MindEngine {
    val config: kotlinx.coroutines.flow.Flow<CloudMindConfig>

    suspend fun currentConfig(): CloudMindConfig
}

/** Extended inventory view of the tiered engine (Settings → Mind screen). */
interface MindInventory {
    suspend fun snapshot(): MindSnapshot
}
