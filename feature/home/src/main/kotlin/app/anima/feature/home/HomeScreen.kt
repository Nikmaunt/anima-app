package app.anima.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.anima.core.creature.CreatureSurface
import app.anima.core.creature.rememberCreature
import app.anima.core.model.ChatRole
import app.anima.core.model.CreatureGenome
import app.anima.core.model.FactCandidate
import app.anima.core.model.MindStatus
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
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Personality tunes the genome (saccades, blink pace) within its bounds;
    // the life stage scales the whole body. Both deterministic (v0.2).
    val genome =
        remember(state.seed, state.personality) {
            CreatureGenome.from(state.seed).tunedBy(state.personality)
        }
    val controller = rememberCreature(state.concept, state.seed, genome)
    controller.setBodyState(state.bodyState)
    controller.setGrowth(state.growth)
    controller.setStage(state.stage)
    controller.onThinking(state.streamingReply != null)

    LaunchedEffect(bodyEvent) {
        when (bodyEvent) {
            BodyEvent.CELEBRATE_CHARGE -> controller.onCelebrate()
            BodyEvent.STARTLE_STORM -> controller.onStartle()
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
                Text(
                    pluralDays(state.daysTogether),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            GhostButton("Soul", onClick = onOpenSoul)
            GhostButton("Diary", onClick = onOpenDiary)
            GhostButton("Settings", onClick = onOpenSettings)
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
        if (state.candidates.isNotEmpty()) {
            CandidateBar(
                candidate = state.candidates.first(),
                onConfirm = viewModel::confirmCandidate,
                onReject = viewModel::rejectCandidate,
            )
        }

        state.mindNotice?.let { notice ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(notice, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                GhostButton("Ok", onClick = viewModel::dismissMindNotice)
            }
        }

        ChatPanel(
            state = state,
            onSend = viewModel::send,
            onTyping = { active ->
                controller.onTyping(active)
                if (active) viewModel.onInteraction()
            },
            onRequestDownload = viewModel::requestMindDownload,
            onOpenMind = onOpenSettings,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ChatPanel(
    state: HomeUiState,
    onSend: (String) -> Unit,
    onTyping: (Boolean) -> Unit,
    onRequestDownload: () -> Unit,
    onOpenMind: () -> Unit,
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
            items(state.messages, key = { it.id }) { message ->
                MessageBubble(text = message.text, mine = message.role == ChatRole.USER)
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
                InputRow(enabled = state.streamingReply == null, onSend = onSend, onTyping = onTyping)
            }
            MindStatus.DOWNLOADABLE ->
                MindBanner(
                    text = "Its mind can wake on this phone — the system needs to fetch it once.",
                    action = { PillButton("Wake the mind", onClick = onRequestDownload) },
                )
            MindStatus.DOWNLOADING -> MindBanner(text = "The mind is waking up… (system download)")
            MindStatus.ASLEEP ->
                MindBanner(
                    text =
                        "The mind sleeps. This phone has no built-in mind for apps, " +
                            "but you can bring one — a single ~530 MB file wakes it, " +
                            "fully on-device (Settings → Mind).",
                    action = { PillButton("Bring a mind", onClick = onOpenMind) },
                )
        }
    }
}

/** TalkBack sentence: "Lumi is dozing, battery 23%". */
private fun creatureA11y(state: HomeUiState): String {
    val name = state.creatureName.ifEmpty { "Your creature" }
    val doing =
        when (state.bodyState.mood) {
            app.anima.core.model.Mood.ALERT -> "is awake and watching"
            app.anima.core.model.Mood.BORED -> "is bored"
            app.anima.core.model.Mood.SLEEPY -> "is dozing"
            app.anima.core.model.Mood.EATING -> "is eating"
            app.anima.core.model.Mood.ANXIOUS -> "is anxious"
            app.anima.core.model.Mood.ASLEEP -> "is asleep"
            app.anima.core.model.Mood.HOT -> "is running hot"
        }
    return "$name $doing, battery ${state.bodyState.signals.batteryPercent}%" +
        if (state.bodyState.signals.charging) ", charging" else ""
}

@Composable
private fun StarterChips(
    starters: List<String>,
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
            Text(
                starter,
                style = MaterialTheme.typography.bodyMedium,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .clickable { onPick(starter) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
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

@Composable
private fun MessageBubble(
    text: String,
    mine: Boolean,
) {
    val colors = LocalAnimaColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (mine) colors.background else colors.text,
            modifier =
                Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (mine) 18.dp else 6.dp,
                            bottomEnd = if (mine) 6.dp else 18.dp,
                        ),
                    ).background(if (mine) colors.accent else colors.surface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
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
            Text("Remember this?", style = MaterialTheme.typography.labelMedium)
            Text(candidate.text, style = MaterialTheme.typography.bodyMedium, color = colors.text)
        }
        GhostButton("No", onClick = { onReject(candidate) })
        GhostButton("Yes", onClick = { onConfirm(candidate) })
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
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            "Say something…",
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
        ) {
            Icon(
                imageVector = sendIcon(),
                contentDescription = "Send",
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

private fun pluralDays(days: Long): String = "together $days ${if (days == 1L) "day" else "days"}"
