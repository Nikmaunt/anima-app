package app.anima.feature.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.anima.core.creature.CreatureSurface
import app.anima.core.creature.rememberCreature
import app.anima.core.model.BodyState
import app.anima.core.model.GenomeFingerprint
import app.anima.core.model.IdentityOrigin
import app.anima.core.ui.components.GhostButton
import app.anima.core.ui.components.LabeledControl
import app.anima.core.ui.components.SectionCard
import app.anima.core.ui.components.SectionLabel
import app.anima.core.ui.theme.LocalAnimaColors
import java.text.DateFormat
import java.util.Date

/**
 * v1.1c task 5.2 — the creature passport, which replaces the body grid.
 *
 * What it replaces was a gallery of all eight bodies in which **one tap rewrote
 * the body, with no confirmation and no undo**, on a phone where a single lived
 * soul exists. It also contradicted the product outright: a catalogue says the
 * body is merchandise, and the body is not chosen.
 *
 * Everything here is read-only except the name, so nothing on the page implies
 * the creature is a configuration — and the one live thing on it is the
 * creature itself, drawn by the real engine rather than by a picture of it.
 */
@Composable
fun PassportScreen(
    onBack: () -> Unit,
    onOpenSoul: () -> Unit,
    onOpenPalettes: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = LocalAnimaColors.current

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .wrapContentWidth()
            .widthIn(max = 720.dp)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
            .testTag("passport"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            GhostButton(stringResource(R.string.settings_back), onClick = onBack)
            Text(
                stringResource(R.string.passport_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        // The live portrait. Same engine as Home. Until identity is read this is
        // an empty box of exactly the same size — the v1.1b rule everywhere in
        // this app: nobody, never somebody else.
        Box(
            Modifier
                .fillMaxWidth()
                .height(PORTRAIT_HEIGHT)
                .testTag("passport.portrait"),
            contentAlignment = Alignment.Center,
        ) {
            val concept = state.concept
            val seed = state.seed
            if (concept != null && seed != null) {
                val controller = rememberCreature(concept, seed)
                controller.setBodyState(BodyState.Resting)
                CreatureSurface(
                    controller = controller,
                    night = false,
                    interactive = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionCard {
                SectionLabel(stringResource(R.string.passport_name_label))
                var draft by remember(state.name) { mutableStateOf(state.name) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(color = colors.text),
                        cursorBrush = SolidColor(colors.accent),
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surfaceHigh)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("passport.name.input"),
                    )
                    GhostButton(
                        stringResource(R.string.settings_save),
                        onClick = { viewModel.rename(draft) },
                        modifier = Modifier.testTag("passport.name.save"),
                    )
                }
            }

            SectionCard {
                SectionLabel(stringResource(R.string.passport_facts_label))
                val unknown = stringResource(R.string.passport_unknown)
                LabeledControl(
                    title = stringResource(R.string.passport_hatched_at),
                    supporting = null,
                ) {
                    Text(
                        state.hatchedAtMillis
                            ?.let { DateFormat.getDateInstance(DateFormat.LONG).format(Date(it)) }
                            ?: unknown,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                LabeledControl(
                    title = stringResource(R.string.passport_fingerprint),
                    supporting = stringResource(R.string.passport_fingerprint_hint),
                ) {
                    Text(
                        state.seed?.let(GenomeFingerprint::of) ?: unknown,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag("passport.fingerprint"),
                    )
                }
                LabeledControl(
                    title = stringResource(R.string.passport_device),
                    supporting = null,
                ) {
                    Text(
                        when (state.origin) {
                            // A transferred soul's body belongs to the phone it
                            // was born on, and nothing here knows that phone's
                            // name. Saying "this one" would be a convenient lie.
                            IdentityOrigin.TRANSFERRED ->
                                stringResource(R.string.passport_device_transferred)
                            else -> "${Build.MANUFACTURER} ${Build.MODEL}"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Text(
                    // Verbatim, by the owner's instruction. It is the product in
                    // one sentence and it is not to be paraphrased.
                    stringResource(R.string.passport_body_belongs),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp).testTag("passport.belongs"),
                )
            }

            SectionCard {
                SectionLabel(stringResource(R.string.passport_palettes_label))
                Text(
                    stringResource(R.string.passport_palettes_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
                GhostButton(
                    stringResource(R.string.palettes_title),
                    onClick = onOpenPalettes,
                    modifier = Modifier.testTag("passport.palettes"),
                )
            }

            SectionCard {
                SectionLabel(stringResource(R.string.passport_transfer_label))
                Text(
                    stringResource(R.string.passport_transfer_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
                GhostButton(
                    stringResource(R.string.passport_transfer_open),
                    onClick = onOpenSoul,
                    modifier = Modifier.testTag("passport.soul"),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private val PORTRAIT_HEIGHT = 300.dp
