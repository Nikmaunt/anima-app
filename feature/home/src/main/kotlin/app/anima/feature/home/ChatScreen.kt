package app.anima.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import app.anima.core.model.CreatureGenome
import app.anima.core.model.MindStatus
import app.anima.core.model.tunedBy
import app.anima.core.ui.components.EmptyState
import app.anima.core.ui.components.GhostButton
import app.anima.core.ui.components.PillButton
import app.anima.core.ui.theme.AnimaSpacing
import app.anima.core.ui.theme.LocalAnimaColors

/**
 * v1.1: chat gets its own route instead of a slot on Home.
 *
 * It is reachable only from Settings, only while the experimental toggle is
 * on, and that toggle is off on a fresh install. Nothing here was deleted —
 * the panel is the same one Home used to host — it is demoted.
 *
 * **The toggle must not dead-end.** Turning chat on when this phone has no
 * local mind used to land the user in a text box that could never answer.
 * Now that case is a first-class state: the creature says plainly that its
 * mind is asleep and offers the one thing that changes it, a route to the
 * Mind screen. Covered by ChatRouteTest.
 */
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    onOpenMind: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = LocalAnimaColors.current
    val haptics = LocalHapticFeedback.current

    // The creature is not drawn here — this screen is the one place in the
    // app that is about words, and the body stays on Home where it is the
    // hero. v1.1b: a `remember { CreatureGenome.from(state.seed) }` stood here
    // with a comment saying voice tuning needed it; the value was discarded and
    // the voice is tuned in the view model, so the seed's nullability exposed it
    // as dead. Removed rather than made null-safe.

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = AnimaSpacing.m, vertical = AnimaSpacing.s),
            verticalArrangement = Arrangement.spacedBy(AnimaSpacing.xs),
        ) {
            GhostButton(
                stringResource(R.string.chat_back),
                onClick = onBack,
                modifier = Modifier.testTag("chat.back"),
            )
            Text(stringResource(R.string.chat_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.chat_experimental_note), style = MaterialTheme.typography.bodyMedium)
        }

        if (state.mindStatus != MindStatus.READY) {
            // Not an error screen and not an empty box: the honest state,
            // with the one step that changes it.
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(AnimaSpacing.m),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EmptyState(
                    voice = stringResource(R.string.chat_no_mind_voice),
                    action = {
                        PillButton(
                            stringResource(R.string.chat_no_mind_action),
                            onClick = onOpenMind,
                            modifier = Modifier.testTag("chat.openMind"),
                        )
                    },
                    modifier = Modifier.testTag("chat.noMind"),
                )
            }
            return@Column
        }

        ChatPanel(
            state = state,
            onSend = { text ->
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                viewModel.send(text)
            },
            onTyping = { active -> if (active) viewModel.onInteraction() },
            onRequestDownload = viewModel::requestMindDownload,
            onOpenMind = onOpenMind,
            onRegenerate = viewModel::regenerate,
            onRememberThis = { message ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.rememberThis(message)
            },
            onReportReply = { message -> viewModel.reportReply(message.id) },
            modifier = Modifier.weight(1f),
        )
    }
}
