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
import androidx.compose.ui.res.stringResource
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
            GhostButton(stringResource(R.string.settings_back), onClick = onBack)
            Text(
                stringResource(R.string.trust_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionCard {
                Text(
                    stringResource(R.string.trust_hero_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    stringResource(R.string.trust_hero_body),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            SectionCard {
                Text(stringResource(R.string.trust_no_account), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.trust_no_servers), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.trust_no_trackers), style = MaterialTheme.typography.bodyLarge)
            }
            SectionCard {
                SectionLabel(stringResource(R.string.trust_cant_read_label))
                Text(
                    stringResource(R.string.trust_cant_read_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            SectionCard {
                SectionLabel(stringResource(R.string.trust_prove_label))
                Text(
                    stringResource(R.string.trust_prove_body),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            SectionCard {
                SectionLabel(stringResource(R.string.trust_network_label))
                Text(
                    stringResource(
                        R.string.trust_network_body,
                        stringResource(
                            if (state.cloud.enabled) {
                                R.string.trust_cloud_on_now
                            } else {
                                R.string.trust_cloud_off_now
                            },
                        ),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            SectionCard {
                SectionLabel(stringResource(R.string.trust_yourdata_label))
                Text(
                    stringResource(R.string.trust_yourdata_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
