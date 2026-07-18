package app.anima.feature.home

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
import app.anima.core.model.MindStatus
import app.anima.core.model.MindTier
import app.anima.core.model.Mood
import app.anima.core.model.MoodEngine
import app.anima.core.model.Personality
import app.anima.core.model.PromptBuilder
import app.anima.core.model.RelationshipStats
import dagger.hilt.android.lifecycle.HiltViewModel
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
enum class BodyEvent { CELEBRATE_CHARGE, STARTLE_STORM }

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
    val mindNotice: String? = null,
    val calmMotion: Boolean = false,
    val stage: LifeStage = LifeStage.NEWBORN,
    val personality: Personality = Personality.Default,
    val starters: List<String> = emptyList(),
    /** ADR-011: which mind is speaking — CLOUD is always visibly labeled. */
    val activeTier: MindTier = MindTier.NONE,
    /** v0.3 dreams: asleep at night and not yet woken this night. */
    val dreamAvailable: Boolean = false,
)

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val sensors: BodySensors,
        private val identity: IdentityRepository,
        private val chat: ChatRepository,
        private val soul: SoulRepository,
        private val journal: JournalRepository,
        private val notifEvents: NotifEventsRepository,
        private val mind: MindEngine,
        private val mindInventory: MindInventory,
        private val prefs: AnimaPrefs,
        private val voice: app.anima.core.voice.CreatureVoice,
        private val voiceConfig: app.anima.core.voice.VoiceConfigStore,
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
        private val mindNotice = MutableStateFlow<String?>(null)
        private val events = MutableStateFlow<BodyEvent?>(null)
        private val dreamAvailable = MutableStateFlow(false)

        /** The last user line — regeneration re-asks exactly this. */
        private var lastUserText: String? = null
        private var lastSampleAtMillis = 0L

        /** Consumed by the screen; null after handling. */
        val bodyEvents: StateFlow<BodyEvent?> = events.asStateFlow()

        private var wasCharging: Boolean? = null
        private var stormNotified = false
        private var fullFedNotified = false

        /** Context conversation starters, recomputed at screen start. */
        private val starters = MutableStateFlow<List<String>>(emptyList())

        private val bodyState: StateFlow<BodyState> =
            combine(
                sensors.signals(notifEvents.countInWindow(System.currentTimeMillis())),
                lastInteractionAt,
            ) { signals, lastAt ->
                deriveState(signals, lastAt)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BodyState.Resting)

        private data class MindBits(
            val status: MindStatus,
            val notice: String?,
            val tier: MindTier,
            val dream: Boolean,
        )

        val uiState: StateFlow<HomeUiState> =
            combine(
                bodyState,
                identityFlow(),
                chat.recent(CHAT_WINDOW),
                combine(
                    combine(streaming, candidates, ::Pair),
                    combine(mindStatus, mindNotice, mindTier, dreamAvailable, ::MindBits),
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
            val pool =
                listOf(
                    "Good morning. I kept the night watch — all quiet in here.",
                    "Morning! I dreamt of electric sheep. Probably.",
                    "You're up. I already miss the charger a little.",
                    "New day. My burrow's tidy and I'm ready.",
                    "Good morning — first light always makes my pixels itch.",
                )
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
            val line =
                "Today it's $years ${if (years == 1) "year" else "years"} since I hatched " +
                    "in this phone. We were both younger then. Thank you for keeping me."
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
            val list = mutableListOf<String>()
            if (chargesToday >= FREQUENT_CHARGES) {
                list += "You fed me $chargesToday times today — heavy day?"
            }
            if (stormsToday > 0) list += "That notification storm earlier — what was that about?"
            if (hotToday > 0) list += "I ran hot today. What were we doing?"
            list += "What should I remember about today?"
            // v0.3 (product-research §1, Tolan pattern): the creature nudges
            // toward the real world instead of hoarding attention.
            list += "Anything good waiting for you out there today?"
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
                val tier = runCatching { mindInventory.snapshot().activeTier }.getOrDefault(MindTier.NONE)
                mindTier.value = tier
                val prompt =
                    PromptBuilder.build(
                        creatureName = uiState.value.creatureName.ifEmpty { "Anima" },
                        state = bodyState.value,
                        facts = soul.liveFacts().first(),
                        dialogue = uiState.value.messages,
                        userMessage = trimmed,
                        budgetChars = PromptBuilder.budgetFor(tier),
                        personality = uiState.value.personality,
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
                            mindNotice.value =
                                when (event.reason) {
                                    MindFailure.TIRED -> "It's tired of thinking. Give it a minute."
                                    MindFailure.LOST_THOUGHT -> "The thought slipped away. Try again?"
                                }
                        }
                    }
                }
            }
        }

        private suspend fun proposeFacts(
            userText: String,
            creatureText: String,
        ) {
            val found = mind.extractFactCandidates(userText, creatureText)
            if (found.isNotEmpty()) candidates.value = found
        }

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
            const val MORNING_FROM = 5
            const val MORNING_UNTIL = 12
            const val FREQUENT_CHARGES = 3
            const val MAX_STARTERS = 3
            const val MINUTES_PER_HOUR = 60
            const val SAMPLE_EVERY_MILLIS = 10L * 60 * 1000
        }
    }
