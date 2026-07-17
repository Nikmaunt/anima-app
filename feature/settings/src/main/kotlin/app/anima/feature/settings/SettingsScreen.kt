package app.anima.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.anima.core.creature.ConceptGallery
import app.anima.core.data.prefs.AnimaPrefs
import app.anima.core.data.repo.IdentityRepository
import app.anima.core.model.CreatureConcept
import app.anima.core.ui.components.GhostButton
import app.anima.core.ui.components.SectionCard
import app.anima.core.ui.components.SectionLabel
import app.anima.core.ui.theme.LocalAnimaColors
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val name: String = "",
    val concept: CreatureConcept? = null,
    val seed: Long = 0L,
    val calmMotion: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val identity: IdentityRepository,
    private val prefs: AnimaPrefs,
) : ViewModel() {

    private val seed = MutableStateFlow(0L)

    val uiState: StateFlow<SettingsUiState> = combine(
        identity.observeName(),
        identity.observeConcept(),
        prefs.calmMotion(),
        seed,
    ) { name, concept, calm, s ->
        SettingsUiState(name.orEmpty(), concept, s, calm)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        viewModelScope.launch { seed.value = identity.seed() ?: 0L }
    }

    fun rename(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { identity.rename(name) }
    }

    fun switchConcept(concept: CreatureConcept) {
        viewModelScope.launch { identity.switchConcept(concept) }
    }

    fun setCalmMotion(value: Boolean) {
        viewModelScope.launch { prefs.setCalmMotion(value) }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenNotifications: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
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
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionCard {
                SectionLabel("Name")
                var draft by androidx.compose.runtime.remember(state.name) {
                    androidx.compose.runtime.mutableStateOf(state.name)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        textStyle = MaterialTheme.typography.headlineSmall.copy(color = colors.text),
                        cursorBrush = SolidColor(colors.accent),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceHigh)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                    GhostButton("Save", onClick = { viewModel.rename(draft) })
                }
            }

            SectionCard {
                SectionLabel("Body")
                Text(
                    "The same soul can live in another body. Live previews — " +
                        "every cell is the real engine.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                ConceptGallery(
                    seed = state.seed,
                    selected = state.concept,
                    onSelect = viewModel::switchConcept,
                    accent = colors.accent,
                    surface = colors.surfaceHigh,
                    outline = colors.outline,
                    modifier = Modifier.height(660.dp),
                )
            }

            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Calm motion", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Still pose, rare blinks. Follows the system " +
                                "reduce-animations setting automatically.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Switch(
                        checked = state.calmMotion,
                        onCheckedChange = viewModel::setCalmMotion,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = colors.accent,
                            checkedThumbColor = colors.background,
                        ),
                    )
                }
            }

            SectionCard {
                SectionLabel("Senses")
                GhostButton("Notification sense…", onClick = onOpenNotifications)
            }

            SectionCard {
                SectionLabel("Privacy, honestly")
                Text(
                    "No internet permission — nothing can leave. No accounts, no " +
                        "analytics, no crash reporting. Memory lives in an encrypted " +
                        "database; its key never leaves this phone's secure hardware. " +
                        "Thinking happens on this device or not at all.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
