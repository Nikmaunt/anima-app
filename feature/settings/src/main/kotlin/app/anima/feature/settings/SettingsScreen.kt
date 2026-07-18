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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import app.anima.core.cloudmind.CloudMindConfigStore
import app.anima.core.creature.ConceptGallery
import app.anima.core.data.prefs.AnimaPrefs
import app.anima.core.data.repo.IdentityRepository
import app.anima.core.model.CloudMindConfig
import app.anima.core.model.CreatureConcept
import app.anima.core.model.Personality
import app.anima.core.ui.components.GhostButton
import app.anima.core.ui.components.SectionCard
import app.anima.core.ui.components.SectionLabel
import app.anima.core.ui.theme.LocalAnimaColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val name: String = "",
    val concept: CreatureConcept? = null,
    val seed: Long = 0L,
    val calmMotion: Boolean = false,
    val personality: Personality = Personality.Default,
    val personalityCustomised: Boolean = false,
    /** ADR-011: always visible — the user must never wonder which mind speaks. */
    val cloud: CloudMindConfig = CloudMindConfig.Disabled,
    /** FLAG_SECURE toggle for the Soul screen (threat-model: shoulder surfing). */
    val soulScreenshotsAllowed: Boolean = false,
)

/** ADR-013: what the voice toggle should honestly display. */
enum class VoiceUiStatus {
    OFF,
    CHECKING,
    READY,
    NO_OFFLINE_VOICE,
    ENGINE_UNAVAILABLE,
}

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val identity: IdentityRepository,
        private val prefs: AnimaPrefs,
        private val cloudStore: CloudMindConfigStore,
        private val voice: app.anima.core.voice.CreatureVoice,
        private val voiceConfig: app.anima.core.voice.VoiceConfigStore,
    ) : ViewModel() {
        /** ADR-013 toggle state; availability re-checked on every enable. */
        val voiceStatus = MutableStateFlow(VoiceUiStatus.OFF)

        val voiceEnabled: StateFlow<Boolean> =
            voiceConfig.enabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

        init {
            viewModelScope.launch {
                if (voiceConfig.enabled.first()) refreshVoiceStatus()
            }
        }

        private suspend fun refreshVoiceStatus() {
            voiceStatus.value = VoiceUiStatus.CHECKING
            voiceStatus.value =
                when (voice.checkAvailability(java.util.Locale.getDefault())) {
                    is app.anima.core.voice.VoiceAvailability.Ready -> VoiceUiStatus.READY
                    is app.anima.core.voice.VoiceAvailability.NoOfflineVoice ->
                        VoiceUiStatus.NO_OFFLINE_VOICE
                    is app.anima.core.voice.VoiceAvailability.EngineUnavailable ->
                        VoiceUiStatus.ENGINE_UNAVAILABLE
                }
        }

        fun setVoiceEnabled(value: Boolean) {
            viewModelScope.launch {
                voiceConfig.setEnabled(value)
                if (value) {
                    refreshVoiceStatus()
                } else {
                    voice.stop()
                    voiceStatus.value = VoiceUiStatus.OFF
                }
            }
        }

        fun ttsSettingsIntent() = voice.ttsSettingsIntent()

        private val seed = MutableStateFlow(0L)

        val uiState: StateFlow<SettingsUiState> =
            combine(
                identity.observeName(),
                identity.observeConcept(),
                combine(prefs.calmMotion(), cloudStore.config, prefs.soulScreenshotsAllowed(), ::Triple),
                prefs.personality(),
                seed,
            ) { name, concept, calmCloudShots, personality, s ->
                val (calm, cloud, shots) = calmCloudShots
                val effectiveConcept = concept ?: CreatureConcept.SPIRIT_ORB
                SettingsUiState(
                    name = name.orEmpty(),
                    concept = concept,
                    seed = s,
                    calmMotion = calm,
                    personality = personality ?: Personality.presetFor(effectiveConcept),
                    personalityCustomised = personality != null,
                    cloud = cloud,
                    soulScreenshotsAllowed = shots,
                )
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

        fun setPersonality(value: Personality) {
            viewModelScope.launch { prefs.setPersonality(value) }
        }

        /** Back to the concept's preset (clears the customisation). */
        fun resetPersonality() {
            viewModelScope.launch { prefs.clearPersonality() }
        }

        /** The always-visible mind switch (ADR-011); setup lives on Mind. */
        fun setCloudEnabled(value: Boolean) {
            viewModelScope.launch { cloudStore.setEnabled(value) }
        }

        fun setSoulScreenshotsAllowed(value: Boolean) {
            viewModelScope.launch { prefs.setSoulScreenshotsAllowed(value) }
        }
    }

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenMind: () -> Unit,
    onOpenTrust: () -> Unit = {},
    onOpenCrashLog: () -> Unit = {},
    onOpenWardrobe: () -> Unit = {},
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
                        modifier =
                            Modifier
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
                // v0.4 milestones: palettes opened by the relationship.
                GhostButton("Forms…", onClick = onOpenWardrobe)
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
                        colors =
                            SwitchDefaults.colors(
                                checkedTrackColor = colors.accent,
                                checkedThumbColor = colors.background,
                            ),
                    )
                }
            }

            SectionCard {
                SectionLabel("Character")
                Text(
                    "Two dials, both honest: they change how it talks AND how it " +
                        "moves (gaze darts, blink pace).",
                    style = MaterialTheme.typography.bodyMedium,
                )
                PersonalitySlider(
                    label = "Gentle ↔ Snarky",
                    value = state.personality.warmth,
                    onChange = { viewModel.setPersonality(state.personality.copy(warmth = it)) },
                )
                PersonalitySlider(
                    label = "Quiet ↔ Chatty",
                    value = state.personality.chattiness,
                    onChange = { viewModel.setPersonality(state.personality.copy(chattiness = it)) },
                )
                if (state.personalityCustomised) {
                    GhostButton("Back to its nature", onClick = viewModel::resetPersonality)
                }
            }

            SectionCard {
                SectionLabel("Senses")
                GhostButton("Notification sense…", onClick = onOpenNotifications)
            }

            SectionCard {
                SectionLabel("Mind")
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (state.cloud.enabled) "Mind: cloud" else "Mind: local",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            when {
                                state.cloud.enabled && state.cloud.usable ->
                                    "Messages go to your provider. Flip off to keep everything on this phone."
                                state.cloud.enabled ->
                                    "Cloud is on but not set up — the local mind keeps speaking."
                                else -> "Everything stays on this phone."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Switch(
                        checked = state.cloud.enabled,
                        onCheckedChange = viewModel::setCloudEnabled,
                        colors =
                            SwitchDefaults.colors(
                                checkedTrackColor = colors.accent,
                                checkedThumbColor = colors.background,
                            ),
                    )
                }
                GhostButton("Mind…", onClick = onOpenMind)
            }

            VoiceCard(viewModel)

            PrivacyCard(cloudEnabled = state.cloud.enabled)

            SectionCard {
                SectionLabel("More")
                GhostButton("Why no internet…", onClick = onOpenTrust)
                GhostButton("Last crash (local only)…", onClick = onOpenCrashLog)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Soul screen screenshots", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Off = the memories screen refuses screenshots and " +
                                "hides itself in the app switcher.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Switch(
                        checked = state.soulScreenshotsAllowed,
                        onCheckedChange = viewModel::setSoulScreenshotsAllowed,
                        colors =
                            SwitchDefaults.colors(
                                checkedTrackColor = colors.accent,
                                checkedThumbColor = colors.background,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalitySlider(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
) {
    val colors = LocalAnimaColors.current
    Column {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = value,
            onValueChange = onChange,
            colors =
                SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.surfaceHigh,
                ),
        )
    }
}

