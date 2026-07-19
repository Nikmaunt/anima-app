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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.anima.core.creature.ConceptGallery
import app.anima.core.creature.CreatureSurface
import app.anima.core.creature.rememberCreature
import app.anima.core.model.BodyState
import app.anima.core.model.CreatureConcept
import app.anima.core.ui.components.GhostButton
import app.anima.core.ui.components.PillButton
import app.anima.core.ui.theme.LocalAnimaColors
import kotlin.math.sin

/** Awakening: hatch → choose a body → give a name → the honest contract. */
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
            OnboardingStage.HATCH -> HatchStage(seed = state.seed, onHatched = viewModel::onHatched)
            OnboardingStage.CHOOSE ->
                ChooseStage(
                    seed = state.seed,
                    selected = state.concept,
                    onSelect = viewModel::onConceptChosen,
                    onConfirm = viewModel::onConceptConfirmed,
                )
            OnboardingStage.NAME ->
                NameStage(
                    seed = state.seed,
                    concept = state.concept ?: CreatureConcept.SPIRIT_ORB,
                    name = state.name,
                    onNameChanged = viewModel::onNameChanged,
                    onConfirm = { viewModel.complete(onFinished) },
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
    seed: Long,
    onHatched: () -> Unit,
) {
    val colors = LocalAnimaColors.current
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
                // Born: the formless spirit; a body is chosen next.
                val controller = rememberCreature(CreatureConcept.SPIRIT_ORB, seed)
                controller.setBodyState(BodyState.Resting)
                CreatureSurface(controller = controller, night = true, modifier = Modifier.fillMaxSize())
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
        if (progress >= 0.999f) {
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

@Composable
private fun ChooseStage(
    seed: Long,
    selected: CreatureConcept?,
    onSelect: (CreatureConcept) -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = LocalAnimaColors.current
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text(
            stringResource(R.string.onboarding_choose_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            stringResource(R.string.onboarding_choose_hint),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        ConceptGallery(
            seed = seed,
            selected = selected,
            onSelect = onSelect,
            accent = colors.accent,
            surface = colors.surface,
            outline = colors.outline,
            modifier = Modifier.weight(1f),
        )
        PillButton(
            stringResource(R.string.onboarding_choose_confirm),
            onClick = onConfirm,
            enabled = selected != null,
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 14.dp)
                    .testTag("onboarding.confirmConcept"),
        )
    }
}

@Composable
private fun NameStage(
    seed: Long,
    concept: CreatureConcept,
    name: String,
    onNameChanged: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = LocalAnimaColors.current
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
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
        Text(stringResource(R.string.onboarding_name_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(14.dp))
        BasicTextField(
            value = name,
            onValueChange = onNameChanged,
            textStyle =
                MaterialTheme.typography.displayMedium.copy(
                    color = colors.text,
                    textAlign = TextAlign.Center,
                ),
            cursorBrush = SolidColor(colors.accent),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .padding(vertical = 14.dp)
                    .testTag("onboarding.name.input"),
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
        Spacer(Modifier.height(14.dp))
        PillButton(
            stringResource(R.string.onboarding_begin),
            onClick = onConfirm,
            enabled = name.isNotBlank(),
            modifier = Modifier.testTag("onboarding.begin"),
        )
        Spacer(Modifier.height(24.dp))
    }
}

private const val HATCH_MILLIS = 6000
