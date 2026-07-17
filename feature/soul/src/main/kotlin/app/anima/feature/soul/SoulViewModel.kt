package app.anima.feature.soul

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.anima.core.data.repo.IdentityRepository
import app.anima.core.data.repo.JournalRepository
import app.anima.core.data.repo.SoulRepository
import app.anima.core.model.CreatureConcept
import app.anima.core.model.FactCandidate
import app.anima.core.model.FactSource
import app.anima.core.model.RelationshipStats
import app.anima.core.model.SoulFact
import app.anima.core.model.SoulPort
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SoulUiState(
    val facts: List<SoulFact> = emptyList(),
    val stats: RelationshipStats? = null,
    val importCandidates: List<FactCandidate> = emptyList(),
    val importText: String = "",
)

@HiltViewModel
class SoulViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val soul: SoulRepository,
    private val identity: IdentityRepository,
    private val journal: JournalRepository,
) : ViewModel() {

    private val importCandidates = MutableStateFlow<List<FactCandidate>>(emptyList())
    private val importText = MutableStateFlow("")
    private val stats = MutableStateFlow<RelationshipStats?>(null)

    val uiState: StateFlow<SoulUiState> = kotlinx.coroutines.flow.combine(
        soul.liveFacts(),
        importCandidates,
        importText,
        stats,
    ) { facts, candidates, text, s ->
        SoulUiState(facts = facts, stats = s, importCandidates = candidates, importText = text)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SoulUiState())

    init {
        viewModelScope.launch { stats.value = identity.stats(System.currentTimeMillis()) }
    }

    fun forget(fact: SoulFact) {
        viewModelScope.launch {
            soul.forget(fact.id, System.currentTimeMillis())
            stats.value = identity.stats(System.currentTimeMillis())
        }
    }

    /** The one road out: a single readable markdown file via the share sheet. */
    fun buildExportIntent(onReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val name = identity.name() ?: "Anima"
            val markdown = SoulPort.export(
                creatureName = name,
                concept = identity.concept() ?: CreatureConcept.SPIRIT_ORB,
                stats = identity.stats(now),
                facts = soul.liveFacts().first(),
                journal = journal.recent(SoulPort.MAX_EXPORT_MOMENTS).first(),
                nowMillis = now,
            )
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, "soul-of-${name.lowercase().replace(Regex("[^a-zа-яё0-9]+"), "-")}.md")
            file.writeText(markdown)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/markdown"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "The soul of $name")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            onReady(Intent.createChooser(intent, "Export soul"))
        }
    }

    fun extractorPrompt(): String = SoulPort.extractorPrompt()

    fun onImportTextChange(value: String) {
        importText.value = value
    }

    /** Parse only — nothing persists until each fact is confirmed by a tap. */
    fun parseImport() {
        importCandidates.value = SoulPort.parseCandidates(importText.value)
    }

    fun confirmImport(candidate: FactCandidate) {
        viewModelScope.launch {
            soul.remember(candidate.category, candidate.text, FactSource.IMPORT_CONFIRMED, System.currentTimeMillis())
            importCandidates.value = importCandidates.value - candidate
            stats.value = identity.stats(System.currentTimeMillis())
        }
    }

    fun rejectImport(candidate: FactCandidate) {
        importCandidates.value = importCandidates.value - candidate
    }
}