/** ADR-013: offline-verified voices only, default OFF, honest mute state. */
@Composable
private fun VoiceCard(viewModel: SettingsViewModel) {
    val colors = LocalAnimaColors.current
    val enabled by viewModel.voiceEnabled.collectAsState()
    val status by viewModel.voiceStatus.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    SectionCard {
        SectionLabel("Voice")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("The creature speaks", style = MaterialTheme.typography.bodyLarge)
                Text(
                    when (status) {
                        VoiceUiStatus.OFF -> "Off. When on, it uses only voices that work offline."
                        VoiceUiStatus.CHECKING -> "Listening for an offline voice…"
                        VoiceUiStatus.READY ->
                            "Speaks with an offline voice — words never leave the phone."
                        VoiceUiStatus.NO_OFFLINE_VOICE ->
                            "No offline voice for your language is installed, so it stays " +
                                "quiet. Install one in system speech settings."
                        VoiceUiStatus.ENGINE_UNAVAILABLE ->
                            "This phone has no speech engine. The creature can't speak here."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = viewModel::setVoiceEnabled,
                colors =
                    SwitchDefaults.colors(
                        checkedTrackColor = colors.accent,
                        checkedThumbColor = colors.background,
                    ),
            )
        }
        if (status == VoiceUiStatus.NO_OFFLINE_VOICE) {
            GhostButton("Open speech settings", onClick = {
                runCatching { context.startActivity(viewModel.ttsSettingsIntent()) }
            })
        }
    }
}

@Composable
private fun PrivacyCard(cloudEnabled: Boolean) {
    SectionCard {
        SectionLabel("Privacy, honestly")
        Text(
            if (cloudEnabled) {
                "Right now the CLOUD mind is on: your messages, the creature's " +
                    "body report and the memory facts needed for an answer go " +
                    "to the provider you configured. Everything else — the " +
                    "soul database, the notification diary — stays on this " +
                    "phone, encrypted. Flip the mind switch above and Anima " +
                    "is fully offline again."
            } else {
                "The network is used for exactly two things, both under your " +
                    "control: delivering the mind file (Google Play or a " +
                    "download you start) and the optional cloud mind — which " +
                    "is OFF. No accounts, no analytics, no crash reporting; " +
                    "a build-time test fails if any other part of the app " +
                    "touches the network. Memory lives in an encrypted " +
                    "database; its key never leaves this phone's secure " +
                    "hardware. Thinking happens on this device or not at all."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
