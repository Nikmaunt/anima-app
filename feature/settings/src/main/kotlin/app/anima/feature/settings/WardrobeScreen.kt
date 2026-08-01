package app.anima.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
    /** v1.1b task 1c: null until the identity row is read. */
    val concept: CreatureConcept? = null,
    val seed: Long? = null,
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
                    concept = identity.concept(),
                    seed = identity.seed(),
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
            GhostButton(stringResource(R.string.settings_back), onClick = onBack)
            Text(
                stringResource(R.string.wardrobe_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            stringResource(R.string.wardrobe_intro),
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
                        // Live rig preview wearing this palette. v1.1b task 1c:
                        // these are shifts of the ASSIGNED body, so with no body
                        // known there is nothing to preview — an empty swatch of
                        // the same size, never a different creature in the palette.
                        val wornConcept = state.concept
                        val wornSeed = state.seed
                        if (wornConcept != null && wornSeed != null) {
                            val controller = rememberCreature(wornConcept, wornSeed)
                            LaunchedEffect(unlock.variant) {
                                controller.paletteShiftDeg = unlock.variant.shiftDeg
                            }
                            CreatureSurface(
                                controller = controller,
                                night = colors.isNight,
                                modifier = Modifier.size(88.dp),
                                interactive = false,
                                contentDescription = paletteName(unlock.variant),
                            )
                        } else {
                            Spacer(Modifier.size(88.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            SectionLabel(paletteName(unlock.variant))
                            Text(
                                when {
                                    selected -> stringResource(R.string.wardrobe_worn_now)
                                    unlock.achieved -> stringResource(R.string.wardrobe_open_tap)
                                    else -> stringResource(R.string.wardrobe_opens_at, unlockCondition(unlock))
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

/**
 * v1.1c defect D4. `PaletteVariant.label` and `Unlock.condition` were English
 * literals in `core/model` — a JVM module that has no `res/` and cannot have
 * one. The enum now carries only facts (a wire value, a hue shift, a threshold)
 * and the wording lives here, where the six locales are.
 */
@Composable
private fun paletteName(variant: PaletteVariant): String =
    stringResource(
        when (variant) {
            PaletteVariant.TRUE_SELF -> R.string.palette_true_self
            PaletteVariant.DAWN -> R.string.palette_dawn
            PaletteVariant.AURORA -> R.string.palette_aurora
            PaletteVariant.DEEP_SEA -> R.string.palette_deep_sea
            PaletteVariant.MOONLIT -> R.string.palette_moonlit
            PaletteVariant.EMBERWISE -> R.string.palette_emberwise
        },
    )

@Composable
private fun unlockCondition(unlock: Milestones.Unlock): String =
    when (unlock.kind) {
        Milestones.UnlockKind.ALWAYS -> stringResource(R.string.unlock_always)
        Milestones.UnlockKind.WISE_STAGE -> stringResource(R.string.unlock_wise_stage)
        Milestones.UnlockKind.DAYS_TOGETHER ->
            pluralStringResource(R.plurals.unlock_days_together, unlock.threshold, unlock.threshold)
        Milestones.UnlockKind.REMEMBERED_FACTS ->
            pluralStringResource(R.plurals.unlock_remembered_facts, unlock.threshold, unlock.threshold)
        Milestones.UnlockKind.RESTS_TOGETHER ->
            pluralStringResource(R.plurals.unlock_rests_together, unlock.threshold, unlock.threshold)
    }
