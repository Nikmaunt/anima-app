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
import app.anima.core.model.FactCandidate
import app.anima.core.model.FactSource
import app.anima.core.model.JournalKind
import app.anima.core.model.MindEngine
import app.anima.core.model.MindEvent
import app.anima.core.model.MindFailure
import app.anima.core.model.MindStatus
import app.anima.core.model.Mood
import app.anima.core.model.MoodEngine
import app.anima.core.model.PromptBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sensors: BodySensors,
    private val identity: IdentityRepository,
    private val chat: ChatRepository,
    private val soul: SoulRepository,
    private val journal: JournalRepository,
    private val notifEvents: NotifEventsRepository,
    private val mind: MindEngine,
    private val prefs: AnimaPrefs,
) : ViewModel() {

    private val lastInteractionAt = MutableStateFlow(System.currentTimeMillis())
    private val streaming = MutableStateFlow<String?>(null)
    private val candidates = MutableStateFlow<List<FactCandidate>>(emptyList())
    private val mindStatus = MutableStateFlow(MindStatus.ASLEEP)
    private val mindNotice = MutableStateFlow<String?>(null)
    private val events = MutableStateFlow<BodyEvent?>(null)

    /** Consumed by the screen; null after handling. */
    val bodyEvents: StateFlow<BodyEvent?> = events.asStateFlow()

    private var wasCharging: Boolean? = null
    private var stormNotified = false

    private val bodyState: StateFlow<BodyState> =
        combine(
            sensors.signals(notifEvents.countInWindow(System.currentTimeMillis())),
            lastInteractionAt,
        ) { signals, lastAt ->
            deriveState(signals, lastAt)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BodyState.Resting)

    val uiState: StateFlow<HomeUiState> =
        combine(
            bodyState,
            identityFlow(),
            chat.recent(CHAT_WINDOW),
            combine(streaming, candidates, mindStatus, mindNotice) { s, c, m, n -> Quad(s, c, m, n) },
            combine(soul.liveCount(), prefs.calmMotion()) { g, calm -> g to calm },
        ) { body, id, messages, quad, growthCalm ->
            HomeUiState(
                creatureName = id.name,
                concept = id.concept,
                seed = id.seed,
                daysTogether = id.daysTogether,
                bodyState = body,
                messages = messages,
                streamingReply = quad.a,
                candidates = quad.b,
                mindStatus = quad.c,
                mindNotice = quad.d,
                growth = (growthCalm.first / GROWTH_FULL_FACTS).coerceIn(0f, 1f),
                calmMotion = growthCalm.second,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch { mindStatus.value = mind.status() }
        viewModelScope.launch { notifEvents.prune(System.currentTimeMillis()) }
        viewModelScope.launch { observeBodyTransitions() }
    }

    /** Body diary + visible reactions; runs only while the app is on screen. */
    private suspend fun observeBodyTransitions() {
        bodyState.collect { state ->
            val now = System.currentTimeMillis()
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

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || streaming.value != null) return
        onInteraction()
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            chat.append(ChatRole.USER, trimmed, now)
            val prompt = PromptBuilder.build(
                creatureName = uiState.value.creatureName.ifEmpty { "Anima" },
                state = bodyState.value,
                facts = soul.liveFacts().first(),
                dialogue = uiState.value.messages,
                userMessage = trimmed,
            )
            streaming.value = ""
            mind.reply(prompt).collect { event ->
                when (event) {
                    is MindEvent.Chunk -> streaming.value = (streaming.value ?: "") + event.text
                    is MindEvent.Done -> {
                        streaming.value = null
                        if (event.fullText.isNotBlank()) {
                            chat.append(ChatRole.CREATURE, event.fullText.trim(), System.currentTimeMillis())
                            proposeFacts(trimmed, event.fullText)
                        }
                    }
                    is MindEvent.Failed -> {
                        streaming.value = null
                        mindNotice.value = when (event.reason) {
                            MindFailure.TIRED -> "It's tired of thinking. Give it a minute."
                            MindFailure.LOST_THOUGHT -> "The thought slipped away. Try again?"
                        }
                    }
                }
            }
        }
    }

    private suspend fun proposeFacts(userText: String, creatureText: String) {
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

    private fun deriveState(signals: BodySignals, lastInteraction: Long): BodyState {
        val idle = (System.currentTimeMillis() - lastInteraction).coerceAtLeast(0)
        return BodyState(signals, MoodEngine.derive(signals, idle))
    }

    private data class IdentitySnapshot(
        val name: String,
        val concept: CreatureConcept,
        val seed: Long,
        val daysTogether: Long,
    )

    private fun identityFlow() = combine(
        identity.observeName(),
        identity.observeConcept(),
    ) { name, concept ->
        IdentitySnapshot(
            name = name.orEmpty(),
            concept = concept ?: CreatureConcept.SPIRIT_ORB,
            seed = identity.seed() ?: 0L,
            daysTogether = identity.stats(System.currentTimeMillis())
                .daysTogether(System.currentTimeMillis()),
        )
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    private companion object {
        const val CHAT_WINDOW = 60
        const val GROWTH_FULL_FACTS = 40f
    }
}
