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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.platform.LocalConfiguration
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
import app.anima.core.ui.components.ActionRow
import app.anima.core.ui.components.GhostButton
import app.anima.core.ui.components.PillButton
import app.anima.core.ui.components.enterStaggered
import app.anima.core.ui.theme.AnimaSpacing
import app.anima.core.ui.theme.LocalAnimaColors

/**
 * The creature's screen. v1.1: the creature IS the screen.
 *
 * Until now the being shared its height with a chat thread, and the owner's
 * verdict on the device was that it read as a sticker wedged between text
 * blocks (defect D10). The chat slot has moved to its own route behind an
 * off-by-default toggle, so what is left here is the body, whatever it has
 * to say right now, and a way out — in that order of importance.
 *
 * The creature still thinks with its body; there is no spinner chrome
 * anywhere.
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
            // v0.6 (ideation №8): a roomier burrow feels like a small feast.
            BodyEvent.BURROW_ROOMIER -> {
                controller.onCelebrate()
                richHaptics.celebrate()
            }
            null -> Unit
        }
        if (bodyEvent != null) viewModel.onBodyEventHandled()
    }

    val hapticFeedback = LocalHapticFeedback.current

    // Header: the creature's name and how long you have been together, and
    // nothing else. v1.1 (defects D1 and D3): the v1.0 header put a weighted
    // name Column and four ghost buttons in one Row, so once the buttons'
    // intrinsic widths exceeded the line, the name was squeezed to near-zero
    // width and wrapped one letter per line while the last button split as
    // "Настройк"/"и". Reproduced at font_scale 1.3 and, separately, at
    // density 480 with the default font size — see
    // docs/design/v11/phase0-emulator-eyes.md. Nothing shares this line now,
    // so there is no width left to lose. Navigation moved to its own wrapping
    // row below the creature.
    val headerRow: @Composable () -> Unit = {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AnimaSpacing.m, vertical = AnimaSpacing.s)
                    .enterStaggered(0),
        ) {
            // displayMedium (32 Light), not headlineMedium (24 Normal): at 24
            // the name sat only two steps above its own caption and the top of
            // the screen had no anchor at all. The 44sp `hero` step is
            // deliberately NOT used here — on this screen the hero is the
            // body, and a giant number would compete with it.
            Text(state.creatureName, style = MaterialTheme.typography.displayMedium)
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
    }

    // Navigation wraps instead of crushing (design-system-v11 §1, overflow
    // rule 2). Four labels at font_scale 1.3 do not fit one line; they now
    // take a second one whole.
    val navRow: @Composable () -> Unit = {
        ActionRow(
            Modifier
                .padding(horizontal = AnimaSpacing.m, vertical = AnimaSpacing.s)
                .enterStaggered(NOTICE_STAGGER_BASE + 3),
        ) {
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
                quiet = true,
            )
        }
    }

    val creature: @Composable (Modifier) -> Unit = { creatureModifier ->
        CreatureSurface(
            controller = controller,
            night = colors.isNight,
            modifier = creatureModifier,
            reducedMotionOverride = if (state.calmMotion) true else null,
            contentDescription = creatureA11y(state),
        )
    }

    val cardsAndNotices: @Composable () -> Unit = {
        // Fact candidates: nothing enters the soul without a tap.
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
        // v0.6 (audit-v05 D1): the letter is soul content — FLAG_SECURE
        // while it's on screen, honoring the same Settings toggle as Soul.
        val dueCapsule by viewModel.dueCapsule.collectAsState()
        val capsuleScreenshotsAllowed by viewModel.screenshotsAllowed.collectAsState()
        app.anima.core.ui.components.SecureWhile(
            dueCapsule != null && !capsuleScreenshotsAllowed,
        )
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
            Notice(
                line = stringResource(R.string.home_goodnight_line),
                action = stringResource(R.string.home_goodnight_button),
                onAction = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.sayGoodnight()
                },
                index = NOTICE_STAGGER_BASE,
                testTag = "home.goodnight",
            )
        }

        // v0.3 dreams: at night, one gentle wake earns a told dream.
        if (state.dreamAvailable && state.streamingReply == null) {
            Notice(
                line = stringResource(R.string.home_dream_line),
                action = stringResource(R.string.home_dream_wake),
                onAction = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.wakeForDream()
                },
                index = NOTICE_STAGGER_BASE + 1,
                testTag = "home.dream",
            )
        }

        state.mindNotice?.let { notice ->
            Notice(
                // Closed set (MindFailure) mapped onto localized resources.
                line =
                    when (notice) {
                        MindFailure.TIRED -> stringResource(R.string.home_notice_tired)
                        MindFailure.LOST_THOUGHT -> stringResource(R.string.home_notice_lost)
                    },
                action = stringResource(R.string.home_ok),
                onAction = viewModel::dismissMindNotice,
                index = NOTICE_STAGGER_BASE + 2,
                testTag = "home.notice",
            )
        }
    }

    HomeAdaptiveScaffold(
        // LocalConfiguration, not BoxWithConstraints: subcomposition remeasures
        // against CreatureSurface's endless frame loop and can trap a pumped
        // test frame in a measure storm (caught live by the v0.6 E2E).
        expanded = LocalConfiguration.current.screenWidthDp >= EXPANDED_MIN_WIDTH_DP,
        headerRow = headerRow,
        creature = creature,
        cards = cardsAndNotices,
        navRow = navRow,
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .imePadding()
                .navigationBarsPadding(),
    )
}

/**
 * v0.6 tablets/folds: past the expanded threshold the creature gets its own
 * pane (Samsung folds are this product's home turf). Thresholds follow the
 * WindowSizeClass dp bands; hand-rolled because one constant does not
 * justify a library (repo convention). Stateless so the golden rig can
 * drive both branches.
 *
 * v1.1: the chat slot is gone (defect D10 — the creature was not the hero of
 * its own screen, it was one of four things competing for the same height).
 * Chat lives on its own route now, off by default behind Settings'
 * experimental toggle. What is left on Home is the creature, whatever it has
 * to say right now, and a way out.
 *
 * The creature no longer shares its height with a message list, so on a phone
 * it takes everything the header, cards and nav do not — see
 * docs/design/v11/after/ for what that looks like.
 */
