package app.anima.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.anima.core.body.BodySensors
import app.anima.core.data.prefs.AnimaPrefs
import app.anima.core.data.repo.ChatRepository
import app.anima.core.data.repo.IdentityRepository
import app.anima.core.data.repo.JournalRepository
import app.anima.core.data.repo.NotifEventsRepository
import app.anima.core.data.repo.SoulRepository
import app.anima.core.model.BodySignals
import app.anima.core.model.BodyState
import app.anima.core.model.ChatMessage
import app.anima.core.model.ChatRole
import app.anima.core.model.CreatureConcept
import app.anima.core.model.Evolution
import app.anima.core.model.FactCandidate
import app.anima.core.model.FactJson
import app.anima.core.model.FactSource
import app.anima.core.model.JournalKind
import app.anima.core.model.LifeStage
import app.anima.core.model.MindEngine
import app.anima.core.model.MindEvent
import app.anima.core.model.MindFailure
import app.anima.core.model.MindInventory
import app.anima.core.model.MindLanguage
import app.anima.core.model.MindLanguageRouting
import app.anima.core.model.MindModelRegistry
import app.anima.core.model.MindStatus
import app.anima.core.model.MindTier
import app.anima.core.model.Mood
import app.anima.core.model.MoodEngine
import app.anima.core.model.Personality
import app.anima.core.model.PromptBuilder
import app.anima.core.model.RelationshipStats
import app.anima.core.model.TimeCapsule
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-shot body events the creature should visibly react to. */
enum class BodyEvent { CELEBRATE_CHARGE, STARTLE_STORM, BURROW_ROOMIER }

/**
 * Closed set of conversation starters. The VM ships data (counters), the
 * UI resolves each into the current locale — the tapped line is sent (and
 * stored) exactly as the user read it.
 */
sealed interface HomeStarter {
    data class FedTimes(
        val count: Int,
    ) : HomeStarter

    data object StormAsk : HomeStarter

    data object RanHot : HomeStarter

    data object RememberToday : HomeStarter

    data object AnythingGood : HomeStarter
}

