package app.anima.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.anima.core.ui.components.GhostButton
import app.anima.core.ui.components.PillButton
import app.anima.core.ui.components.SectionCard
import app.anima.core.ui.components.SectionLabel
import app.anima.core.ui.theme.LocalAnimaColors

/**
 * The notification sense — strict OPT-IN with an honest explainer. Content
 * of notifications is never a command; it becomes the creature's "hearing":
 * storm → anxiety, counters, and an on-demand daily digest.
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = LocalAnimaColors.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Re-check system grant every time the user returns from settings.
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
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
            Text(
                "Notification sense",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                SectionCard {
                    Text("What this is", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "If you allow it, the creature can feel your notifications: " +
                            "a storm of them makes it anxious, and it can tell you what " +
                            "the day sounded like.\n\n" +
                            "Honestly: only apps YOU allowlist are heard; login codes and " +
                            "anything OTP-shaped are dropped before being stored; message " +
                            "text is stored only if you switch that on separately; " +
                            "everything stays in the encrypted database on this phone and " +
                            "is erased after 7 days. This app cannot send anything " +
                            "anywhere — it has no network access at all.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (!state.accessGranted) {
                        PillButton("Allow on the system screen", onClick = {
                            context.startActivity(viewModel.openAccessSettings())
                        })
                    } else {
                        ToggleRow(
                            label = "Sense is on",
                            checked = state.enabled,
                            onChange = viewModel::setEnabled,
                        )
                    }
                }
            }

            if (state.accessGranted && state.enabled) {
                item {
                    SectionCard {
                        SectionLabel("Allowed apps")
                        if (state.allowlist.isEmpty()) {
                            Text(
                                "No apps allowed yet — the creature hears nothing.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        state.allowlist.forEach { pkg ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    pkg,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                GhostButton("Remove", onClick = { viewModel.removePackage(pkg) })
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.surfaceHigh)
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                            ) {
                                if (state.packageInput.isEmpty()) {
                                    Text(
                                        "package.name.here",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                BasicTextField(
                                    value = state.packageInput,
                                    onValueChange = viewModel::onPackageInput,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text),
                                    cursorBrush = SolidColor(colors.accent),
                                )
                            }
                            GhostButton(
                                "Add",
                                onClick = viewModel::addPackage,
                                enabled = state.packageInputValid,
                            )
                        }
                    }
                }
                item {
                    SectionCard {
                        ToggleRow(
                            label = "Also store notification text",
                            checked = state.storeText,
                            onChange = viewModel::setStoreText,
                        )
                        Text(
                            "Off = only app, time and title are kept.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                item {
                    SectionCard {
                        SectionLabel("Today")
                        if (state.todayPerApp.isEmpty()) {
                            Text("Nothing heard today.", style = MaterialTheme.typography.bodyMedium)
                        }
                        state.todayPerApp.entries.take(8).forEach { (pkg, count) ->
                            Row {
                                Text(pkg, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text("$count", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        state.digest?.let {
                            Text(it, style = MaterialTheme.typography.bodyLarge)
                        }
                        GhostButton(
                            if (state.digestBusy) "Listening back…" else "What did today sound like?",
                            onClick = viewModel::buildDigest,
                            enabled = !state.digestBusy,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = LocalAnimaColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colors.accent,
                checkedThumbColor = colors.background,
            ),
        )
    }
}
