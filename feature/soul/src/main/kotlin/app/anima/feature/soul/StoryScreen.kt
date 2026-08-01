package app.anima.feature.soul

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.anima.core.data.repo.ChatRepository
import app.anima.core.data.repo.IdentityRepository
import app.anima.core.data.repo.JournalRepository
import app.anima.core.data.repo.SoulRepository
import app.anima.core.model.CreatureConcept
import app.anima.core.model.StoryMoment
import app.anima.core.model.StoryMomentKind
import app.anima.core.model.StoryTimeline
import app.anima.core.ui.components.GhostButton
import app.anima.core.ui.theme.LocalAnimaColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

data class StoryUiState(
    val creatureName: String = "",
    val moments: List<StoryMoment> = emptyList(),
    /** v1.1b task 1c: null until the identity row is read. */
    val concept: CreatureConcept? = null,
    val seed: Long? = null,
)

@HiltViewModel
class StoryViewModel
    @Inject
    constructor(
        private val identity: IdentityRepository,
        private val journal: JournalRepository,
        private val soul: SoulRepository,
        private val chat: ChatRepository,
    ) : ViewModel() {
        private val state = MutableStateFlow(StoryUiState())
        val uiState: StateFlow<StoryUiState> = state.asStateFlow()

        init {
            viewModelScope.launch {
                val now = System.currentTimeMillis()
                // Full journal for firsts (the recent() window is newest-first;
                // the timeline needs the OLDEST of each kind, so fetch wide).
                val journalAll = journal.recent(JOURNAL_WINDOW).first().sortedBy { it.atMillis }
                state.value =
                    StoryUiState(
                        creatureName = identity.name().orEmpty(),
                        concept = identity.concept(),
                        seed = identity.seed(),
                        moments =
                            StoryTimeline.build(
                                hatchedAtMillis = identity.hatchedAtMillis() ?: now,
                                journal = journalAll,
                                facts = soul.everything(),
                                conversationCount = chat.userMessageCount().first(),
                                nowMillis = now,
                            ),
                    )
            }
        }

        private companion object {
            const val JOURNAL_WINDOW = 10_000
        }
    }

/** "Our story" — the relationship as a quiet timeline. No gamification. */
@Composable
fun StoryScreen(
    onBack: () -> Unit,
    viewModel: StoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = LocalAnimaColors.current
    val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            GhostButton(stringResource(R.string.story_back), onClick = onBack)
            Text(
                if (state.creatureName.isEmpty()) {
                    stringResource(R.string.story_title)
                } else {
                    stringResource(R.string.story_title_named, state.creatureName)
                },
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            if (state.moments.isEmpty()) {
                item {
                    app.anima.core.creature.CreatureEmptyState(
                        concept = state.concept,
                        seed = state.seed,
                        line = stringResource(R.string.story_empty),
                        night = colors.isNight,
                    )
                }
            }
            items(state.moments) { moment ->
                Row(Modifier.padding(vertical = 10.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(colors.accent),
                        )
                    }
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(
                            dateFormat.format(Date(moment.atMillis)),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(momentTitle(moment), style = MaterialTheme.typography.bodyLarge)
                        momentDetail(moment)?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

/**
 * v1.1c defect D4. The timeline used to arrive from `core/model` as finished
 * English sentences — a JVM module with no `res/` was deciding wording, so all
 * six locales read "Hatched" and "First meal with you". A moment now arrives as
 * a kind and a number, and this is where it becomes a sentence.
 */
@Composable
private fun momentTitle(moment: StoryMoment): String =
    when (moment.kind) {
        StoryMomentKind.HATCHED -> stringResource(R.string.story_moment_hatched)
        StoryMomentKind.FIRST_CHARGE -> stringResource(R.string.story_moment_first_charge)
        StoryMomentKind.MIND_AWAKENED -> stringResource(R.string.story_moment_mind)
        StoryMomentKind.FIRST_STORM -> stringResource(R.string.story_moment_storm)
        StoryMomentKind.FIRST_MEMORY -> stringResource(R.string.story_moment_first_memory)
        StoryMomentKind.NTH_MEMORY ->
            pluralStringResource(
                R.plurals.story_moment_nth_memory,
                moment.amount ?: 0,
                moment.amount ?: 0,
            )
        StoryMomentKind.DAYS_TOGETHER ->
            pluralStringResource(
                R.plurals.story_moment_days_together,
                moment.amount ?: 0,
                moment.amount ?: 0,
            )
        StoryMomentKind.HUNDRED_CONVERSATIONS -> stringResource(R.string.story_moment_hundred_talks)
    }

@Composable
private fun momentDetail(moment: StoryMoment): String? =
    when (moment.kind) {
        StoryMomentKind.HATCHED -> stringResource(R.string.story_moment_hatched_detail)
        StoryMomentKind.FIRST_CHARGE -> stringResource(R.string.story_moment_first_charge_detail)
        StoryMomentKind.MIND_AWAKENED -> stringResource(R.string.story_moment_mind_detail)
        StoryMomentKind.FIRST_STORM ->
            moment.amount?.let {
                pluralStringResource(R.plurals.story_moment_storm_detail, it, it)
            }
        else -> null
    }
