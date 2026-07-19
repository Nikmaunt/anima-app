package app.anima.feature.rest

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.anima.core.creature.CreatureSurface
import app.anima.core.creature.rememberCreature
import app.anima.core.model.BodySignals
import app.anima.core.model.BodyState
import app.anima.core.model.CreatureConcept
import app.anima.core.model.Mood
import app.anima.core.model.RestPhase
import app.anima.core.model.RestSessions
import app.anima.core.ui.components.GhostButton
import app.anima.core.ui.theme.LocalAnimaColors
import kotlinx.coroutines.delay

/**
 * Rest together (v0.4 core): pick minutes, dim the world, breathe with the
 * creature. Leaving pauses silently ("I kept your place"); only completed
 * sessions are recorded. The clock is UI-driven — RESUMED only, no timers
 * outside this composition (entity budget).
 */
@Composable
fun RestScreen(
    onBack: () -> Unit,
    viewModel: RestViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState.collectAsState()
    val phase by viewModel.phase.collectAsState()
    val charging by viewModel.charging.collectAsState()
    var keepScreenOn by remember { mutableStateOf(false) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var cameBackFromWait by remember { mutableStateOf(false) }

    // The session clock: beats while THIS screen is composed and RESUMED.
    LaunchedEffect(phase is RestPhase.Running) {
        while (phase is RestPhase.Running) {
            nowMs = System.currentTimeMillis()
            viewModel.tick()
            delay(1_000L)
        }
    }

    // Guilt-free pause: ON_STOP freezes, ON_RESUME quietly continues.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> viewModel.pause()
                    Lifecycle.Event.ON_RESUME -> {
                        if (viewModel.manager.phase.value is RestPhase.Waiting) {
                            cameBackFromWait = true
                            viewModel.resume()
                        }
                    }
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.pause()
        }
    }

    // Dim the window during the session; restore on leave. Optional
    // keep-screen-on so the phone can lie face-up without dying dark.
    val view = LocalView.current
    val running = phase is RestPhase.Running
    DisposableEffect(running, keepScreenOn) {
        val window = (view.context as? Activity)?.window
        val previous = window?.attributes?.screenBrightness
        if (running) {
            window?.attributes = window.attributes.apply { screenBrightness = DIMMED_BRIGHTNESS }
            view.keepScreenOn = keepScreenOn
        }
        onDispose {
            window?.attributes =
                window.attributes.apply {
                    screenBrightness = previous ?: -1f
                }
            view.keepScreenOn = false
        }
    }

    val paletteShift by viewModel.paletteShift.collectAsState()
    RestContent(
        ui = ui,
        phase = phase,
        nowMs = nowMs,
        charging = charging,
        paletteShiftDeg = paletteShift,
        keepScreenOn = keepScreenOn,
        cameBackFromWait = cameBackFromWait,
        onPick = { viewModel.start(it) },
        onKeepScreenOn = { keepScreenOn = it },
        onDone = {
            viewModel.reset()
            onBack()
        },
        onBack = {
            viewModel.reset()
            onBack()
        },
    )
}

/**
 * Stateless content — goldenable by construction (Phase 0.4 DoD).
 * [frameLoop] exists for the golden test: a composable that requests a
 * frame every frame never reaches quiescence under ComposeTestRule (the
 * v0.2 lesson recorded in CreatureSurfaceUiTest).
 */
