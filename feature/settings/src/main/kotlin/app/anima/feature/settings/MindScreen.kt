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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.anima.core.cloudmind.KeyProbeResult
import app.anima.core.model.CloudMindConfig
import app.anima.core.model.CloudPresets
import app.anima.core.model.InstalledMindModel
import app.anima.core.model.MindModelSpec
import app.anima.core.model.MindStatus
import app.anima.core.model.MindTier
import app.anima.core.model.ModelLicense
import app.anima.core.modeldelivery.DeliveryFailure
import app.anima.core.modeldelivery.PackPhase
import app.anima.core.ui.components.GhostButton
import app.anima.core.ui.components.SectionCard
import app.anima.core.ui.components.SectionLabel
import app.anima.core.ui.theme.LocalAnimaColors

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
            GhostButton(stringResource(R.string.settings_back), onClick = onBack)
            Text(
                stringResource(R.string.mind_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionCard {
                SectionLabel(stringResource(R.string.mind_now_label))
                Text(
                    when (state.tier) {
                        MindTier.CLOUD ->
                            stringResource(
                                R.string.mind_now_cloud,
                                state.cloud.host(stringResource(R.string.mind_provider_fallback)),
                            )
                        MindTier.NANO -> stringResource(R.string.mind_now_nano)
                        MindTier.GEMMA ->
                            if (state.model?.fromPack == true) {
                                stringResource(R.string.mind_now_gemma_pack)
                            } else {
                                stringResource(R.string.mind_now_gemma_file)
                            }
                        MindTier.NONE -> stringResource(R.string.mind_now_none)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                // Phase 1D honesty row: never pretend a language is spoken.
                val routing = state.routing
                if (routing != null && state.tier != MindTier.NONE) {
                    if (routing.showBadge) {
                        Text(
                            stringResource(
                                R.string.mind_language_english_only,
                                state.uiLanguage.selfName,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.warn,
                        )
                    } else {
                        Text(
                            stringResource(R.string.mind_language_native, routing.language.selfName),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                if (state.nano == MindStatus.DOWNLOADABLE || state.nano == MindStatus.DOWNLOADING) {
                    Text(
                        stringResource(R.string.mind_nano_fetchable),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            PackCard(state, onFetch = viewModel::requestPackFetch, onForceGemma = viewModel::setForceGemma)

            SectionCard {
                SectionLabel(stringResource(R.string.mind_installed_label))
                if (state.models.isEmpty()) {
                    Text(
                        stringResource(R.string.mind_installed_empty),
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
                            stringResource(R.string.mind_auto_button),
                            onClick = { viewModel.selectModel(null) },
                            enabled = state.selected != null,
                            modifier = Modifier.testTag("mind.model.auto"),
                        )
                        Text(
                            if (state.selected == null) {
                                stringResource(R.string.mind_auto_on)
                            } else {
                                stringResource(R.string.mind_auto_off)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Text(
                    stringResource(R.string.mind_free_space, state.freeBytes / GB),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (state.busy) {
                SectionCard {
                    SectionLabel(
                        if (state.importing) {
                            stringResource(R.string.mind_progress_import_label)
                        } else {
                            stringResource(R.string.mind_progress_download_label)
                        },
                    )
                    val progress = state.progress
                    if (progress?.second != null) {
                        LinearProgressIndicator(
                            progress = { (progress.first.toFloat() / progress.second!!).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.accent,
                        )
                        Text(
                            stringResource(R.string.mind_progress_mb, progress.first / MB, progress.second!! / MB),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = colors.accent)
                    }
                    Text(
                        stringResource(R.string.mind_keep_open),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                // v0.5: both delivery paths stay open — new files land NEXT
                // to installed minds (commit never deletes neighbors).
                SectionCard {
                    SectionLabel(stringResource(R.string.mind_bring_label))
                    Text(
                        stringResource(R.string.mind_bring_hint),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    GhostButton(stringResource(R.string.mind_choose_file), onClick = { picker.launch(arrayOf("*/*")) })
                }

                SectionCard {
                    SectionLabel(stringResource(R.string.mind_direct_link_label))
                    MindTextField(
                        value = state.urlDraft,
                        onValueChange = viewModel::onUrlChange,
                        placeholder = stringResource(R.string.mind_url_placeholder),
                    )
                    MindTextField(
                        value = state.shaDraft,
                        onValueChange = viewModel::onShaChange,
                        placeholder = stringResource(R.string.mind_sha_placeholder),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.mind_metered_title),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                // v0.7 (audit-v06 DEGRADED №1): registry-fed
                                // size — copy can never go stale again.
                                stringResource(R.string.mind_metered_hint, packDefaultSize()),
                                style = MaterialTheme.typography.bodyMedium,
                            )
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
                    GhostButton(
                        stringResource(R.string.mind_download_button, packDefaultSize()),
                        onClick = viewModel::download,
                    )
                }
            }

            CloudCard(state, viewModel)

            state.notice?.let { notice ->
                SectionCard {
                    SectionLabel(stringResource(R.string.mind_notice_label))
                    Text(noticeText(notice), style = MaterialTheme.typography.bodyLarge)
                    GhostButton(stringResource(R.string.mind_notice_ok), onClick = viewModel::dismissNotice)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** v0.5 l10n: the closed [MindNotice] set mapped to resources, VM stays text-free. */
@Composable
private fun noticeText(notice: MindNotice): String =
    when (notice) {
        MindNotice.MindArrived -> stringResource(R.string.mind_notice_arrived)
        MindNotice.ModelDeleted -> stringResource(R.string.mind_notice_deleted)
        MindNotice.PasteLinkFirst -> stringResource(R.string.mind_notice_paste_link)
        MindNotice.HttpsRequired -> stringResource(R.string.mind_notice_https_required)
        MindNotice.KeyErased -> stringResource(R.string.mind_notice_key_erased)
        is MindNotice.KeyProbe ->
            when (notice.result) {
                KeyProbeResult.OK -> stringResource(R.string.mind_notice_key_ok)
                KeyProbeResult.BAD_KEY -> stringResource(R.string.mind_notice_key_bad)
                KeyProbeResult.UNREACHABLE -> stringResource(R.string.mind_notice_key_unreachable)
                KeyProbeResult.NOT_CONFIGURED -> stringResource(R.string.mind_notice_key_unconfigured)
            }
        is MindNotice.DeliveryFailed ->
            when (notice.reason) {
                DeliveryFailure.NEEDS_WIFI -> stringResource(R.string.mind_failure_needs_wifi)
                DeliveryFailure.NOT_HTTPS -> stringResource(R.string.mind_failure_not_https)
                DeliveryFailure.HTTP_ERROR ->
                    stringResource(
                        R.string.mind_failure_http,
                        notice.detail ?: stringResource(R.string.mind_failure_http_no_detail),
                    )
                DeliveryFailure.INTERRUPTED -> stringResource(R.string.mind_failure_interrupted)
                DeliveryFailure.CHECKSUM_MISMATCH ->
                    stringResource(R.string.mind_failure_checksum, notice.detail ?: "?")
                DeliveryFailure.NOT_A_MODEL -> stringResource(R.string.mind_failure_not_model)
                DeliveryFailure.NO_SPACE -> stringResource(R.string.mind_failure_no_space)
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
                SectionLabel(stringResource(R.string.mind_pack_incoming_label))
                if (phase.bytesTotal > 0) {
                    LinearProgressIndicator(
                        progress = { (phase.bytesDone.toFloat() / phase.bytesTotal).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.accent,
                    )
                    Text(
                        stringResource(
                            R.string.mind_pack_progress,
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
                SectionLabel(stringResource(R.string.mind_pack_packed_label))
                Text(
                    stringResource(R.string.mind_pack_packed_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
                GhostButton(stringResource(R.string.mind_pack_fetch), onClick = onFetch)
            }
        }
        is PackPhase.WaitingForConsent -> {
            SectionCard {
                SectionLabel(stringResource(R.string.mind_pack_consent_label))
                Text(
                    stringResource(R.string.mind_pack_consent_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
                GhostButton(stringResource(R.string.mind_pack_fetch), onClick = onFetch)
            }
        }
        is PackPhase.Ready -> {
            if (!state.ramGateAllows) {
                SectionCard {
                    SectionLabel(stringResource(R.string.mind_ram_label))
                    Text(
                        stringResource(R.string.mind_ram_hint),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.mind_ram_try),
                                style = MaterialTheme.typography.bodyLarge,
                            )
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
                    stringResource(R.string.mind_model_active),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accent,
                )
            }
        }
        Text(
            stringResource(
                R.string.mind_model_meta,
                model.sizeBytes / MB,
                spec.languages.joinToString(" ") { it.selfName },
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            listOfNotNull(
                licenseShort(spec.license),
                if (model.fromPack) stringResource(R.string.mind_model_from_pack) else null,
                if (spec.displayName != model.fileName) model.fileName else null,
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textDim,
        )
        if (onDelete != null) {
            GhostButton(stringResource(R.string.mind_model_delete), onClick = onDelete)
        }
    }
}

/** License names are proper nouns and stay untranslated; only UNKNOWN localizes. */
@Composable
private fun licenseShort(license: ModelLicense): String =
    when (license) {
        ModelLicense.APACHE_2 -> "Apache-2.0"
        ModelLicense.GEMMA_TOU -> "Gemma ToU"
        ModelLicense.UNKNOWN -> stringResource(R.string.mind_license_unknown)
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
        SectionLabel(stringResource(R.string.mind_cloud_label))
        Text(
            stringResource(
                R.string.mind_cloud_intro,
                state.cloud.host(stringResource(R.string.mind_provider_fallback)),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
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
                if (state.cloud.enabled && !state.cloud.usable) {
                    Text(
                        stringResource(R.string.mind_cloud_unconfigured),
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
                        stringResource(R.string.mind_cloud_key_prefix, prefix),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textDim,
                    )
                }
                if (matched.freeTier) {
                    Text(
                        stringResource(R.string.mind_cloud_free_tier),
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
            placeholder = stringResource(R.string.mind_cloud_url_placeholder),
        )
        MindTextField(
            value = state.cloudModelDraft,
            onValueChange = viewModel::onCloudModelChange,
            placeholder = stringResource(R.string.mind_cloud_model_placeholder),
        )
        MindTextField(
            value = keyDraft,
            onValueChange = { keyDraft = it },
            placeholder =
                if (state.cloud.hasKey) {
                    stringResource(R.string.mind_cloud_key_placeholder_stored)
                } else {
                    stringResource(R.string.mind_cloud_key_placeholder)
                },
            // audit-v03 F5: the key is a credential — masked while typed,
            // password keyboard (no learning/autofill suggestion cache).
            secret = true,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            GhostButton(stringResource(R.string.settings_save), onClick = {
                viewModel.saveCloudSetup(keyDraft)
                keyDraft = ""
            })
            if (state.cloud.hasKey) {
                Spacer(Modifier.width(8.dp))
                GhostButton(stringResource(R.string.mind_cloud_forget_key), onClick = viewModel::forgetCloudKey)
            }
            Spacer(Modifier.width(8.dp))
            GhostButton(
                stringResource(R.string.mind_cloud_check_key),
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
            stringResource(R.string.mind_cloud_key_note),
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

private fun CloudMindConfig.host(fallback: String): String =
    baseUrl
        .removePrefix("https://")
        .substringBefore('/')
        .ifBlank { fallback }

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

/**
 * Registry-default artifact size for copy (audit-v06 DEGRADED №1). Same
 * Formatter as the home banner, so both screens quote the same number.
 */
@Composable
private fun packDefaultSize(): String =
    android.text.format.Formatter.formatShortFileSize(
        androidx.compose.ui.platform.LocalContext.current,
        app.anima.core.model.MindModelRegistry.packDefault.approxSizeBytes,
    )

private const val MB = 1024.0 * 1024.0
private const val GB = MB * 1024.0
