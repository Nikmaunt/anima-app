package app.anima.feature.soul

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import app.anima.core.model.FactCategory
import app.anima.core.ui.components.ActionRow
import app.anima.core.ui.components.EmptyState
import app.anima.core.ui.components.GhostButton
import app.anima.core.ui.components.PillButton
import app.anima.core.ui.components.ScrollableChipRow
import app.anima.core.ui.components.SectionCard
import app.anima.core.ui.components.SectionLabel
import app.anima.core.ui.components.SecureWhile
import app.anima.core.ui.components.StatRow
import app.anima.core.ui.theme.LocalAnimaColors

/** The soul: counters, memory browser, edits, encrypted migration, story. */
@Composable
fun SoulScreen(
    onBack: () -> Unit,
    onOpenStory: () -> Unit,
    viewModel: SoulViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = LocalAnimaColors.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    // Threat-model.md: the memory list is the most shoulder-surfable and
    // screenshot-leakable surface. FLAG_SECURE while this screen shows,
    // unless the user flipped the Settings toggle. Cleared on leave.
    // v0.6: shared implementation (SecureWhile) — capsules use it too.
    val screenshotsAllowed by viewModel.screenshotsAllowed.collectAsState()
    SecureWhile(!screenshotsAllowed)

    var backupPassphrase by remember { mutableStateOf("") }
    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream"),
        ) { uri -> uri?.let { viewModel.exportBackup(it, backupPassphrase) } }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.importBackup(it, backupPassphrase) }
        }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            GhostButton(
                stringResource(R.string.soul_back),
                onClick = onBack,
                modifier = Modifier.testTag("soul.back"),
            )
            Text(
                stringResource(R.string.soul_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.testTag("soul.list"),
        ) {
            state.stats?.let { stats ->
                item {
                    SectionCard {
                        SectionLabel(stringResource(R.string.soul_together_label))
                        val days = stats.daysTogether(System.currentTimeMillis()).toInt()
                        // v1.1c defect D9: on the first day this row was four
                        // zeros under four captions — a scoreboard reading nil,
                        // which is the least welcoming thing a relationship
                        // screen can open with. Nothing lived yet is not a
                        // score of nothing; it is a state, and it gets a state.
                        val nothingYet =
                            days == 0 &&
                                stats.conversationCount == 0 &&
                                stats.liveFactCount == 0 &&
                                stats.chargeCount == 0
                        if (nothingYet) {
                            EmptyState(
                                voice = stringResource(R.string.soul_together_empty),
                                modifier = Modifier.testTag("soul.together.empty"),
                            )
                        } else {
                            // v1.1c: StatRow, not Row. A plain Row divides the
                            // width evenly and lets the last caption break —
                            // "кормёжек" came out as "кормёж / ек" and its
                            // baseline fell out of line with the other three.
                            // Defect class D1, still alive on this screen after
                            // the layout primitives were built to end it.
                            StatRow {
                                Counter(
                                    pluralStringResource(R.plurals.soul_counter_days, days),
                                    days.toString(),
                                )
                                Counter(
                                    pluralStringResource(R.plurals.soul_counter_talks, stats.conversationCount),
                                    stats.conversationCount.toString(),
                                )
                                Counter(
                                    pluralStringResource(R.plurals.soul_counter_memories, stats.liveFactCount),
                                    stats.liveFactCount.toString(),
                                )
                                Counter(
                                    pluralStringResource(R.plurals.soul_counter_meals, stats.chargeCount),
                                    stats.chargeCount.toString(),
                                )
                            }
                        }
                        // v1.1c: ActionRow wraps to a second line instead of
                        // squeezing the third button to one character wide.
                        // "Открытка" was rendering as eight stacked letters —
                        // the loudest thing on the screen and unmistakably a
                        // layout falling over. Found by an outside reviewer
                        // looking at the screenshot, which is why that review
                        // exists.
                        ActionRow {
                            PillButton(stringResource(R.string.soul_export_button), onClick = {
                                viewModel.buildExportIntent { context.startActivity(it) }
                            })
                            GhostButton(stringResource(R.string.soul_story_button), onClick = onOpenStory)
                            GhostButton(stringResource(R.string.soul_postcard_button), onClick = {
                                viewModel.buildPostcardIntent { context.startActivity(it) }
                            })
                        }
                        Text(
                            stringResource(R.string.soul_export_hint),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            item { SectionLabel(stringResource(R.string.soul_memories_label), Modifier.padding(top = 6.dp)) }

            item {
                // Memory browser (v2): search + category filter.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surfaceHigh)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    if (state.query.isEmpty()) {
                        Text(stringResource(R.string.soul_search_hint), style = MaterialTheme.typography.bodyMedium)
                    }
                    BasicTextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text),
                        cursorBrush = SolidColor(colors.accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                // v1.1c defect D2: the same fade as Rest, for the same reason —
                // a row cut clean through a chip reads as broken, not as more.
                ScrollableChipRow(spacing = 8.dp) {
                    CategoryChip(stringResource(R.string.soul_category_all), state.categoryFilter == null) {
                        viewModel.onCategoryFilter(null)
                    }
                    FactCategory.entries.forEach { category ->
                        CategoryChip(stringResource(categoryNameRes(category)), state.categoryFilter == category) {
                            viewModel.onCategoryFilter(category)
                        }
                    }
                }
            }
            if (state.facts.isEmpty()) {
                item {
                    // v0.3: empty states keep the creature in the room.
                    app.anima.core.creature.CreatureEmptyState(
                        concept = state.concept,
                        seed = state.seed,
                        line =
                            stringResource(
                                if (state.query.isBlank() && state.categoryFilter == null) {
                                    R.string.soul_empty_field
                                } else {
                                    R.string.soul_empty_no_match
                                },
                            ),
                        night = colors.isNight,
                    )
                }
            }
            items(state.facts, key = { it.id }) { fact ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(categoryNameRes(fact.category)),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(fact.text, style = MaterialTheme.typography.bodyLarge)
                    }
                    GhostButton(stringResource(R.string.soul_edit_button), onClick = { viewModel.startEdit(fact) })
                    GhostButton(stringResource(R.string.soul_forget_button), onClick = { viewModel.forget(fact) })
                }
            }

            item {
                SectionCard(Modifier.padding(top = 10.dp)) {
                    SectionLabel(stringResource(R.string.soul_backup_label))
                    Text(
                        stringResource(R.string.soul_backup_hint),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surfaceHigh)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        if (backupPassphrase.isEmpty()) {
                            Text(
                                stringResource(R.string.soul_backup_passphrase_hint),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        BasicTextField(
                            value = backupPassphrase,
                            onValueChange = { backupPassphrase = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text),
                            cursorBrush = SolidColor(colors.accent),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Row {
                        GhostButton(
                            stringResource(R.string.soul_backup_seal_button),
                            enabled = backupPassphrase.length >= 8,
                            onClick = { exportLauncher.launch("anima-soul.backup") },
                        )
                        GhostButton(
                            stringResource(R.string.soul_backup_open_button),
                            enabled = backupPassphrase.isNotEmpty(),
                            onClick = { importLauncher.launch(arrayOf("*/*")) },
                        )
                    }
                    state.backupNotice?.let { notice ->
                        Text(backupNoticeText(notice), style = MaterialTheme.typography.bodyLarge)
                        GhostButton(stringResource(R.string.soul_ok), onClick = viewModel::dismissBackupNotice)
                    }
                }
            }

            item {
                SectionCard(Modifier.padding(top = 10.dp)) {
                    SectionLabel(stringResource(R.string.soul_import_label))
                    Text(
                        stringResource(R.string.soul_import_steps),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    // v0.6: the prompt is a localized resource — the other
                    // AI is asked in the user's language, but the response
                    // headings are pinned to English for the parser.
                    val extractorPrompt = stringResource(R.string.soul_import_extractor_prompt)
                    GhostButton(stringResource(R.string.soul_import_copy_button), onClick = {
                        clipboard.setText(AnnotatedString(extractorPrompt))
                    })
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 96.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surfaceHigh)
                            .padding(12.dp),
                    ) {
                        if (state.importText.isEmpty()) {
                            Text(
                                stringResource(R.string.soul_import_paste_hint),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        BasicTextField(
                            value = state.importText,
                            onValueChange = viewModel::onImportTextChange,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text),
                            cursorBrush = SolidColor(colors.accent),
                            modifier = Modifier.fillMaxWidth().testTag("soul.import.input"),
                        )
                    }
                    GhostButton(
                        stringResource(R.string.soul_import_find_button),
                        onClick = viewModel::parseImport,
                        enabled = state.importText.isNotBlank(),
                        modifier = Modifier.testTag("soul.import.find"),
                    )
                }
            }

            items(state.importCandidates, key = { it.text }) { candidate ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.accentSoft)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(categoryNameRes(candidate.category)),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(candidate.text, style = MaterialTheme.typography.bodyLarge)
                    }
                    GhostButton(
                        stringResource(R.string.soul_import_skip_button),
                        onClick = { viewModel.rejectImport(candidate) },
                    )
                    GhostButton(
                        stringResource(R.string.soul_import_keep_button),
                        onClick = { viewModel.confirmImport(candidate) },
                        modifier = Modifier.testTag("soul.import.keep"),
                    )
                }
            }
        }
    }

    EditFactDialog(state = state, viewModel = viewModel)
}

