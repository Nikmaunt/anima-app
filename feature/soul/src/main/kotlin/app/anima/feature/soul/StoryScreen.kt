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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.anima.core.data.repo.ChatRepository
import app.anima.core.data.repo.IdentityRepository
import app.anima.core.data.repo.JournalRepository
import app.anima.core.data.repo.SoulRepository
import app.anima.core.model.StoryMoment
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
            GhostButton("Back", onClick = onBack)
            Text(
                if (state.creatureName.isEmpty()) "Our story" else "Me and ${state.creatureName}",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
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
                        Text(moment.title, style = MaterialTheme.typography.bodyLarge)
                        moment.detail?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
