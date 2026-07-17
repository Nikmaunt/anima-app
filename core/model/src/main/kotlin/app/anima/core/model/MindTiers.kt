package app.anima.core.model

import kotlinx.coroutines.flow.StateFlow

/**
 * ADR-005: which backend is speaking. NANO = ML Kit Prompt API via AICore
 * (system-managed). GEMMA = local Gemma 3 1B through MediaPipe, model file
 * owned by :core:model-delivery. NONE = the mind sleeps; never faked.
 */
enum class MindTier { NANO, GEMMA, NONE }

/** A validated local model file for the GEMMA tier. */
data class InstalledMindModel(
    val fileName: String,
    val sizeBytes: Long,
    val path: String,
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
)

/** Extended inventory view of the tiered engine (Settings → Mind screen). */
interface MindInventory {
    suspend fun snapshot(): MindSnapshot
}
