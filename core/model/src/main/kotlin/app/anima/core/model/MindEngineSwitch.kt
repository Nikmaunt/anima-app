package app.anima.core.model

import kotlinx.coroutines.flow.Flow

/**
 * ADR-020: which implementation serves the LOCAL model tier. The tier ladder
 * (CLOUD → NANO → local model → sleep) never sees this switch — it decides by
 * [MindModelLocator] alone; the switch only picks the runtime that executes
 * the already-selected local tier. Implemented over AnimaPrefs in :core:data;
 * the default is OFF (tasks-genai stays the shipped runtime) and the toggle
 * exists only in debug builds' developer section.
 */
interface MindEngineSwitch {
    /** true = route the local tier through LiteRT-LM (developer flag). */
    val altLocalEngine: Flow<Boolean>
}
