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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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

        /** ADR-020 developer flag; the section renders in debug builds only. */
        val litertlmEngine: StateFlow<Boolean> =
            prefs.litertlmEngine().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

        fun setLitertlmEngine(value: Boolean) {
            viewModelScope.launch { prefs.setLitertlmEngine(value) }
        }

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
    onOpenLicenses: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = LocalAnimaColors.current

    // v0.6 tablets/folds: settings reads as a capped column, not a banner.
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .wrapContentWidth()
            .widthIn(max = 720.dp)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            GhostButton(stringResource(R.string.settings_back), onClick = onBack)
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionCard {
                SectionLabel(stringResource(R.string.settings_name_label))
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
                    GhostButton(stringResource(R.string.settings_save), onClick = { viewModel.rename(draft) })
                }
            }

            SectionCard {
                SectionLabel(stringResource(R.string.settings_body_label))
                Text(
                    stringResource(R.string.settings_body_hint),
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
                GhostButton(stringResource(R.string.settings_forms_button), onClick = onOpenWardrobe)
            }

            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.settings_calm_motion_title),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            stringResource(R.string.settings_calm_motion_hint),
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
                SectionLabel(stringResource(R.string.settings_character_label))
                Text(
                    stringResource(R.string.settings_character_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
                PersonalitySlider(
                    label = stringResource(R.string.settings_character_warmth),
                    value = state.personality.warmth,
                    onChange = { viewModel.setPersonality(state.personality.copy(warmth = it)) },
                )
                PersonalitySlider(
                    label = stringResource(R.string.settings_character_chattiness),
                    value = state.personality.chattiness,
                    onChange = { viewModel.setPersonality(state.personality.copy(chattiness = it)) },
                )
                if (state.personalityCustomised) {
                    GhostButton(
                        stringResource(R.string.settings_character_reset),
                        onClick = viewModel::resetPersonality,
                    )
                }
            }

            SectionCard {
                SectionLabel(stringResource(R.string.settings_senses_label))
                GhostButton(stringResource(R.string.settings_notification_sense), onClick = onOpenNotifications)
            }

            SectionCard {
                SectionLabel(stringResource(R.string.settings_mind_label))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (state.cloud.enabled) {
                                stringResource(R.string.mind_state_cloud)
                            } else {
                                stringResource(R.string.mind_state_local)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            when {
                                state.cloud.enabled && state.cloud.usable ->
                                    stringResource(R.string.settings_mind_desc_cloud)
                                state.cloud.enabled ->
                                    stringResource(R.string.settings_mind_desc_cloud_unconfigured)
                                else -> stringResource(R.string.settings_mind_desc_local)
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
                GhostButton(stringResource(R.string.settings_mind_open), onClick = onOpenMind)
            }

            VoiceCard(viewModel)

            PrivacyCard(cloudEnabled = state.cloud.enabled)

            SectionCard {
                SectionLabel(stringResource(R.string.settings_more_label))
                GhostButton(stringResource(R.string.settings_trust_open), onClick = onOpenTrust)
                GhostButton(stringResource(R.string.settings_crashlog_open), onClick = onOpenCrashLog)
                // v0.6: OSS attribution (release engineering; license duty).
                GhostButton(
                    stringResource(R.string.settings_licenses_open),
                    onClick = onOpenLicenses,
                    modifier = Modifier.testTag("settings.licenses"),
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.settings_soul_screenshots_title),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            stringResource(R.string.settings_soul_screenshots_hint),
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

            // ADR-020: developer section — debug builds only. Release UI has
            // no toggle AND release DI has no engine (double gate).
            if (BuildConfig.DEBUG) {
                val litertlm by viewModel.litertlmEngine.collectAsState()
                SectionCard {
                    SectionLabel(stringResource(R.string.settings_developer_label))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_dev_litertlm_title),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                stringResource(R.string.settings_dev_litertlm_hint),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Switch(
                            checked = litertlm,
                            onCheckedChange = viewModel::setLitertlmEngine,
                            modifier = Modifier.testTag("settings.dev.litertlm"),
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
        SectionLabel(stringResource(R.string.settings_voice_label))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_voice_title),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    when (status) {
                        VoiceUiStatus.OFF -> stringResource(R.string.settings_voice_off)
                        VoiceUiStatus.CHECKING -> stringResource(R.string.settings_voice_checking)
                        VoiceUiStatus.READY -> stringResource(R.string.settings_voice_ready)
                        VoiceUiStatus.NO_OFFLINE_VOICE ->
                            stringResource(R.string.settings_voice_no_offline)
                        VoiceUiStatus.ENGINE_UNAVAILABLE ->
                            stringResource(R.string.settings_voice_no_engine)
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
            GhostButton(stringResource(R.string.settings_voice_open_settings), onClick = {
                runCatching { context.startActivity(viewModel.ttsSettingsIntent()) }
            })
        }
    }
}

@Composable
private fun PrivacyCard(cloudEnabled: Boolean) {
    SectionCard {
        SectionLabel(stringResource(R.string.settings_privacy_label))
        Text(
            if (cloudEnabled) {
                stringResource(R.string.settings_privacy_cloud_on)
            } else {
                stringResource(R.string.settings_privacy_local)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