@Composable
fun RestContent(
    ui: RestUiState,
    phase: RestPhase,
    nowMs: Long,
    charging: Boolean,
    keepScreenOn: Boolean,
    cameBackFromWait: Boolean,
    onPick: (Int) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    frameLoop: Boolean = true,
    paletteShiftDeg: Float = 0f,
) {
    val colors = LocalAnimaColors.current
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth()) {
                GhostButton(
                    stringResource(R.string.rest_back),
                    onClick = onBack,
                    modifier = Modifier.testTag("rest.back"),
                )
            }
            Spacer(Modifier.height(8.dp))

            val controller = rememberCreature(ui.concept, ui.seed)
            controller.paletteShiftDeg = paletteShiftDeg
            val restingMood = if (phase is RestPhase.Running) Mood.ASLEEP else Mood.ALERT
            LaunchedEffect(restingMood, charging) {
                controller.setBodyState(
                    BodyState(BodySignals.Resting.copy(charging = charging), restingMood),
                )
            }
            CreatureSurface(
                controller = controller,
                night = phase is RestPhase.Running,
                modifier = Modifier.size(220.dp),
                interactive = false,
                contentDescription = stringResource(R.string.rest_a11y_creature),
                runFrameLoop = frameLoop,
            )
            Spacer(Modifier.height(24.dp))

            when (phase) {
                is RestPhase.Idle, is RestPhase.Waiting ->
                    PickPanel(ui, charging, onPick)
                is RestPhase.Running -> {
                    val remaining = phase.remainingMs(nowMs).coerceAtLeast(0)
                    val mm = remaining / 60_000
                    val ss = remaining % 60_000 / 1_000
                    Text(
                        "%d:%02d".format(java.util.Locale.US, mm, ss),
                        style = MaterialTheme.typography.displayMedium,
                        color = colors.text,
                        modifier = Modifier.testTag("rest.timer"),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when {
                            cameBackFromWait -> stringResource(R.string.rest_status_returned)
                            charging -> stringResource(R.string.rest_status_eating)
                            else -> stringResource(R.string.rest_status_together)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textDim,
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            stringResource(R.string.rest_keep_screen),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textDim,
                        )
                        Switch(
                            checked = keepScreenOn,
                            onCheckedChange = onKeepScreenOn,
                            colors =
                                SwitchDefaults.colors(
                                    checkedTrackColor = colors.accent,
                                    checkedThumbColor = colors.background,
                                ),
                        )
                    }
                }
                is RestPhase.Completed -> {
                    Text(
                        pluralStringResource(R.plurals.rest_completed_title, phase.plannedMin, phase.plannedMin),
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.text,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(
                            R.string.rest_completed_growth,
                            pluralStringResource(R.plurals.rest_completed_sessions, ui.totalSessions, ui.totalSessions),
                            pluralStringResource(
                                R.plurals.rest_completed_minutes,
                                ui.totalQuietMinutes,
                                ui.totalQuietMinutes,
                            ),
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textDim,
                    )
                    Spacer(Modifier.height(24.dp))
                    GhostButton(stringResource(R.string.rest_thanks), onClick = onDone)
                }
            }
        }
    }
}

@Composable
private fun PickPanel(
    ui: RestUiState,
    charging: Boolean,
    onPick: (Int) -> Unit,
) {
    val colors = LocalAnimaColors.current
    Text(
        if (charging) {
            stringResource(R.string.rest_pick_title_eating)
        } else {
            stringResource(R.string.rest_pick_title)
        },
        style = MaterialTheme.typography.headlineSmall,
        color = colors.text,
    )
    Spacer(Modifier.height(4.dp))
    if (ui.totalSessions > 0) {
        Text(
            stringResource(
                R.string.rest_pick_week,
                pluralStringResource(R.plurals.rest_pick_sessions, ui.totalSessions, ui.totalSessions),
                ui.weekSessions,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textDim,
        )
    }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RestSessions.DURATIONS_MIN.forEach { minutes ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceHigh)
                    .clickable { onPick(minutes) }
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .testTag("rest.start.$minutes"),
            ) {
                Text(
                    stringResource(R.string.rest_minutes_chip, minutes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.text,
                )
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Text(
        stringResource(R.string.rest_leave_hint),
        style = MaterialTheme.typography.bodySmall,
        color = colors.textDim,
    )
}

private const val DIMMED_BRIGHTNESS = 0.25f
