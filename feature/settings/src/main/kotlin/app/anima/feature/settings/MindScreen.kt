package app.anima.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.anima.core.model.CloudMindConfig
import app.anima.core.model.CloudPresets
import app.anima.core.model.InstalledMindModel
import app.anima.core.model.MindModelSpec
import app.anima.core.model.MindStatus
import app.anima.core.model.MindTier
import app.anima.core.model.ModelLicense
import app.anima.core.modeldelivery.PackPhase
import app.anima.core.ui.components.GhostButton
import app.anima.core.ui.components.SectionCard
import app.anima.core.ui.components.SectionLabel
import app.anima.core.ui.theme.LocalAnimaColors
import java.util.Locale

/**
 * Settings → Mind. Honest inventory of every tier (ADR-005) plus the two
 * delivery paths for the GEMMA tier: bring-the-file (SAF, primary) and
 * download-by-URL (the user's own mirror or tokenized link — their license
 * acceptance). Download runs only while this screen keeps the app foreground.
 */
@Composable
fun MindScreen(
    onBack: () -> Unit,
    viewModel: MindViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = LocalAnimaColors.current
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let(viewModel::importModel)
        }

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
                "Mind",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionCard {
                SectionLabel("Right now")
                Text(
                    when (state.tier) {
                        MindTier.CLOUD ->
                            "Thinking with YOUR cloud provider (${state.cloud.host()}). " +
                                "In this mode messages and the facts needed for an answer " +
                                "leave this phone."
                        MindTier.NANO -> "Thinking with this phone's built-in mind (Gemini Nano)."
                        MindTier.GEMMA ->
                            if (state.model?.fromPack == true) {
                                "Thinking with the bundled mind (Gemma, delivered with the " +
                                    "app, fully on this phone)."
                            } else {
                                "Thinking with the downloaded mind (Gemma, fully on this phone)."
                            }
                        MindTier.NONE ->
                            "The mind sleeps. This phone has no built-in mind for apps, " +
                                "and no mind file is installed yet. Everything else about " +
                                "the creature still works."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                // Phase 1D honesty row: never pretend a language is spoken.
                val routing = state.routing
                if (routing != null && state.tier != MindTier.NONE) {
                    if (routing.showBadge) {
                        Text(
                            "Answers in English — this mind doesn't speak " +
                                "${state.uiLanguage.selfName}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.warn,
                        )
                    } else {
                        Text(
                            "Speaks ${routing.language.selfName} natively.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                if (state.nano == MindStatus.DOWNLOADABLE || state.nano == MindStatus.DOWNLOADING) {
                    Text(
                        "Built-in mind: the system can fetch it from the home screen chat.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            PackCard(state, onFetch = viewModel::requestPackFetch, onForceGemma = viewModel::setForceGemma)

            SectionCard {
                SectionLabel("Installed minds")
                if (state.models.isEmpty()) {
                    Text(
                        "No mind file installed. A local mind (hundreds of MB, once) " +
                            "lets the creature talk entirely on this phone. Bring a " +
                            "file below, or download one by direct link.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    state.models.forEach { (model, spec) ->
                        MindModelRow(
                            model = model,
                            spec = spec,
                            active = model.fileName == state.model?.fileName,
                            onSelect = { viewModel.selectModel(model.fileName) },
                            onDelete =
                                if (model.fromPack) {
                                    null
                                } else {
                                    { viewModel.deleteModel(model.fileName) }
                                },
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GhostButton(
                            "Auto",
                            onClick = { viewModel.selectModel(null) },
                            enabled = state.selected != null,
                            modifier = Modifier.testTag("mind.model.auto"),
                        )
                        Text(
                            if (state.selected == null) {
                                "Picking by itself — your files first, then the pack."
                            } else {
                                "Your pick stays until you press Auto."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Text(
                    "%.1f GB free on this phone".format(Locale.US, state.freeBytes / GB),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (state.busy) {
                SectionCard {
                    SectionLabel(if (state.importing) "Bringing the mind in…" else "Downloading the mind…")
                    val progress = state.progress
                    if (progress?.second != null) {
                        LinearProgressIndicator(
                            progress = { (progress.first.toFloat() / progress.second!!).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.accent,
                        )
                        Text(
                            "%.0f / %.0f MB".format(Locale.US, progress.first / MB, progress.second!! / MB),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = colors.accent)
                    }
                    Text(
                        "Keep this screen open — the creature carries no background " +
                            "services, so leaving pauses the transfer (it resumes later).",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                // v0.5: both delivery paths stay open — new files land NEXT
                // to installed minds (commit never deletes neighbors).
                SectionCard {
                    SectionLabel("Bring the file")
                    Text(
                        "Pick a downloaded .task or .litertlm file. It is copied into " +
                            "the creature's private storage and verified.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    GhostButton("Choose mind file…", onClick = { picker.launch(arrayOf("*/*")) })
                }

                SectionCard {
                    SectionLabel("Or download by direct link")
                    MindTextField(
                        value = state.urlDraft,
                        onValueChange = viewModel::onUrlChange,
                        placeholder = "https://… (.task / .litertlm)",
                    )
                    MindTextField(
                        value = state.shaDraft,
                        onValueChange = viewModel::onShaChange,
                        placeholder = "SHA-256 (optional, verifies the file)",
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("Allow mobile data", style = MaterialTheme.typography.bodyLarge)
                            Text("Off = Wi-Fi only (it's ~530 MB)", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = state.allowMetered,
                            onCheckedChange = viewModel::onAllowMeteredChange,
                            colors =
                                SwitchDefaults.colors(
                                    checkedTrackColor = colors.accent,
                                    checkedThumbColor = colors.background,
                                ),
                        )
                    }
                    GhostButton("Download (~530 MB, once)", onClick = viewModel::download)
                }
            }

            CloudCard(state, viewModel)

            state.notice?.let { notice ->
                SectionCard {
                    SectionLabel("What happened")
                    Text(notice, style = MaterialTheme.typography.bodyLarge)
                    GhostButton("OK", onClick = viewModel::dismissNotice)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** ADR-010: what Play's mind pack is doing right now, honestly. */
@Composable
private fun PackCard(
    state: MindUiState,
    onFetch: () -> Unit,
    onForceGemma: (Boolean) -> Unit,
) {
    val colors = LocalAnimaColors.current
    when (val phase = state.packPhase) {
        is PackPhase.Downloading -> {
            SectionCard {
                SectionLabel("The mind is on its way")
                if (phase.bytesTotal > 0) {
                    LinearProgressIndicator(
                        progress = { (phase.bytesDone.toFloat() / phase.bytesTotal).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.accent,
                    )
                    Text(
                        "%.0f / %.0f MB — Google Play delivers it by itself; no need to wait here.".format(
                            Locale.US,
                            phase.bytesDone / MB,
                            phase.bytesTotal / MB,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = colors.accent)
                }
            }
        }
        is PackPhase.NotFetched -> {
            SectionCard {
                SectionLabel("A mind is packed with the app")
                Text(
                    "Google Play carries a mind for this creature but hasn't " +
                        "delivered it yet. Ask, and it comes on its own.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                GhostButton("Let it come", onClick = onFetch)
            }
        }
        is PackPhase.WaitingForConsent -> {
            SectionCard {
                SectionLabel("The mind waits for permission")
                Text(
                    "Google Play holds the ~660 MB mind until you allow the download " +
                        "(it prefers Wi-Fi).",
                    style = MaterialTheme.typography.bodyMedium,
                )
                GhostButton("Let it come", onClick = onFetch)
            }
        }
        is PackPhase.Ready -> {
            if (!state.ramGateAllows) {
                SectionCard {
                    SectionLabel("A caution about this phone")
                    Text(
                        "The bundled mind is here, but this phone has little memory " +
                            "(under ~6 GB). Running Gemma may make everything slow. " +
                            "You can try anyway, or use the cloud mind below.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("Try anyway", style = MaterialTheme.typography.bodyLarge)
                        }
                        Switch(
                            checked = state.forceGemma,
                            onCheckedChange = onForceGemma,
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
        is PackPhase.Failed, PackPhase.Absent -> Unit // The manual paths below stay.
    }
}

/** ADR-017: one installed mind — spec-labeled, tappable, honestly licensed. */
@Composable
private fun MindModelRow(
    model: InstalledMindModel,
    spec: MindModelSpec,
    active: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val colors = LocalAnimaColors.current
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceHigh)
            .border(1.dp, if (active) colors.accent else colors.outline.copy(alpha = 0.5f), shape)
            .clickable(onClick = onSelect)
            .testTag("mind.model.${model.fileName}")
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                spec.displayName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            if (active) {
                Text(
                    "Active",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accent,
                )
            }
        }
        Text(
            "%.0f MB · %s".format(
                Locale.US,
                model.sizeBytes / MB,
                spec.languages.joinToString(" ") { it.selfName },
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            listOfNotNull(
                licenseShort(spec.license),
                if (model.fromPack) "from pack" else null,
                if (spec.displayName != model.fileName) model.fileName else null,
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textDim,
        )
        if (onDelete != null) {
            GhostButton("Delete", onClick = onDelete)
        }
    }
}

private fun licenseShort(license: ModelLicense): String =
    when (license) {
        ModelLicense.APACHE_2 -> "Apache-2.0"
        ModelLicense.GEMMA_TOU -> "Gemma ToU"
        ModelLicense.UNKNOWN -> "unknown license"
    }

/** ADR-011: the optional user-keyed cloud mind — plain words, no dark corners. */
@Composable
private fun CloudCard(
    state: MindUiState,
    viewModel: MindViewModel,
) {
    val colors = LocalAnimaColors.current
    var keyDraft by remember { mutableStateOf("") }
    SectionCard {
        SectionLabel("Cloud mind (optional)")
        Text(
            "Off by default. If you turn it on, the creature thinks with an AI " +
                "provider YOU choose, using YOUR key. In that mode every message " +
                "you send — plus the creature's memory facts needed for the " +
                "answer and its body report — leaves this phone and goes to " +
                "${state.cloud.host()}. Nothing else does. The soul stays here.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (state.cloud.enabled) "Mind: cloud" else "Mind: local",
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (state.cloud.enabled && !state.cloud.usable) {
                    Text(
                        "Enabled but not configured — the local mind keeps speaking.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
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
        // Phase 1E: presets are convenience, not endorsement — a tap only
        // fills the fields; the owner still brings the key and saves.
        val matched = CloudPresets.match(state.cloudUrlDraft)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            CloudPresets.all.forEach { preset ->
                PresetChip(
                    text = preset.displayName,
                    selected = matched?.id == preset.id,
                    onClick = { viewModel.applyPreset(preset) },
                    modifier = Modifier.testTag("mind.preset.${preset.id}"),
                )
            }
        }
        if (matched != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                matched.keyPrefix?.let { prefix ->
                    Text(
                        "Keys usually start with $prefix…",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textDim,
                    )
                }
                if (matched.freeTier) {
                    Text(
                        "free tier",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.accent,
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(colors.accentSoft)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
        MindTextField(
            value = state.cloudUrlDraft,
            onValueChange = viewModel::onCloudUrlChange,
            placeholder = "https://api.your-provider.com/v1",
        )
        MindTextField(
            value = state.cloudModelDraft,
            onValueChange = viewModel::onCloudModelChange,
            placeholder = "model name (e.g. gpt-4.1-mini)",
        )
        MindTextField(
            value = keyDraft,
            onValueChange = { keyDraft = it },
            placeholder = if (state.cloud.hasKey) "API key (stored — paste to replace)" else "API key",
            // audit-v03 F5: the key is a credential — masked while typed,
            // password keyboard (no learning/autofill suggestion cache).
            secret = true,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            GhostButton("Save", onClick = {
                viewModel.saveCloudSetup(keyDraft)
                keyDraft = ""
            })
            if (state.cloud.hasKey) {
                Spacer(Modifier.width(8.dp))
                GhostButton("Forget key", onClick = viewModel::forgetCloudKey)
            }
            Spacer(Modifier.width(8.dp))
            GhostButton(
                "Check key",
                onClick = viewModel::checkKey,
                enabled = !state.checkingKey,
                modifier = Modifier.testTag("mind.checkKey"),
            )
            if (state.checkingKey) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = colors.accent,
                    strokeWidth = 2.dp,
                )
            }
        }
        Text(
            "The key is encrypted with this phone's hardware keystore, never " +
                "logged, and never part of the soul export. Offline? The " +
                "creature falls back to its local mind by itself.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Phase 1E: one provider preset chip; selected = drafts match its URL. */
@Composable
private fun PresetChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAnimaColors.current
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) colors.accent else colors.text,
        modifier =
            modifier
                .clip(RoundedCornerShape(50))
                .background(if (selected) colors.accentSoft else colors.surfaceHigh)
                .border(
                    1.dp,
                    if (selected) colors.accent else colors.outline.copy(alpha = 0.5f),
                    RoundedCornerShape(50),
                ).clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

private fun CloudMindConfig.host(): String =
    baseUrl
        .removePrefix("https://")
        .substringBefore('/')
        .ifBlank { "your provider" }

@Composable
private fun MindTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    secret: Boolean = false,
) {
    val colors = LocalAnimaColors.current
    Column(Modifier.padding(vertical = 4.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.text),
            cursorBrush = SolidColor(colors.accent),
            visualTransformation =
                if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions =
                if (secret) {
                    KeyboardOptions(keyboardType = KeyboardType.Password)
                } else {
                    KeyboardOptions.Default
                },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceHigh)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
        )
        if (value.isEmpty()) {
            Text(
                placeholder,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 14.dp, top = 2.dp),
            )
        }
    }
}

private const val MB = 1024.0 * 1024.0
private const val GB = MB * 1024.0