@Composable
private fun EditFactDialog(
    state: SoulUiState,
    viewModel: SoulViewModel,
) {
    val colors = LocalAnimaColors.current
    val fact = state.editing ?: return
    Dialog(onDismissRequest = viewModel::cancelEdit) {
        SectionCard {
            SectionLabel(stringResource(R.string.soul_edit_title))
            Text(stringResource(categoryNameRes(fact.category)), style = MaterialTheme.typography.labelMedium)
            Text(
                stringResource(R.string.soul_edit_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceHigh)
                    .padding(12.dp),
            ) {
                BasicTextField(
                    value = state.editDraft,
                    onValueChange = viewModel::onEditDraftChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row {
                GhostButton(stringResource(R.string.soul_edit_cancel), onClick = viewModel::cancelEdit)
                GhostButton(
                    stringResource(R.string.soul_edit_save),
                    enabled = state.editDraft.isNotBlank(),
                    onClick = viewModel::confirmEdit,
                )
            }
        }
    }
}

/** Maps the closed [BackupNotice] set from the view model onto localized text. */
@Composable
private fun backupNoticeText(notice: BackupNotice): String =
    when (notice) {
        is BackupNotice.PassphraseTooShort ->
            stringResource(R.string.soul_notice_passphrase_short, notice.minChars)
        BackupNotice.ExportDone -> stringResource(R.string.soul_notice_sealed)
        is BackupNotice.ExportFailed ->
            stringResource(
                R.string.soul_notice_export_failed,
                notice.detail ?: stringResource(R.string.soul_error_unknown),
            )
        is BackupNotice.ImportDone -> {
            val arrived =
                pluralStringResource(
                    R.plurals.soul_notice_imported,
                    notice.imported,
                    notice.creatureName,
                    notice.imported,
                )
            if (notice.skipped > 0) {
                arrived + " " + pluralStringResource(R.plurals.soul_notice_skipped, notice.skipped, notice.skipped)
            } else {
                arrived
            }
        }
        BackupNotice.WrongPassphraseOrCorrupt -> stringResource(R.string.soul_notice_wrong_passphrase)
        BackupNotice.NoBodyYet -> stringResource(R.string.soul_notice_no_body_yet)
        BackupNotice.UnknownBodyInFile -> stringResource(R.string.soul_notice_unknown_body)
        is BackupNotice.ImportFailed ->
            stringResource(
                R.string.soul_notice_import_failed,
                notice.detail ?: stringResource(R.string.soul_error_unknown),
            )
    }

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAnimaColors.current
    Text(
        label,
        style = MaterialTheme.typography.bodyMedium,
        color = if (selected) colors.background else colors.text,
        modifier =
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) colors.accent else colors.surface)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@Composable
private fun Counter(
    label: String,
    value: String,
) {
    Column(Modifier.padding(end = 22.dp)) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@StringRes
private fun categoryNameRes(category: FactCategory): Int =
    when (category) {
        FactCategory.IDENTITY -> R.string.soul_category_identity
        FactCategory.PREFERENCE -> R.string.soul_category_preference
        FactCategory.PEOPLE -> R.string.soul_category_people
        FactCategory.WORK -> R.string.soul_category_work
        FactCategory.MOMENT -> R.string.soul_category_moment
        FactCategory.OTHER -> R.string.soul_category_other
    }
