package app.anima.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.anima.core.creature.CreatureSurface
import app.anima.core.creature.rememberCreature
import app.anima.core.data.prefs.AnimaPrefs
import app.anima.core.data.repo.IdentityRepository
import app.anima.core.model.CreatureConcept
import app.anima.core.model.Milestones
import app.anima.core.model.PaletteVariant
import app.anima.core.ui.components.GhostButton
import app.anima.core.ui.components.SectionCard
import app.anima.core.ui.components.SectionLabel
import app.anima.core.ui.theme.LocalAnimaColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WardrobeUiState(
    val concept: CreatureConcept = CreatureConcept.SPIRIT_ORB,
    val seed: Long = 0L,
    val selectedWire: String? = null,
    val board: List<Milestones.Unlock> = emptyList(),
)

/**
 * Forms (v0.4 milestones): palette variants on the LIVE rig. Unlocks are
 * pure functions of only-growing counters — nothing here can be bought,
 * expire, or re-lock; locked cards state their condition, never a timer.
 */
@HiltViewModel
class WardrobeViewModel
    @Inject
    constructor(
        private val identity: IdentityRepository,
        private val prefs: AnimaPrefs,
    ) : ViewModel() {
        private val state = MutableStateFlow(WardrobeUiState())
        val uiState: StateFlow<WardrobeUiState> = state.asStateFlow()

        init {
            viewModelScope.launch { refresh() }
            viewModelScope.launch {
                prefs.paletteVariant().collect { wire ->
                    state.value = state.value.copy(selectedWire = wire)
                }
            }
        }

        private suspend fun refresh() {
            val now = System.currentTimeMillis()
            state.value =
                state.value.copy(
                    concept = identity.concept() ?: CreatureConcept.SPIRIT_ORB,
                    seed = identity.seed() ?: 0L,
                    board = Milestones.board(identity.stats(now), now),
                )
        }

        fun select(variant: PaletteVariant) {
            viewModelScope.launch { prefs.setPaletteVariant(variant.wire) }
        }
    }

@Composable
fun WardrobeScreen(
    onBack: () -> Unit,
    viewModel: WardrobeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = LocalAnimaColors.current
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            GhostButton("Back", onClick = onBack)
            Text(
                "Forms",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            "Palettes open as you live together — days, memories, rests. " +
                "Whatever opens stays open, forever.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(vertical = 14.dp),
        ) {
            state.board.forEach { unlock ->
                val selected =
                    PaletteVariant.fromWire(state.selectedWire) == unlock.variant
                SectionCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .alpha(if (unlock.achieved) 1f else 0.45f)
                                .clickable(enabled = unlock.achieved) {
                                    viewModel.select(unlock.variant)
                                },
                    ) {
                        // Live rig preview wearing this palette.
                        val controller = rememberCreature(state.concept, state.seed)
                        LaunchedEffect(unlock.variant) {
                            controller.paletteShiftDeg = unlock.variant.shiftDeg
                        }
                        CreatureSurface(
                            controller = controller,
                            night = colors.isNight,
                            modifier = Modifier.size(88.dp),
                            interactive = false,
                            contentDescription = unlock.variant.label,
                        )
                        Column(Modifier.weight(1f)) {
                            SectionLabel(unlock.variant.label)
                            Text(
                                when {
                                    selected -> "worn now"
                                    unlock.achieved -> "open — tap to wear"
                                    else -> "opens at ${unlock.condition}"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}
