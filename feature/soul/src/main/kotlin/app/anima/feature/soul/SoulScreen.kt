package app.anima.feature.soul

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import app.anima.core.model.FactCategory
import app.anima.core.ui.components.GhostButton
import app.anima.core.ui.components.PillButton
import app.anima.core.ui.components.SectionCard
import app.anima.core.ui.components.SectionLabel
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
    val screenshotsAllowed by viewModel.screenshotsAllowed.collectAsState()
    val activity = androidx.activity.compose.LocalActivity.current
    androidx.compose.runtime.DisposableEffect(screenshotsAllowed, activity) {
        val window = activity?.window
        if (!screenshotsAllowed) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose { window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
    }

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
            GhostButton("Back", onClick = onBack)
            Text("Soul", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 8.dp))
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            state.stats?.let { stats ->
                item {
                    SectionCard {
                        SectionLabel("Together")
                        Row {
                            Counter("days", stats.daysTogether(System.currentTimeMillis()).toString())
                            Counter("talks", stats.conversationCount.toString())
                            Counter("memories", stats.liveFactCount.toString())
                            Counter("meals", stats.chargeCount.toString())
                        }
                        Row {
                            PillButton("Export the soul", onClick = {
                                viewModel.buildExportIntent { context.startActivity(it) }
                            })
                            GhostButton("Our story…", onClick = onOpenStory)
                            GhostButton("Postcard", onClick = {
                                viewModel.buildPostcardIntent { context.startActivity(it) }
                            })
                        }
                        Text(
                            "The markdown export stays readable by any human or AI. " +
                                "The sealed backup below moves the whole soul between phones.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            item { SectionLabel("What it remembers", Modifier.padding(top = 6.dp)) }

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
                        Text("Search memories…", style = MaterialTheme.typography.bodyMedium)
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
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CategoryChip("all", state.categoryFilter == null) { viewModel.onCategoryFilter(null) }
                    FactCategory.entries.forEach { category ->
                        CategoryChip(categoryName(category), state.categoryFilter == category) {
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
                            if (state.query.isBlank() && state.categoryFilter == null) {
                                "\"My memory is an open field so far. Tell me things — " +
                                    "and tap yes on what I may keep.\""
                            } else {
                                "\"Nothing in my memory matches that. Try another word?\""
                            },
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
                        Text(categoryName(fact.category), style = MaterialTheme.typography.labelMedium)
                        Text(fact.text, style = MaterialTheme.typography.bodyLarge)
                    }
                    GhostButton("Edit", onClick = { viewModel.startEdit(fact) })
                    GhostButton("Forget", onClick = { viewModel.forget(fact) })
                }
            }

            item {
                SectionCard(Modifier.padding(top = 10.dp)) {
                    SectionLabel("Sealed backup — move the soul")
                    Text(
                        "The whole soul (memories with their full history, the body " +
                            "journal, identity) in one encrypted file. Same passphrase " +
                            "opens it on the new phone. Lose the passphrase — lose the copy.",
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
                            Text("Passphrase (min 8 chars)…", style = MaterialTheme.typography.bodyMedium)
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
                            "Seal & save…",
                            enabled = backupPassphrase.length >= 8,
                            onClick = { exportLauncher.launch("anima-soul.backup") },
                        )
                        GhostButton(
                            "Open a backup…",
                            enabled = backupPassphrase.isNotEmpty(),
                            onClick = { importLauncher.launch(arrayOf("*/*")) },
                        )
                    }
                    state.backupNotice?.let { notice ->
                        Text(notice, style = MaterialTheme.typography.bodyLarge)
                        GhostButton("OK", onClick = viewModel::dismissBackupNotice)
                    }
                }
            }

            item {
                SectionCard(Modifier.padding(top = 10.dp)) {
                    SectionLabel("Bring a soul from another AI")
                    Text(
                        "1. Copy the extractor prompt and ask your other assistant.\n" +
                            "2. Paste its answer below.\n" +
                            "3. Confirm each fact — nothing is saved without your yes.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    GhostButton("Copy extractor prompt", onClick = {
                        clipboard.setText(AnnotatedString(viewModel.extractorPrompt()))
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
                            Text("Paste the answer here…", style = MaterialTheme.typography.bodyMedium)
                        }
                        BasicTextField(
                            value = state.importText,
                            onValueChange = viewModel::onImportTextChange,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text),
                            cursorBrush = SolidColor(colors.accent),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    GhostButton(
                        "Find facts",
                        onClick = viewModel::parseImport,
                        enabled = state.importText.isNotBlank(),
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
                        Text(categoryName(candidate.category), style = MaterialTheme.typography.labelMedium)
                        Text(candidate.text, style = MaterialTheme.typography.bodyLarge)
                    }
                    GhostButton("Skip", onClick = { viewModel.rejectImport(candidate) })
                    GhostButton("Keep", onClick = { viewModel.confirmImport(candidate) })
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
            SectionLabel("Correct a memory")
            Text(categoryName(fact.category), style = MaterialTheme.typography.labelMedium)
            Text(
                "The old version isn't erased — it's kept underneath, superseded. " +
                    "The soul's history stays honest.",
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
                GhostButton("Cancel", onClick = viewModel::cancelEdit)
                GhostButton(
                    "Save correction",
                    enabled = state.editDraft.isNotBlank(),
                    onClick = viewModel::confirmEdit,
                )
            }
        }
    }
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

private fun categoryName(category: FactCategory): String =
    when (category) {
        FactCategory.IDENTITY -> "identity"
        FactCategory.PREFERENCE -> "preference"
        FactCategory.PEOPLE -> "people"
        FactCategory.WORK -> "work"
        FactCategory.MOMENT -> "moment"
        FactCategory.OTHER -> "other"
    }
