package app.anima.feature.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.anima.core.creature.CreatureSurface
import app.anima.core.creature.rememberCreature
import app.anima.core.model.BodyState
import app.anima.core.model.CreatureConcept
import app.anima.core.ui.components.GhostButton
import app.anima.core.ui.components.PillButton
import app.anima.core.ui.theme.LocalAnimaColors
import kotlin.math.sin

/**
 * v1.1c: hatch → the body and the name appear → the widget.
 *
 * The middle step used to be a gallery of all eight bodies and a text field.
 * Both are gone: the body is a function of this phone (`CreatureConcept.assignedTo`)
 * and so is the name (`CreatureName.forSeed`), and renaming lives in Settings
 * where it belongs — you rename something you already know, not something you
 * are about to meet.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = LocalAnimaColors.current

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        when (state.stage) {
            OnboardingStage.HATCH ->
                HatchStage(
                    // Until the seed is read there is no creature to hatch into,
                    // so the shell does not open. This is the same rule as
                    // everywhere else since v1.1b: nobody, rather than somebody
                    // else. The read is a DataStore hit and takes one frame.
                    ready = state.seed != null,
                    onHatched = viewModel::onHatched,
                )

            OnboardingStage.REVEAL ->
                state.seed?.let { seed ->
                    state.concept?.let { concept ->
                        RevealStage(
                            seed = seed,
                            concept = concept,
                            name = state.name,
                            onContinue = viewModel::onRevealAcknowledged,
                        )
                    }
                }

            OnboardingStage.WIDGET ->
                WidgetStage(
                    canPin = state.canPinWidget,
                    onPin = viewModel::onPinWidgetRequested,
                    onDone = { viewModel.onFinished(onFinished) },
                )
        }
    }
}

/**
 * The one long animation in the product (motion bible): a finite episode —
 * an egg of light trembles, cracks, bursts; the newborn spirit fades in.
 * Tap to skip. It runs once and stops; afterwards only the creature's own
 * ambient life continues.
 */