@Composable
internal fun HomeAdaptiveScaffold(
    expanded: Boolean,
    headerRow: @Composable () -> Unit,
    creature: @Composable (Modifier) -> Unit,
    cards: @Composable () -> Unit,
    navRow: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (expanded) {
        Row(modifier) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                headerRow()
                creature(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                cards()
                navRow()
            }
        }
    } else {
        Column(modifier) {
            headerRow()
            creature(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
            // The notices and the way out are two different things and used to
            // be one undifferentiated pile in the bottom quarter. `xl` between
            // them is the same gap the design system uses everywhere else to
            // mean "new group", and it is four times the spacing inside either
            // one, so the boundary reads without a rule or a card.
            Column(verticalArrangement = Arrangement.spacedBy(AnimaSpacing.xl)) {
                Column { cards() }
                navRow()
            }
            Spacer(Modifier.height(AnimaSpacing.m))
        }
    }
}

/**
 * One line the creature has to say, and the one thing you can do about it.
 *
 * v1.1: this replaces three copies of `Row { Text(weight(1f)); GhostButton }`.
 * That shape put a sentence and a button on the same line, so the sentence
 * got whatever width the button did not want — on the device it wrapped
 * mid-phrase and then ran out of room and ellipsised
 * ("Оно спит и видит сны о прошедшем дне…", docs/design/v11/before/07-home.png).
 * Stacking removes the competition entirely: the line gets the full width,
 * the action sits under it, and neither can starve the other at any font
 * scale.
 *
 * The line is `bodyLarge`, not the dimmed `bodyMedium` it used to be — the
 * creature talking is not supporting text.
 */
@Composable
private fun Notice(
    line: String,
    action: String,
    onAction: () -> Unit,
    index: Int,
    testTag: String,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = AnimaSpacing.m, vertical = AnimaSpacing.xs)
            .enterStaggered(index),
    ) {
        Text(line, style = MaterialTheme.typography.bodyLarge)
        GhostButton(action, onClick = onAction, modifier = Modifier.testTag(testTag))
    }
}

/**
 * The header enters first (0), then each notice, then the way out. Distinct
 * indices matter: two blocks sharing one index appear simultaneously, which
 * is the one thing a stagger is supposed to prevent.
 */
private const val NOTICE_STAGGER_BASE = 1

/** WindowSizeClass "expanded" lower bound (dp) — the two-pane switch. */
private const val EXPANDED_MIN_WIDTH_DP = 840

@Composable
internal fun ChatPanel(
    state: HomeUiState,
    onSend: (String) -> Unit,
    onTyping: (Boolean) -> Unit,
    onRequestDownload: () -> Unit,
    onOpenMind: () -> Unit,
    onRegenerate: () -> Unit,
    onRememberThis: (ChatMessage) -> Unit,
    onReportReply: (ChatMessage) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalAnimaColors.current
    val listState = rememberLazyListState()
    // v0.6: the long-press chooser for a creature reply (remember/report).
    var actionMessage by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<ChatMessage?>(null)
    }
    val onMessageActions: (ChatMessage) -> Unit = { actionMessage = it }
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
                    // v0.6: creature replies open a chooser instead — the
                    // second action is the GenAI-policy report affordance.
                    onLongPress = {
                        if (message.role == ChatRole.USER) {
                            onRememberThis(message)
                        } else {
                            onMessageActions(message)
                        }
                    },
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
        // v0.6: chooser for a long-pressed creature reply — remember it, or
        // report it (Play GenAI policy affordance; deletes the reply).
        actionMessage?.let { message ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
            ) {
                Text(
                    stringResource(R.string.home_reply_actions_title),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton(stringResource(R.string.home_remember_this), onClick = {
                        actionMessage = null
                        onRememberThis(message)
                    })
                    GhostButton(
                        stringResource(R.string.home_report_reply),
                        onClick = {
                            actionMessage = null
                            onReportReply(message)
                        },
                        modifier = Modifier.testTag("home.chat.report"),
                    )
                    GhostButton(stringResource(R.string.home_report_cancel), onClick = {
                        actionMessage = null
                    })
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
                    // v0.7 (audit-v06 DEGRADED №1): the size comes from the
                    // registry default, never a hardcoded number in copy.
                    text =
                        stringResource(
                            R.string.home_banner_asleep,
                            android.text.format.Formatter.formatShortFileSize(
                                LocalContext.current,
                                app.anima.core.model.MindModelRegistry.packDefault.approxSizeBytes,
                            ),
                        ),
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
