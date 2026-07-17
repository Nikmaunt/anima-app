package app.anima.feature.soul

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.anima.core.model.FactCategory
import app.anima.core.ui.components.GhostButton
import app.anima.core.ui.components.PillButton
import app.anima.core.ui.components.SectionCard
import app.anima.core.ui.components.SectionLabel
import app.anima.core.ui.theme.LocalAnimaColors

/** The soul: counters, remembered facts, one-file export, confirmed import. */
@Composable
fun SoulScreen(
    onBack: () -> Unit,
    viewModel: SoulViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = LocalAnimaColors.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

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
                        PillButton("Export the soul", onClick = {
                            viewModel.buildExportIntent { context.startActivity(it) }
                        })
                        Text(
                            "One readable markdown file. This is the only way anything " +
                                "ever leaves this phone.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            item { SectionLabel("What it remembers", Modifier.padding(top = 6.dp)) }
            if (state.facts.isEmpty()) {
                item {
                    Text(
                        "Nothing yet. Talk to it — and confirm what it may keep.",
                        style = MaterialTheme.typography.bodyMedium,
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
                    GhostButton("Forget", onClick = { viewModel.forget(fact) })
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
}

@Composable
private fun Counter(label: String, value: String) {
    Column(Modifier.padding(end = 22.dp)) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

private fun categoryName(category: FactCategory): String = when (category) {
    FactCategory.IDENTITY -> "identity"
    FactCategory.PREFERENCE -> "preference"
    FactCategory.PEOPLE -> "people"
    FactCategory.WORK -> "work"
    FactCategory.MOMENT -> "moment"
    FactCategory.OTHER -> "other"
}
