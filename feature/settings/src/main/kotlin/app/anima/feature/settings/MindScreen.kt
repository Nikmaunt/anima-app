package app.anima.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.anima.core.model.MindStatus
import app.anima.core.model.MindTier
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
                        MindTier.NANO -> "Thinking with this phone's built-in mind (Gemini Nano)."
                        MindTier.GEMMA -> "Thinking with the downloaded mind (Gemma, fully on this phone)."
                        MindTier.NONE ->
                            "The mind sleeps. This phone has no built-in mind for apps, " +
                                "and no mind file is installed yet. Everything else about " +
                                "the creature still works."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (state.nano == MindStatus.DOWNLOADABLE || state.nano == MindStatus.DOWNLOADING) {
                    Text(
                        "Built-in mind: the system can fetch it from the home screen chat.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            SectionCard {
                SectionLabel("Mind file")
                val model = state.model
                if (model != null) {
                    Text(model.fileName, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "%.0f MB on disk · %.1f GB free".format(
                            Locale.US,
                            model.sizeBytes / MB,
                            state.freeBytes / GB,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(6.dp))
                    GhostButton("Delete the mind file", onClick = viewModel::deleteModel)
                } else {
                    Text(
                        "No mind file installed. Gemma 3 1B (~530 MB, once) lets the " +
                            "creature talk entirely on this phone. Google gates the " +
                            "official file behind a license page, so the honest path is: " +
                            "download it in your browser, then bring it here.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "%.1f GB free on this phone".format(Locale.US, state.freeBytes / GB),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
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
            } else if (state.model == null) {
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

@Composable
private fun MindTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val colors = LocalAnimaColors.current
    Column(Modifier.padding(vertical = 4.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.text),
            cursorBrush = SolidColor(colors.accent),
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
