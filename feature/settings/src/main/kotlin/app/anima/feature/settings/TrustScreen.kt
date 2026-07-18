package app.anima.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.anima.core.ui.components.GhostButton
import app.anima.core.ui.components.SectionCard
import app.anima.core.ui.components.SectionLabel
import app.anima.core.ui.theme.LocalAnimaColors

/**
 * "Why Anima doesn't need the internet" — privacy as a product surface
 * (product-research §4): ownership, incapability-not-promise, blunt
 * negations, architecture-as-guarantee, an airplane-mode proof, and an
 * honesty section for the two things that DO use the network.
 */
@Composable
fun TrustScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = LocalAnimaColors.current
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
                "Why no internet",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionCard {
                Text(
                    "Your creature lives here. Only here.",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    "Anima runs entirely on this phone. No account, no cloud " +
                        "by default, no one watching.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            SectionCard {
                Text("No account. There's nothing to sign up for.", style = MaterialTheme.typography.bodyLarge)
                Text("No servers of ours. Conversations never reach us.", style = MaterialTheme.typography.bodyLarge)
                Text("No trackers. No ads, no analytics, no kidding.", style = MaterialTheme.typography.bodyLarge)
            }
            SectionCard {
                SectionLabel("We can't read your conversations")
                Text(
                    "Not because we promise not to — because they never reach " +
                        "us. There is nothing on our side to read. The mind is " +
                        "a language model running on this phone's own chip; " +
                        "everything it learns lives in an encrypted database " +
                        "whose key never leaves this phone's secure hardware.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            SectionCard {
                SectionLabel("Prove it")
                Text(
                    "Turn on airplane mode. Everything keeps working — the " +
                        "creature senses the connection change like weather, " +
                        "and nothing else happens. No feature needs the network.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            SectionCard {
                SectionLabel("What DOES use the network — the whole list")
                Text(
                    "1. Delivering the mind file: Google Play brings it with " +
                        "the app, or you start a download yourself on the Mind " +
                        "screen. Model bytes come in; nothing about you goes " +
                        "out.\n\n" +
                        "2. The cloud mind — only if YOU turn it on, with your " +
                        "own provider and key. " +
                        (
                            if (state.cloud.enabled) {
                                "It is ON right now: messages and the facts " +
                                    "needed for an answer go to your provider."
                            } else {
                                "It is OFF right now."
                            }
                        ) +
                        "\n\nA build-time test fails if any other part of the " +
                        "app so much as mentions a network API.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            SectionCard {
                SectionLabel("Your data, your call")
                Text(
                    "Forget a memory and it's marked dead in the soul (the " +
                        "creature stops using it). Export the soul to one file " +
                        "you own. Delete Anima and everything goes with it — " +
                        "there is no copy anywhere else.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
