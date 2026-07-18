package app.anima.feature.rest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.anima.core.body.BodySensors
import app.anima.core.data.repo.IdentityRepository
import app.anima.core.data.repo.JournalRepository
import app.anima.core.model.CreatureConcept
import app.anima.core.model.JournalKind
import app.anima.core.model.RestPhase
import app.anima.core.model.RestSessions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RestUiState(
    val concept: CreatureConcept = CreatureConcept.SPIRIT_ORB,
    val seed: Long = 0L,
    val creatureName: String = "",
    val charging: Boolean = false,
    /** Derived, only-growing counters (Finch-ethics: nothing to break). */
    val totalSessions: Int = 0,
    val totalQuietMinutes: Int = 0,
    val weekSessions: Int = 0,
)

@HiltViewModel
class RestViewModel
    @Inject
    constructor(
        val manager: RestSessionManager,
        private val identity: IdentityRepository,
        private val journal: JournalRepository,
        body: BodySensors,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(RestUiState())
        val uiState: StateFlow<RestUiState> = mutableState.asStateFlow()

        val phase: StateFlow<RestPhase> = manager.phase

        val charging: StateFlow<Boolean> =
            body
                .battery()
                .map { it.charging }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

        init {
            viewModelScope.launch {
                mutableState.value =
                    mutableState.value.copy(
                        concept = identity.concept() ?: CreatureConcept.SPIRIT_ORB,
                        seed = identity.seed() ?: 0L,
                        creatureName = identity.name().orEmpty(),
                    )
            }
            refreshTotals()
        }

        /** Called after a completion lands so the totals celebrate quietly. */
        fun refreshTotals() {
            viewModelScope.launch {
                val entries = journal.ofKindSince(JournalKind.REST_SESSION, sinceMillis = 0L)
                val minutes =
                    entries.sumOf { RestSessions.parseDetail(it.detail)?.second ?: 0 }
                val weekAgo = System.currentTimeMillis() - WEEK_MS
                mutableState.value =
                    mutableState.value.copy(
                        totalSessions = entries.size,
                        totalQuietMinutes = minutes,
                        weekSessions = entries.count { it.atMillis >= weekAgo },
                    )
            }
        }

        fun start(plannedMin: Int) = manager.start(plannedMin, System.currentTimeMillis())

        suspend fun tick() {
            val before = manager.phase.value
            manager.tick(System.currentTimeMillis())
            if (before is RestPhase.Running && manager.phase.value is RestPhase.Completed) {
                refreshTotals()
            }
        }

        fun pause() = manager.pause(System.currentTimeMillis())

        fun resume() = manager.resume(System.currentTimeMillis())

        fun reset() = manager.reset()

        private companion object {
            const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
        }
    }
