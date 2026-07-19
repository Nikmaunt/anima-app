package app.anima.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.anima.core.creature.CreatureSurface
import app.anima.core.creature.PurrHaptics
import app.anima.core.creature.rememberCreature
import app.anima.core.model.ChatMessage
import app.anima.core.model.ChatRole
import app.anima.core.model.CreatureGenome
import app.anima.core.model.FactCandidate
import app.anima.core.model.MindFailure
import app.anima.core.model.MindStatus
import app.anima.core.model.MindTier
import app.anima.core.model.tunedBy
import app.anima.core.ui.components.GhostButton
import app.anima.core.ui.components.PillButton
import app.anima.core.ui.theme.LocalAnimaColors

/**
 * The creature's screen: the being fills the upper half; conversation flows
 * beneath it. The creature reacts to typing and thinks with its body — there
 * is no spinner chrome anywhere.
 */
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenSoul: () -> Unit,
    onOpenDiary: () -> Unit,
    onOpenRest: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val bodyEvent by viewModel.bodyEvents.collectAsState()
    val colors = LocalAnimaColors.current

    // ON_STOP drops the loaded Gemma engine (~1 GB); ON_RESUME re-probes the
    // tier — the Mind screen may have installed or deleted a model meanwhile.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> viewModel.onAppBackgrounded()
                    Lifecycle.Event.ON_RESUME -> viewModel.refreshMindStatus()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Navigating away: speech is a visible-screen episode (ADR-013).
            viewModel.stopVoice()
        }
    }

    // Personality tunes the genome (saccades, blink pace) within its bounds;
    // the life stage scales the whole body. Both deterministic (v0.2).
    val genome =
        remember(state.seed, state.personality) {
            CreatureGenome.from(state.seed).tunedBy(state.personality)
        }
    val controller = rememberCreature(state.concept, state.seed, genome)
    val paletteShift by viewModel.paletteShift.collectAsState()
    controller.paletteShiftDeg = paletteShift
    controller.setBodyState(state.bodyState)
    controller.setGrowth(state.growth)
    controller.setStage(state.stage)
    controller.onThinking(state.streamingReply != null)

    // v0.3 haptic map: rich moments run through PurrHaptics' own primitive
    // gates (arePrimitivesSupported) — unsupported hardware stays silent.
    val context = LocalContext.current
    val richHaptics = remember { PurrHaptics(context) }
    LaunchedEffect(bodyEvent) {
        when (bodyEvent) {
            BodyEvent.CELEBRATE_CHARGE -> {
                controller.onCelebrate()
                richHaptics.celebrate()
            }
            BodyEvent.STARTLE_STORM -> {
                controller.onStartle()
                richHaptics.startle()
            }
            null -> Unit
        }
        if (bodyEvent != null) viewModel.onBodyEventHandled()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .imePadding()
                .navigationBarsPadding(),
    ) {
        // Header: quiet chrome, the creature owns the screen.
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(state.creatureName, style = MaterialTheme.typography.titleMedium)
                val days = state.daysTogether.toInt().coerceAtLeast(0)
                val daysText = pluralStringResource(R.plurals.home_days_together, days, days)
                Text(
                    // ADR-011: the user always knows which mind speaks.
                    if (state.activeTier == MindTier.CLOUD) {
                        stringResource(R.string.home_days_cloud, daysText)
                    } else {
                        daysText
                    },
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            GhostButton(
                stringResource(R.string.home_nav_rest),
                onClick = onOpenRest,
                modifier = Modifier.testTag("home.rest"),
            )
            GhostButton(
                stringResource(R.string.home_nav_soul),
                onClick = onOpenSoul,
                modifier = Modifier.testTag("home.soul"),
            )
            GhostButton(
                stringResource(R.string.home_nav_diary),
                onClick = onOpenDiary,
                modifier = Modifier.testTag("home.diary"),
            )
            GhostButton(
                stringResource(R.string.home_nav_settings),
                onClick = onOpenSettings,
                modifier = Modifier.testTag("home.settings"),
            )
        }

        CreatureSurface(
            controller = controller,
            night = colors.isNight,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(0.95f),
            reducedMotionOverride = if (state.calmMotion) true else null,
            contentDescription = creatureA11y(state),
        )

        // Fact candidates: nothing enters the soul without a tap.
        val hapticFeedback = LocalHapticFeedback.current
        if (state.candidates.isNotEmpty()) {
            CandidateBar(
                candidate = state.candidates.first(),
                onConfirm = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                    viewModel.confirmCandidate(it)
                },
                onReject = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                    viewModel.rejectCandidate(it)
                },
            )
        }

        // v0.5 capsule delivery (ideation-v5 №3): a letter from the past
        // self came due — the creature hands it over, exactly once.
        val dueCapsule by viewModel.dueCapsule.collectAsState()
        dueCapsule?.let { capsule ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
            ) {
                Text(
                    stringResource(R.string.home_capsule_intro),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    capsule.text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                GhostButton(
                    stringResource(R.string.home_capsule_thanks),
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                        viewModel.openCapsule()
                    },
                    modifier = Modifier.testTag("home.capsule.keep"),
                )
            }
        }

        // v0.5 evening farewell (ideation-v5 №1): an offer, never a demand —
        // skipping it records nothing and changes nothing.
        val goodnightAvailable by viewModel.goodnightAvailable.collectAsState()
        if (goodnightAvailable && state.streamingReply == null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.home_goodnight_line),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                GhostButton(
                    stringResource(R.string.home_goodnight_button),
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.sayGoodnight()
                    },
                    modifier = Modifier.testTag("home.goodnight"),
                )
            }
        }

        // v0.3 dreams: at night, one gentle wake earns a told dream.
        if (state.dreamAvailable && state.streamingReply == null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.home_dream_line),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                GhostButton(stringResource(R.string.home_dream_wake), onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.wakeForDream()
                })
            }
        }

        state.mindNotice?.let { notice ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    // Closed set (MindFailure) mapped onto localized resources.
                    when (notice) {
                        MindFailure.TIRED -> stringResource(R.string.home_notice_tired)
                        MindFailure.LOST_THOUGHT -> stringResource(R.string.home_notice_lost)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                GhostButton(stringResource(R.string.home_ok), onClick = viewModel::dismissMindNotice)
            }
        }

        ChatPanel(
            state = state,
            onSend = { text ->
                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                viewModel.send(text)
            },
            onTyping = { active ->
                controller.onTyping(active)
                if (active) viewModel.onInteraction()
            },
            onRequestDownload = viewModel::requestMindDownload,
            onOpenMind = onOpenSettings,
            onRegenerate = viewModel::regenerate,
            onRememberThis = { message ->
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.rememberThis(message)
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun ChatPanel(
    state: HomeUiState,
    onSend: (String) -> Unit,
    onTyping: (Boolean) -> Unit,
    onRequestDownload: () -> Unit,
    onOpenMind: () -> Unit,
    onRegenerate: () -> Unit,
    onRememberThis: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAnimaColors.current
    val listState = rememberLazyListState()
    val itemCount = state.messages.size + (if (state.streamingReply != null) 1 else 0)

    LaunchedEffect(itemCount, state.streamingReply?.length) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.messages.isEmpty() && state.streamingReply == null) {
                item(key = "empty") {
                    // v0.3: no blank thread — the creature (already on
                    // screen above) breaks the ice in its own voice.
                    Text(
                        if (state.mindStatus == MindStatus.READY) {
                            stringResource(R.string.home_empty_ready)
                        } else {
                            stringResource(R.string.home_empty_no_mind)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                }
            }
            items(state.messages, key = { it.id }) { message ->
                MessageBubble(
                    text = message.text,
                    mine = message.role == ChatRole.USER,
                    // v0.3: long-press → "remember this" (same confirm gate).
                    onLongPress = { onRememberThis(message) },
                )
            }
            if (state.streamingReply != null) {
                item(key = "streaming") {
                    MessageBubble(
                        text = state.streamingReply.ifEmpty { "…" },
                        mine = false,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        when (state.mindStatus) {
            MindStatus.READY -> {
                // Conversation starters from the body diary (v0.2): shown
                // while the thread is idle; a tap just sends the question.
                if (state.starters.isNotEmpty() && state.streamingReply == null) {
                    StarterChips(starters = state.starters, onPick = onSend)
                }
                // v0.3: another roll of the same question, honestly appended.
                if (state.streamingReply == null && state.messages.lastOrNull()?.role == ChatRole.CREATURE) {
                    Row(Modifier.padding(horizontal = 20.dp)) {
                        GhostButton(stringResource(R.string.home_regenerate), onClick = onRegenerate)
                    }
                }
                InputRow(enabled = state.streamingReply == null, onSend = onSend, onTyping = onTyping)
            }
            MindStatus.DOWNLOADABLE ->
                MindBanner(
                    text = stringResource(R.string.home_banner_downloadable),
                    action = { PillButton(stringResource(R.string.home_banner_wake), onClick = onRequestDownload) },
                )
            MindStatus.DOWNLOADING -> MindBanner(text = stringResource(R.string.home_banner_downloading))
            MindStatus.ASLEEP ->
                MindBanner(
                    text = stringResource(R.string.home_banner_asleep),
                    action = { PillButton(stringResource(R.string.home_banner_bring), onClick = onOpenMind) },
                )
        }
    }
}

/** TalkBack sentence: "Lumi is dozing, battery 23%". */
@Composable
private fun creatureA11y(state: HomeUiState): String {
    val name = state.creatureName.ifEmpty { stringResource(R.string.home_a11y_name_fallback) }
    val doing =
        stringResource(
            when (state.bodyState.mood) {
                app.anima.core.model.Mood.ALERT -> R.string.home_a11y_mood_awake
                app.anima.core.model.Mood.BORED -> R.string.home_a11y_mood_bored
                app.anima.core.model.Mood.SLEEPY -> R.string.home_a11y_mood_dozing
                app.anima.core.model.Mood.EATING -> R.string.home_a11y_mood_eating
                app.anima.core.model.Mood.ANXIOUS -> R.string.home_a11y_mood_anxious
                app.anima.core.model.Mood.ASLEEP -> R.string.home_a11y_mood_asleep
                app.anima.core.model.Mood.HOT -> R.string.home_a11y_mood_hot
            },
        )
    return stringResource(R.string.home_a11y_state, name, doing, state.bodyState.signals.batteryPercent) +
        if (state.bodyState.signals.charging) stringResource(R.string.home_a11y_charging) else ""
}

@Composable
private fun StarterChips(
    starters: List<HomeStarter>,
    onPick: (String) -> Unit,
) {
    val colors = LocalAnimaColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        starters.forEach { starter ->
            // Resolved here, in the current locale; the tap sends the
            // resolved line, so what lands in the chat is what was read.
            val text = starterText(starter)
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .clickable { onPick(text) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

/** Closed starter set (VM ships data, the UI speaks the locale). */
@Composable
private fun starterText(starter: HomeStarter): String =
    when (starter) {
        is HomeStarter.FedTimes ->
            pluralStringResource(R.plurals.home_starter_fed, starter.count, starter.count)
        HomeStarter.StormAsk -> stringResource(R.string.home_starter_storm)
        HomeStarter.RanHot -> stringResource(R.string.home_starter_hot)
        HomeStarter.RememberToday -> stringResource(R.string.home_starter_remember)
        HomeStarter.AnythingGood -> stringResource(R.string.home_starter_good)
    }

@Composable
private fun MindBanner(
    text: String,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = LocalAnimaColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
        action?.invoke()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    text: String,
    mine: Boolean,
    onLongPress: (() -> Unit)? = null,
) {
    val colors = LocalAnimaColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        val shape =
            RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (mine) 18.dp else 6.dp,
                bottomEnd = if (mine) 6.dp else 18.dp,
            )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (mine) colors.background else colors.text,
            modifier =
                Modifier
                    .testTag("home.chat.message")
                    .clip(shape)
                    .background(if (mine) colors.accent else colors.surface)
                    .let { base ->
                        if (onLongPress != null) {
                            base.combinedClickable(onClick = {}, onLongClick = onLongPress)
                        } else {
                            base
                        }
                    }.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun CandidateBar(
    candidate: FactCandidate,
    onConfirm: (FactCandidate) -> Unit,
    onReject: (FactCandidate) -> Unit,
) {
    val colors = LocalAnimaColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.accentSoft)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.home_remember_q), style = MaterialTheme.typography.labelMedium)
            Text(candidate.text, style = MaterialTheme.typography.bodyMedium, color = colors.text)
        }
        GhostButton(stringResource(R.string.home_no), onClick = { onReject(candidate) })
        GhostButton(stringResource(R.string.home_yes), onClick = { onConfirm(candidate) })
    }
}

@Composable
private fun InputRow(
    enabled: Boolean,
    onSend: (String) -> Unit,
    onTyping: (Boolean) -> Unit,
) {
    val colors = LocalAnimaColors.current
    var text by remember { mutableStateOf("") }

    LaunchedEffect(text) { onTyping(text.isNotEmpty()) }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(colors.surface)
            .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.weight(1f).testTag("home.chat.input"),
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            stringResource(R.string.home_input_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textDim,
                        )
                    }
                    inner()
                }
            },
        )
        IconButton(
            onClick = {
                if (enabled && text.isNotBlank()) {
                    onSend(text)
                    text = ""
                }
            },
            modifier = Modifier.testTag("home.chat.send"),
        ) {
            Icon(
                imageVector = sendIcon(),
                contentDescription = stringResource(R.string.home_send),
                tint = if (enabled && text.isNotBlank()) colors.accent else colors.textDim,
            )
        }
    }
}

/** Hand-drawn send glyph — no icon pack dependency. */
@Composable
private fun sendIcon(): ImageVector =
    remember {
        ImageVector
            .Builder(
                name = "send",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(androidx.compose.ui.graphics.Color.White)) {
                    moveTo(3f, 20f)
                    lineTo(21f, 12f)
                    lineTo(3f, 4f)
                    lineTo(3f, 10f)
                    lineTo(15f, 12f)
                    lineTo(3f, 14f)
                    close()
                }
            }.build()
    }
