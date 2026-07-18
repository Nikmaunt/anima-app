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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
        val observer =
            LifecycleEventObserver { _, event ->
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
            GhostButton(stringResource(R.string.notif_back), onClick = onBack)
            Text(
                stringResource(R.string.notif_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                SectionCard {
                    // v0.3 quick-win (product-research §2): the creature asks
                    // in its own voice, in context, never during onboarding.
                    Text(stringResource(R.string.notif_ask_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.notif_explainer),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (!state.accessGranted) {
                        Text(
                            stringResource(R.string.notif_decline_hint),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        PillButton(stringResource(R.string.notif_grant_button), onClick = {
                            context.startActivity(viewModel.openAccessSettings())
                        })
                    } else {
                        ToggleRow(
                            label = stringResource(R.string.notif_sense_on),
                            checked = state.enabled,
                            onChange = viewModel::setEnabled,
                        )
                    }
                }
            }

            if (state.accessGranted && state.enabled) {
                item {
                    SectionCard {
                        SectionLabel(stringResource(R.string.notif_allowlist_label))
                        if (state.allowlist.isEmpty()) {
                            Text(
                                stringResource(R.string.notif_allowlist_empty),
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
                                GhostButton(
                                    stringResource(R.string.notif_remove),
                                    onClick = { viewModel.removePackage(pkg) },
                                )
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
                                        stringResource(R.string.notif_package_hint),
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
                                stringResource(R.string.notif_add),
                                onClick = viewModel::addPackage,
                                enabled = state.packageInputValid,
                            )
                        }
                    }
                }
                item {
                    SectionCard {
                        ToggleRow(
                            label = stringResource(R.string.notif_store_text),
                            checked = state.storeText,
                            onChange = viewModel::setStoreText,
                        )
                        Text(
                            stringResource(R.string.notif_store_text_hint),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                item {
                    SectionCard {
                        ToggleRow(
                            label = stringResource(R.string.notif_quiet_hours),
                            checked = state.quietHours,
                            onChange = viewModel::setQuietHours,
                        )
                        Text(
                            stringResource(R.string.notif_quiet_hours_hint),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                item {
                    SectionCard {
                        SectionLabel(stringResource(R.string.notif_today_label))
                        if (state.todayPerApp.isEmpty()) {
                            Text(
                                stringResource(R.string.notif_today_empty),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        state.todayPerApp.entries.take(8).forEach { (pkg, count) ->
                            Row {
                                Text(pkg, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text("$count", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        state.digest?.let {
                            Text(digestText(it), style = MaterialTheme.typography.bodyLarge)
                        }
                        GhostButton(
                            if (state.digestBusy) {
                                stringResource(R.string.notif_digest_busy)
                            } else {
                                stringResource(R.string.notif_digest_button)
                            },
                            onClick = viewModel::buildDigest,
                            enabled = !state.digestBusy,
                        )
                    }
                }
            }
        }
    }
}

/** Maps the closed [DigestResult] set from the view model onto localized text. */
@Composable
private fun digestText(digest: DigestResult): String =
    when (digest) {
        DigestResult.QuietToday -> stringResource(R.string.notif_digest_quiet)
        DigestResult.MindAsleep -> stringResource(R.string.notif_digest_asleep)
        is DigestResult.BusyFallback ->
            pluralStringResource(R.plurals.notif_digest_fallback, digest.count, digest.count)
        is DigestResult.Composed -> digest.text
    }

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val colors = LocalAnimaColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors =
                SwitchDefaults.colors(
                    checkedTrackColor = colors.accent,
                    checkedThumbColor = colors.background,
                ),
        )
    }
}