data class HomeUiState(
    val creatureName: String = "",
    val concept: CreatureConcept = CreatureConcept.SPIRIT_ORB,
    val seed: Long = 0L,
    val daysTogether: Long = 1,
    val bodyState: BodyState = BodyState.Resting,
    val messages: List<ChatMessage> = emptyList(),
    val streamingReply: String? = null,
    val mindStatus: MindStatus = MindStatus.ASLEEP,
    val candidates: List<FactCandidate> = emptyList(),
    val growth: Float = 0f,
    /** Closed failure set; the UI maps it onto localized resources. */
    val mindNotice: MindFailure? = null,
    val calmMotion: Boolean = false,
    val stage: LifeStage = LifeStage.NEWBORN,
    val personality: Personality = Personality.Default,
    val starters: List<HomeStarter> = emptyList(),
    /** ADR-011: which mind is speaking — CLOUD is always visibly labeled. */
    val activeTier: MindTier = MindTier.NONE,
    /** v0.3 dreams: asleep at night and not yet woken this night. */
    val dreamAvailable: Boolean = false,
    /**
     * Phase 1D honesty badge: the active mind can't speak the interface
     * language, so the creature answers in English (never garbled).
     */
    val englishFallback: Boolean = false,
)

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        @ApplicationContext private val appContext: Context,
        private val sensors: BodySensors,
        private val weatherFeel: app.anima.core.body.WeatherFeel,
        private val identity: IdentityRepository,
        private val chat: ChatRepository,
        private val soul: SoulRepository,
        private val journal: JournalRepository,
        private val notifEvents: NotifEventsRepository,
        private val capsules: app.anima.core.data.repo.TimeCapsuleRepository,
        private val mind: MindEngine,
        private val mindInventory: MindInventory,
        private val prefs: AnimaPrefs,
        private val voice: app.anima.core.voice.CreatureVoice,
        private val voiceConfig: app.anima.core.voice.VoiceConfigStore,
        // v0.6: time-of-day rituals gate on this seam, not the wall clock,
        // so the day-in-life E2E can pin an evening (ClockModule).
        private val clock: app.anima.core.model.AnimaClock,
    ) : ViewModel() {
        /** ADR-013: armed only after an offline voice was confirmed. */
        @Volatile
        private var voiceReady = false

        /** v0.4 milestones: the worn palette's hue rotation (0 = true self). */
        val paletteShift: StateFlow<Float> =
            prefs
                .paletteVariant()
                .map { wire ->
                    val now = System.currentTimeMillis()
                    app.anima.core.model.Milestones
                        .effectiveShiftDeg(wire, identity.stats(now), now)
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)
        private val lastInteractionAt = MutableStateFlow(System.currentTimeMillis())
        private val streaming = MutableStateFlow<String?>(null)
        private val candidates = MutableStateFlow<List<FactCandidate>>(emptyList())
        private val mindStatus = MutableStateFlow(MindStatus.ASLEEP)
        private val mindTier = MutableStateFlow(MindTier.NONE)
        private val mindNotice = MutableStateFlow<MindFailure?>(null)
        private val langFallback = MutableStateFlow(false)
        private val events = MutableStateFlow<BodyEvent?>(null)
        private val dreamAvailable = MutableStateFlow(false)

        // Declared BEFORE init: the init coroutines touch these on
        // Main.immediate, and Kotlin initializes properties in declaration
        // order (the first GMD run of v0.5 caught exactly this NPE).
        private val goodnight = MutableStateFlow(false)
        private val dueCapsuleState = MutableStateFlow<TimeCapsule?>(null)

        /** The last user line — regeneration re-asks exactly this. */
        private var lastUserText: String? = null

        /** Language of the last routed reply; extraction follows it. */
        private var lastLanguage: MindLanguage = MindLanguage.EN
        private var lastSampleAtMillis = 0L

        /** Consumed by the screen; null after handling. */
        val bodyEvents: StateFlow<BodyEvent?> = events.asStateFlow()

        private var wasCharging: Boolean? = null

        /** v0.6 (ideation №8): session floor of disk-free, for the cleanup joy. */
        private var lastDiskFreeFraction: Float? = null
        private var stormNotified = false
        private var fullFedNotified = false

        /** Context conversation starters, recomputed at screen start. */
        private val starters = MutableStateFlow<List<HomeStarter>>(emptyList())

        private val bodyState: StateFlow<BodyState> =
            combine(
                sensors.signals(notifEvents.countInWindow(System.currentTimeMillis())),
                weatherFeel.weather(),
                lastInteractionAt,
            ) { signals, weather, lastAt ->
                deriveState(signals.copy(weather = weather), lastAt)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BodyState.Resting)

        private data class MindBits(
            val status: MindStatus,
            val notice: MindFailure?,
            val tier: MindTier,
            val dream: Boolean,
            val englishFallback: Boolean,
        )

        val uiState: StateFlow<HomeUiState> =
            combine(
                bodyState,
                identityFlow(),
                chat.recent(CHAT_WINDOW),
                combine(
                    combine(streaming, candidates, ::Pair),
                    combine(mindStatus, mindNotice, mindTier, dreamAvailable, langFallback, ::MindBits),
                    ::Pair,
                ).map { (sc, bits) -> Quad(sc.first, sc.second, bits, Unit) },
                combine(soul.liveCount(), prefs.calmMotion(), prefs.personality(), starters) { g, calm, p, st ->
                    Quad(g, calm, p, st)
                },
            ) { body, id, messages, quad, extras ->
                // Evolution (ADR: Evolution.kt): stage + growth are pure
                // functions of the relationship, recomputed on every count
                // change — no stored stage to migrate or corrupt.
                val now = System.currentTimeMillis()
                val stats =
                    RelationshipStats(
                        hatchedAtMillis = id.hatchedAt,
                        conversationCount = id.conversationCount,
                        liveFactCount = extras.a,
                        chargeCount = 0,
                    )
                HomeUiState(
                    creatureName = id.name,
                    concept = id.concept,
                    seed = id.seed,
                    daysTogether = id.daysTogether,
                    bodyState = body,
                    messages = messages,
                    streamingReply = quad.a,
                    candidates = quad.b,
                    mindStatus = quad.c.status,
                    mindNotice = quad.c.notice,
                    growth = Evolution.growthOf(stats, now),
                    calmMotion = extras.b,
                    stage = Evolution.stageOf(stats, now),
                    personality = extras.c ?: Personality.presetFor(id.concept),
                    starters = extras.d,
                    activeTier = quad.c.tier,
                    dreamAvailable = quad.c.dream,
                    englishFallback = quad.c.englishFallback,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

        init {
            viewModelScope.launch { refreshMindBits() }
            viewModelScope.launch { notifEvents.prune(System.currentTimeMillis()) }
            viewModelScope.launch { observeBodyTransitions() }
            viewModelScope.launch { armVoice() }
            viewModelScope.launch { morningGreeting() }
            viewModelScope.launch { birthdayMoment() }
            viewModelScope.launch { computeStarters() }
            viewModelScope.launch { computeDreamAvailability() }
            viewModelScope.launch { computeGoodnight() }
            viewModelScope.launch { deliverDueCapsule() }
        }

        // --- v0.5 evening farewell (ideation-v5 №1) ---

        /** Evening, not said yet — the moon gesture shows. Never a demand. */
        val goodnightAvailable: StateFlow<Boolean> = goodnight.asStateFlow()

        private suspend fun computeGoodnight() {
            val zone = clock.zone()
            val nowInstant =
                java.time.Instant
                    .ofEpochMilli(clock.nowMillis())
                    .atZone(zone)
            val evening = nowInstant.hour >= EVENING_FROM || nowInstant.hour < NIGHT_UNTIL
            if (!evening) {
                goodnight.value = false
                return
            }
            // The evening began today at EVENING_FROM (or yesterday, if we're
            // past midnight) — one farewell per evening, keyed by that start.
            val eveningStart =
                nowInstant
                    .toLocalDate()
                    .minusDays(if (nowInstant.hour < NIGHT_UNTIL) 1 else 0)
                    .atTime(EVENING_FROM, 0)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
            goodnight.value = journal.countOfSince(JournalKind.GOODNIGHT, eveningStart) == 0
        }

        /**
         * The farewell ritual: a deterministic warm line, a journal moment,
         * nothing else. NOT saying goodnight records nothing and costs
         * nothing — no streaks, no guilt, by covenant (ideation-v5 №1).
         */
        fun sayGoodnight() {
            if (!goodnight.value) return
            goodnight.value = false
            viewModelScope.launch {
                val now = clock.nowMillis()
                journal.record(JournalKind.GOODNIGHT, now)
                // l10n: context-bound — the line lands in the chat DB at
                // creation time, in the locale of that moment (glossary §4).
                val pool = appContext.resources.getStringArray(R.array.home_goodnight_pool)
                val epochDay = now / RelationshipStats.DAY_MILLIS
                val seed = (identity.seed() ?: 0L) + epochDay
                val line = pool[(seed % pool.size).toInt().let { if (it < 0) it + pool.size else it }]
                chat.append(ChatRole.CREATURE, line, now)
                speakIfEnabled(line)
                onInteraction()
            }
        }

        // --- v0.5 time capsules (ideation-v5 №3) ---

        /** A letter that came due — the creature offers it once visible. */
        val dueCapsule: StateFlow<TimeCapsule?> = dueCapsuleState.asStateFlow()

        /** v0.6 (audit-v05 D1): the Soul screenshot toggle also governs
         * capsule display — a delivered letter is soul content. */
        val screenshotsAllowed: StateFlow<Boolean> =
            prefs
                .soulScreenshotsAllowed()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

        private suspend fun deliverDueCapsule() {
            dueCapsuleState.value = capsules.due(clock.nowMillis()).firstOrNull()
        }

        /** Owner read the letter: stamp it opened, offer the next if any. */
        fun openCapsule() {
            val capsule = dueCapsuleState.value ?: return
            viewModelScope.launch {
                val now = clock.nowMillis()
                capsules.markOpened(capsule.id, now)
                journal.record(JournalKind.CAPSULE_DELIVERED, now)
                deliverDueCapsule()
            }
        }

        /** Write a letter; the creature holds it for [horizonDays]. */
        fun writeCapsule(
            text: String,
            horizonDays: Int,
        ) {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return
            viewModelScope.launch {
                val now = System.currentTimeMillis()
                capsules.write(trimmed, now + horizonDays * RelationshipStats.DAY_MILLIS, now)
                onInteraction()
            }
        }

        /** ADR-013: offline-voice check once per screen life, only if on. */
        private suspend fun armVoice() {
            if (!voiceConfig.enabled.first()) return
            voiceReady = (
                voice.checkAvailability(java.util.Locale.getDefault())
                    is app.anima.core.voice.VoiceAvailability.Ready
            )
        }

        /** Speaks a creature line iff the toggle is on AND offline-verified. */
        private suspend fun speakIfEnabled(text: String) {
            if (!voiceReady || !voiceConfig.enabled.first()) return
            // v0.6 silence sense: a muted phone means "not out loud" — TTS
            // rides the media stream, which the ringer doesn't gate, so the
            // creature honors the intent itself.
            if (bodyState.value.signals.silenced) return
            val s = uiState.value
            voice.speak(
                text,
                app.anima.core.voice.VoiceCharacter.of(
                    s.concept,
                    s.personality.warmth,
                    s.personality.chattiness,
                ),
            )
        }

        /** Screen-off / navigation: the caller's duty per CreatureVoice KDoc. */
        fun stopVoice() = voice.stop()

        private suspend fun refreshMindBits() {
            mindStatus.value = mind.status()
            mindTier.value = runCatching { mindInventory.snapshot().activeTier }.getOrDefault(MindTier.NONE)
        }

        /**
         * Morning ritual: deterministic once-per-local-day trigger, executed
         * only with the screen open. The line itself is deterministic
         * (seed + day pick a template) — no inference, so it works asleep too.
         */
        private suspend fun morningGreeting() {
            val now = System.currentTimeMillis()
            val zone = java.time.ZoneId.systemDefault()
            val epochDay =
                java.time.Instant
                    .ofEpochMilli(now)
                    .atZone(zone)
                    .toLocalDate()
                    .toEpochDay()
            val hour =
                java.time.Instant
                    .ofEpochMilli(now)
                    .atZone(zone)
                    .hour
            if (hour !in MORNING_FROM until MORNING_UNTIL) return
            if (!prefs.shouldGreetToday(epochDay)) return
            prefs.markGreetedToday(epochDay)
            // l10n: context-bound — appended to the chat DB at creation time
            // in the current locale; history is never repainted (glossary §4).
            val pool = appContext.resources.getStringArray(R.array.home_morning_pool)
            val seed = (identity.seed() ?: 0L) + epochDay
            val line = pool[(seed % pool.size).toInt().let { if (it < 0) it + pool.size else it }]
            chat.append(ChatRole.CREATURE, line, now)
            speakIfEnabled(line)
        }

        /**
         * v0.3 dreams: at night the creature sleeps (MoodEngine); waking it
         * once per night earns a deterministic dream from yesterday's journal
         * (DreamWeaver — templates, zero inference).
         */
        private suspend fun computeDreamAvailability() {
            val zone = java.time.ZoneId.systemDefault()
            val nowInstant =
                java.time.Instant
                    .ofEpochMilli(System.currentTimeMillis())
                    .atZone(zone)
            val night = MoodEngine.isNight(nowInstant.hour * MINUTES_PER_HOUR + nowInstant.minute)
            if (!night) {
                dreamAvailable.value = false
                return
            }
            // A "night" is identified by the day it BEGAN (23:00 belongs to
            // today, 02:00 to yesterday) — one dream per night, not per day.
            dreamAvailable.value = prefs.shouldDreamTonight(nightKey(nowInstant))
        }

        fun wakeForDream() {
            viewModelScope.launch {
                val zone = java.time.ZoneId.systemDefault()
                val now = System.currentTimeMillis()
                val nowInstant =
                    java.time.Instant
                        .ofEpochMilli(now)
                        .atZone(zone)
                val key = nightKey(nowInstant)
                if (!prefs.shouldDreamTonight(key)) return@launch
                prefs.markDreamTold(key)
                dreamAvailable.value = false
                val dayStart = now - RelationshipStats.DAY_MILLIS
                val echo =
                    app.anima.core.model.DreamWeaver.DayEcho(
                        charges = journal.countOfSince(JournalKind.CHARGE_START, dayStart),
                        storms = journal.countOfSince(JournalKind.NOTIF_STORM, dayStart),
                        ranHot = journal.countOfSince(JournalKind.RAN_HOT, dayStart),
                        fullyFed = journal.countOfSince(JournalKind.FULLY_FED, dayStart) > 0,
                    )
                val dream =
                    app.anima.core.model.DreamWeaver
                        .weave(identity.seed() ?: 0L, key, echo)
                chat.append(ChatRole.CREATURE, dream, now)
                journal.record(JournalKind.DREAM_TOLD, now)
            }
        }

        private fun nightKey(now: java.time.ZonedDateTime): Long {
            val day = now.toLocalDate().toEpochDay()
            return if (now.hour < MoodEngine.NIGHT_END_MINUTE / MINUTES_PER_HOUR) day - 1 else day
        }

        /**
         * v0.3: the hatch anniversary, once per year, any hour of the day.
         * Deterministic line; a Story moment lands in the journal too.
         */
        private suspend fun birthdayMoment() {
            val zone = java.time.ZoneId.systemDefault()
            val hatchedAt = identity.hatchedAtMillis() ?: return
            val today =
                java.time.Instant
                    .ofEpochMilli(System.currentTimeMillis())
                    .atZone(zone)
                    .toLocalDate()
            val hatched =
                java.time.Instant
                    .ofEpochMilli(hatchedAt)
                    .atZone(zone)
                    .toLocalDate()
            val years = today.year - hatched.year
            if (years < 1 || today.dayOfMonth != hatched.dayOfMonth || today.month != hatched.month) return
            if (!prefs.shouldCelebrateBirthday(today.year.toLong())) return
            prefs.markBirthdayCelebrated(today.year.toLong())
            val now = System.currentTimeMillis()
            // l10n: context-bound — a chat line minted once a year, in the
            // locale of that morning (glossary §4).
            val line = appContext.resources.getQuantityString(R.plurals.home_birthday, years, years)
            chat.append(ChatRole.CREATURE, line, now)
            journal.record(JournalKind.BIRTHDAY, now, "$years")
            events.value = BodyEvent.CELEBRATE_CHARGE
        }

        /** Deterministic starters from the body diary — data, not inference. */
        private suspend fun computeStarters() {
            val now = System.currentTimeMillis()
            val dayStart = now - RelationshipStats.DAY_MILLIS
            val chargesToday = journal.countOfSince(JournalKind.CHARGE_START, dayStart)
            val stormsToday = journal.countOfSince(JournalKind.NOTIF_STORM, dayStart)
            val hotToday = journal.countOfSince(JournalKind.RAN_HOT, dayStart)
            val list = mutableListOf<HomeStarter>()
            if (chargesToday >= FREQUENT_CHARGES) list += HomeStarter.FedTimes(chargesToday)
            if (stormsToday > 0) list += HomeStarter.StormAsk
            if (hotToday > 0) list += HomeStarter.RanHot
            list += HomeStarter.RememberToday
            // v0.3 (product-research §1, Tolan pattern): the creature nudges
            // toward the real world instead of hoarding attention.
            list += HomeStarter.AnythingGood
            starters.value = list.take(MAX_STARTERS)
        }

        /** Body diary + visible reactions; runs only while the app is on screen. */
        private suspend fun observeBodyTransitions() {
            bodyState.collect { state ->
                val now = System.currentTimeMillis()
                // v0.3 diary chart: foreground-only battery samples (no new
                // background entities). Sparse by design; the chart says so.
                if (now - lastSampleAtMillis >= SAMPLE_EVERY_MILLIS) {
                    lastSampleAtMillis = now
                    journal.record(JournalKind.BODY_SAMPLE, now, "${state.signals.batteryPercent}")
                }
                val charging = state.signals.charging
                val was = wasCharging
                if (was != null && charging != was) {
                    if (charging) {
                        journal.record(JournalKind.CHARGE_START, now)
                        events.value = BodyEvent.CELEBRATE_CHARGE
                    } else {
                        journal.record(JournalKind.CHARGE_STOP, now)
                    }
                }
                wasCharging = charging
                // Charge festival: the creature celebrates a full belly, once
                // per charge session (anti-nag: no repeat while it stays full).
                val full = charging && state.signals.batteryPercent >= FULL_BATTERY
                if (full && !fullFedNotified) {
                    fullFedNotified = true
                    journal.record(JournalKind.FULLY_FED, now)
                    events.value = BodyEvent.CELEBRATE_CHARGE
                }
                if (!charging) fullFedNotified = false
                val storm = state.signals.notifRecentCount >= MoodEngine.STORM_NOTIF_COUNT
                if (storm && !stormNotified) {
                    stormNotified = true
                    journal.record(JournalKind.NOTIF_STORM, now, "${state.signals.notifRecentCount}")
                    events.value = BodyEvent.STARTLE_STORM
                }
                if (!storm) stormNotified = false
                // v0.6 (ideation №8): the owner freed real space — the
                // burrow got roomier. Positive loop only: shrinking space
                // says nothing (the anxious mood already covers scarcity).
                val free = state.signals.diskFreeFraction
                val base = lastDiskFreeFraction
                if (base != null && free - base >= BURROW_CLEAN_DELTA) {
                    journal.record(
                        JournalKind.BURROW_CLEANED,
                        now,
                        "${((free - base) * PERCENT).toInt()}",
                    )
                    val pool = appContext.resources.getStringArray(R.array.home_burrow_pool)
                    val seed = (identity.seed() ?: 0L) + now / RelationshipStats.DAY_MILLIS
                    val line = pool[(seed % pool.size).toInt().let { if (it < 0) it + pool.size else it }]
                    chat.append(ChatRole.CREATURE, line, now)
                    events.value = BodyEvent.BURROW_ROOMIER
                }
                // Track the session floor so one big cleanup fires once:
                // the baseline only ratchets up after celebrating.
                if (base == null || free < base || free - base >= BURROW_CLEAN_DELTA) {
                    lastDiskFreeFraction = free
                }
            }
        }

        fun onBodyEventHandled() {
            events.value = null
        }

        fun onInteraction() {
            lastInteractionAt.value = System.currentTimeMillis()
        }

        /** ON_STOP: drop the ~1 GB Gemma engine; it reloads lazily on return. */
        fun onAppBackgrounded() {
            // Finite episodes: speech never outlives the visible screen.
            voice.stop()
            viewModelScope.launch { mind.releaseResources() }
        }

        /** ON_RESUME: the Mind screen may have installed/deleted a model. */
        fun refreshMindStatus() {
            viewModelScope.launch {
                refreshMindBits()
                computeDreamAvailability()
            }
        }

        /**
         * v0.6 Play GenAI policy (research-v6 §B.5): the owner can flag an
         * AI reply as offensive/wrong without leaving the app. The reply
         * is deleted from the thread and a REPLY_FLAGGED moment lands in
         * the body journal — locally, like everything else.
         */
        fun reportReply(messageId: String) {
            viewModelScope.launch {
                chat.remove(messageId)
                journal.record(JournalKind.REPLY_FLAGGED, clock.nowMillis())
            }
        }

        /**
         * v0.3: ask the same question again — a fresh roll of the same dice.
         * The old reply stays in the thread (append-only chat, like the soul).
         */
        fun regenerate() {
            val text = lastUserText ?: return
            if (streaming.value != null) return
            viewModelScope.launch { reply(text, reAppendUserLine = false) }
        }

        /**
         * v0.3: long-press "remember this" — the message becomes a fact
         * CANDIDATE and walks through the exact same confirmation gate as
         * extraction; nothing enters the soul without the explicit yes.
         */
        fun rememberThis(message: ChatMessage) {
            val text = message.text.take(FactJson.MAX_FACT_CHARS)
            candidates.value =
                (candidates.value + FactCandidate(app.anima.core.model.FactCategory.OTHER, text))
                    .distinctBy { it.text }
        }

        fun send(text: String) {
            val trimmed = text.trim()
            if (trimmed.isEmpty() || streaming.value != null) return
            onInteraction()
            viewModelScope.launch { reply(trimmed, reAppendUserLine = true) }
        }

        private suspend fun reply(
            trimmed: String,
            reAppendUserLine: Boolean,
        ) {
            run {
                val now = System.currentTimeMillis()
                lastUserText = trimmed
                if (reAppendUserLine) chat.append(ChatRole.USER, trimmed, now)
                // Budget follows the tier that will actually speak (ADR-005):
                // Gemma's 2048-token window is much tighter than Nano's, and
                // the cloud tier (ADR-011) gets real context room.
                val snapshot = runCatching { mindInventory.snapshot() }.getOrNull()
                val tier = snapshot?.activeTier ?: MindTier.NONE
                mindTier.value = tier
                // Phase 1D: honest language choice — UI language when the
                // active model genuinely speaks it, else English + badge.
                val spec = snapshot?.gemmaModel?.let(MindModelRegistry::specFor)
                val decision = MindLanguageRouting.decide(uiLanguage(), tier, spec)
                langFallback.value = decision.showBadge
                lastLanguage = decision.language
                val prompt =
                    PromptBuilder.build(
                        creatureName = uiState.value.creatureName.ifEmpty { "Anima" },
                        state = bodyState.value,
                        facts = soul.liveFacts().first(),
                        dialogue = uiState.value.messages,
                        userMessage = trimmed,
                        budgetChars = PromptBuilder.budgetFor(tier, spec),
                        personality = uiState.value.personality,
                        language = decision.language,
                        // v0.6 silence sense: muted phone → whispered reply.
                        silenced = bodyState.value.signals.silenced,
                    )
                streaming.value = ""
                mind.reply(prompt).collect { event ->
                    when (event) {
                        is MindEvent.Chunk -> streaming.value = (streaming.value ?: "") + event.text
                        is MindEvent.Done -> {
                            streaming.value = null
                            if (event.fullText.isNotBlank()) {
                                chat.append(ChatRole.CREATURE, event.fullText.trim(), System.currentTimeMillis())
                                speakIfEnabled(event.fullText.trim())
                                proposeFacts(trimmed, event.fullText)
                            }
                        }
                        is MindEvent.Failed -> {
                            streaming.value = null
                            // Closed set: the UI maps MindFailure onto
                            // localized resources at display time.
                            mindNotice.value = event.reason
                        }
                    }
                }
            }
        }

        private suspend fun proposeFacts(
            userText: String,
            creatureText: String,
        ) {
            // Facts come back for confirmation — they must read natively.
            val found = mind.extractFactCandidates(userText, creatureText, lastLanguage)
            if (found.isNotEmpty()) candidates.value = found
        }

        /** Interface language = the app's resolved locale (Phase 1D). */
        private fun uiLanguage(): MindLanguage =
            MindLanguage.fromTag(
                java.util.Locale
                    .getDefault()
                    .toLanguageTag(),
            )

        fun confirmCandidate(candidate: FactCandidate) {
            viewModelScope.launch {
                soul.remember(candidate.category, candidate.text, FactSource.CHAT_CONFIRMED, System.currentTimeMillis())
                candidates.value = candidates.value - candidate
            }
        }

        fun rejectCandidate(candidate: FactCandidate) {
            candidates.value = candidates.value - candidate
        }

        fun dismissMindNotice() {
            mindNotice.value = null
        }

        fun requestMindDownload() {
            viewModelScope.launch {
                mindStatus.value = MindStatus.DOWNLOADING
                mind.requestDownload()
                mindStatus.value = mind.status()
            }
        }

        private fun deriveState(
            signals: BodySignals,
            lastInteraction: Long,
        ): BodyState {
            val idle = (System.currentTimeMillis() - lastInteraction).coerceAtLeast(0)
            return BodyState(signals, MoodEngine.derive(signals, idle))
        }

        private data class IdentitySnapshot(
            val name: String,
            val concept: CreatureConcept,
            val seed: Long,
            val daysTogether: Long,
            val hatchedAt: Long,
            val conversationCount: Int,
        )

        private fun identityFlow() =
            combine(
                identity.observeName(),
                identity.observeConcept(),
                chat.userMessageCount(),
            ) { name, concept, conversations ->
                val now = System.currentTimeMillis()
                val stats = identity.stats(now)
                IdentitySnapshot(
                    name = name.orEmpty(),
                    concept = concept ?: CreatureConcept.SPIRIT_ORB,
                    seed = identity.seed() ?: 0L,
                    daysTogether = stats.daysTogether(now),
                    hatchedAt = stats.hatchedAtMillis,
                    conversationCount = conversations,
                )
            }

        private data class Quad<A, B, C, D>(
            val a: A,
            val b: B,
            val c: C,
            val d: D,
        )

        private companion object {
            const val CHAT_WINDOW = 60
            const val FULL_BATTERY = 100

            // v0.6 (ideation №8): ≥5% of total capacity freed = a real
            // cleanup, not file-churn noise.
            const val BURROW_CLEAN_DELTA = 0.05f
            const val PERCENT = 100f
            const val MORNING_FROM = 5
            const val MORNING_UNTIL = 12
            const val FREQUENT_CHARGES = 3
            const val MAX_STARTERS = 3
            const val MINUTES_PER_HOUR = 60
            const val SAMPLE_EVERY_MILLIS = 10L * 60 * 1000
            const val EVENING_FROM = 21
            const val NIGHT_UNTIL = 3
        }
    }