@Composable
private fun HatchStage(
    ready: Boolean,
    onHatched: () -> Unit,
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var skipped by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = HATCH_MILLIS, easing = LinearEasing),
        ) { value, _ ->
            if (!skipped) progress = value
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .testTag("onboarding.hatch")
            .pointerInput(Unit) {
                detectTapGestures {
                    skipped = true
                    progress = 1f
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.6f))
        Box(Modifier.fillMaxWidth().weight(2f), contentAlignment = Alignment.Center) {
            if (progress < 0.999f) {
                Canvas(Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val r = size.minDimension * 0.22f
                    // Tremble grows with progress; pure function of progress.
                    val tremble = sin(progress * 80f) * r * 0.035f * progress
                    val eggC = Offset(c.x + tremble, c.y)
                    drawOval(
                        brush =
                            Brush.radialGradient(
                                listOf(Color(0xFFEFF3FF), Color(0xFF9FB4D8)),
                                center = eggC.copy(y = eggC.y - r * 0.4f),
                                radius = r * 1.6f,
                            ),
                        topLeft = Offset(eggC.x - r * 0.82f, eggC.y - r * 1.05f),
                        size =
                            androidx.compose.ui.geometry
                                .Size(r * 1.64f, r * 2.1f),
                    )
                    // Cracks appear in thirds.
                    val crack = Color(0xFF3A4160)
                    if (progress > 0.35f) {
                        drawLine(
                            crack,
                            Offset(eggC.x - r * 0.3f, eggC.y - r * 0.5f),
                            Offset(eggC.x + r * 0.05f, eggC.y - r * 0.1f),
                            strokeWidth = r * 0.045f,
                            cap = StrokeCap.Round,
                        )
                    }
                    if (progress > 0.6f) {
                        drawLine(
                            crack,
                            Offset(eggC.x + r * 0.05f, eggC.y - r * 0.1f),
                            Offset(eggC.x + r * 0.4f, eggC.y + r * 0.25f),
                            strokeWidth = r * 0.04f,
                            cap = StrokeCap.Round,
                        )
                    }
                    if (progress > 0.8f) {
                        // Light leaks out of the cracks before the burst.
                        drawCircle(
                            Color(0xFF8FD3C7).copy(alpha = (progress - 0.8f) * 3f),
                            radius = r * (0.4f + progress * 0.8f),
                            center = eggC,
                        )
                    }
                }
            } else {
                // Born, and not yet anybody. v1.1b task 1c: this used to be a
                // literal `rememberCreature(SPIRIT_ORB, seed)` — the comment
                // called it "the formless spirit", but SPIRIT_ORB is one of the
                // eight real bodies, so whoever's phone was going to be a fox
                // watched a different creature hatch out of their egg. What is
                // drawn now is light with no features: the same glow the cracks
                // were leaking, opened out. Nothing here says which body follows.
                Canvas(Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val r = size.minDimension * 0.22f
                    drawCircle(
                        brush =
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFFDFF7F1),
                                    Color(0xFF8FD3C7).copy(alpha = 0.55f),
                                    Color(0x008FD3C7),
                                ),
                                center = c,
                                radius = r * BORN_GLOW_RADII,
                            ),
                        radius = r * BORN_GLOW_RADII,
                        center = c,
                    )
                }
            }
        }
        Text(
            if (progress < 0.999f) {
                stringResource(R.string.onboarding_waking)
            } else {
                stringResource(R.string.onboarding_hatched)
            },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        if (progress >= 0.999f && ready) {
            PillButton(
                stringResource(R.string.onboarding_meet),
                onClick = onHatched,
                modifier = Modifier.testTag("onboarding.meet"),
            )
        } else {
            Text(
                stringResource(R.string.onboarding_hurry_hint),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Spacer(Modifier.weight(0.8f))
    }
}

/**
 * The body and the name, both already decided by the phone. Read-only on
 * purpose: nothing on this screen can be picked, so nothing on it implies the
 * creature is a configuration.
 */
@Composable
private fun RevealStage(
    seed: Long,
    concept: CreatureConcept,
    name: String,
    onContinue: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .testTag("onboarding.reveal"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val controller = rememberCreature(concept, seed)
        controller.setBodyState(BodyState.Resting)
        CreatureSurface(
            controller = controller,
            night = true,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        )
        Text(name, style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.onboarding_reveal_body_belongs),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        // The honest contract, compressed to what matters before the first
        // word (v0.3 ≤3-screen onboarding); the full story is the trust
        // page in Settings, reachable any time.
        Text(
            stringResource(R.string.onboarding_contract),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        PillButton(
            stringResource(R.string.onboarding_reveal_continue, name),
            onClick = onContinue,
            modifier = Modifier.testTag("onboarding.reveal.continue"),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.onboarding_rename_hint),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * The last step, and the one that decides whether the creature is ever seen
 * again: a companion nobody looks at is a companion that dies of neglect by
 * design. Asking here costs one tap and the system draws its own confirmation.
 */
@Composable
private fun WidgetStage(
    canPin: Boolean,
    onPin: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .testTag("onboarding.widget"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.onboarding_widget_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(
                if (canPin) R.string.onboarding_widget_hint else R.string.onboarding_widget_manual,
            ),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        if (canPin) {
            PillButton(
                stringResource(R.string.onboarding_widget_add),
                onClick = onPin,
                modifier = Modifier.testTag("onboarding.widget.add"),
            )
            Spacer(Modifier.height(10.dp))
        }
        GhostButton(
            stringResource(R.string.onboarding_begin),
            onClick = onDone,
            modifier = Modifier.testTag("onboarding.begin"),
        )
    }
}

/** Radius of the featureless born-glow, in egg radii (v1.1b task 1c). */
private const val BORN_GLOW_RADII = 1.6f

private const val HATCH_MILLIS = 6000
