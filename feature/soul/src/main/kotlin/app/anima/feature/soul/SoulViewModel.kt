package app.anima.feature.soul

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.anima.core.data.backup.SoulBackup
import app.anima.core.data.backup.SoulBackupCodec
import app.anima.core.data.repo.IdentityRepository
import app.anima.core.data.repo.JournalRepository
import app.anima.core.data.repo.SoulRepository
import app.anima.core.model.CreatureConcept
import app.anima.core.model.FactCandidate
import app.anima.core.model.FactCategory
import app.anima.core.model.FactSource
import app.anima.core.model.RelationshipStats
import app.anima.core.model.SoulFact
import app.anima.core.model.SoulPort
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SoulUiState(
    val facts: List<SoulFact> = emptyList(),
    val stats: RelationshipStats? = null,
    val importCandidates: List<FactCandidate> = emptyList(),
    val importText: String = "",
    val query: String = "",
    val categoryFilter: FactCategory? = null,
    val editing: SoulFact? = null,
    val editDraft: String = "",
    val backupNotice: String? = null,
)

@HiltViewModel
class SoulViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val soul: SoulRepository,
        private val identity: IdentityRepository,
        private val journal: JournalRepository,
        private val backup: SoulBackup,
    ) : ViewModel() {
        private val importCandidates = MutableStateFlow<List<FactCandidate>>(emptyList())
        private val importText = MutableStateFlow("")
        private val stats = MutableStateFlow<RelationshipStats?>(null)
        private val query = MutableStateFlow("")
        private val categoryFilter = MutableStateFlow<FactCategory?>(null)
        private val editing = MutableStateFlow<Pair<SoulFact, String>?>(null)
        private val backupNotice = MutableStateFlow<String?>(null)

        val uiState: StateFlow<SoulUiState> =
            kotlinx.coroutines.flow
                .combine(
                    soul.liveFacts(),
                    kotlinx.coroutines.flow
                        .combine(importCandidates, importText, stats) { c, t, s -> Triple(c, t, s) },
                    kotlinx.coroutines.flow
                        .combine(query, categoryFilter, editing, backupNotice) { q, f, e, n ->
                            SearchAndEdit(q, f, e, n)
                        },
                ) { facts, imports, search ->
                    // Memory browser (Soul v2): search + category filter run
                    // in memory over the already-loaded live list — at soul
                    // scale (hundreds of rows) this beats any FTS plumbing
                    // (ADR-008 records why FTS5 was rejected).
                    val filtered =
                        facts
                            .filter { search.filter == null || it.category == search.filter }
                            .filter {
                                search.query.isBlank() ||
                                    it.text.contains(search.query.trim(), ignoreCase = true)
                            }
                    SoulUiState(
                        facts = filtered,
                        stats = imports.third,
                        importCandidates = imports.first,
                        importText = imports.second,
                        query = search.query,
                        categoryFilter = search.filter,
                        editing = search.editing?.first,
                        editDraft = search.editing?.second.orEmpty(),
                        backupNotice = search.notice,
                    )
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SoulUiState())

        private data class SearchAndEdit(
            val query: String,
            val filter: FactCategory?,
            val editing: Pair<SoulFact, String>?,
            val notice: String?,
        )

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
                val markdown =
                    SoulPort.export(
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
                val intent =
                    Intent(Intent.ACTION_SEND).apply {
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
                soul.remember(
                    candidate.category,
                    candidate.text,
                    FactSource.IMPORT_CONFIRMED,
                    System.currentTimeMillis(),
                )
                importCandidates.value = importCandidates.value - candidate
                stats.value = identity.stats(System.currentTimeMillis())
            }
        }

        fun rejectImport(candidate: FactCandidate) {
            importCandidates.value = importCandidates.value - candidate
        }

        // --- Soul v2: browser, edit-by-supersede, encrypted migration ---

        fun onQueryChange(value: String) {
            query.value = value
        }

        fun onCategoryFilter(value: FactCategory?) {
            categoryFilter.value = value
        }

        fun startEdit(fact: SoulFact) {
            editing.value = fact to fact.text
        }

        fun onEditDraftChange(value: String) {
            editing.value = editing.value?.copy(second = value)
        }

        fun cancelEdit() {
            editing.value = null
        }

        /** Append-only correction: the old row is superseded, never rewritten. */
        fun confirmEdit() {
            val (fact, draft) = editing.value ?: return
            if (draft.isBlank() || draft.trim() == fact.text) {
                editing.value = null
                return
            }
            viewModelScope.launch {
                soul.replace(fact.id, fact.category, draft, fact.source, System.currentTimeMillis())
                editing.value = null
                stats.value = identity.stats(System.currentTimeMillis())
            }
        }

        /** Soul migration: whole soul → one passphrase-sealed file at [uri]. */
        fun exportBackup(
            uri: android.net.Uri,
            passphrase: String,
        ) {
            if (passphrase.length < MIN_PASSPHRASE) {
                backupNotice.value = "Use at least $MIN_PASSPHRASE characters — this file protects everything."
                return
            }
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val payload = backup.exportPayload(System.currentTimeMillis())
                    val sealed = SoulBackupCodec.seal(payload, passphrase.toCharArray())
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(sealed) }
                        ?: error("cannot open the chosen location")
                }.onSuccess {
                    backupNotice.value = "Soul sealed and saved. Keep the passphrase — there is no recovery."
                }.onFailure {
                    backupNotice.value = "Export failed: ${it.message ?: "unknown error"}"
                }
            }
        }

        /** Soul migration, arriving side. Additive; duplicates are skipped. */
        fun importBackup(
            uri: android.net.Uri,
            passphrase: String,
        ) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val blob =
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: error("cannot read the chosen file")
                    val payload = SoulBackupCodec.open(blob, passphrase.toCharArray())
                    backup.importPayload(payload)
                }.onSuccess { summary ->
                    backupNotice.value =
                        "${summary.creatureName} has moved in: ${summary.factsImported} memories arrived" +
                        (if (summary.factsSkipped > 0) ", ${summary.factsSkipped} already here" else "") + "."
                    stats.value = identity.stats(System.currentTimeMillis())
                }.onFailure {
                    backupNotice.value =
                        if (it is SoulBackupCodec.WrongPassphraseOrCorrupt) {
                            "Wrong passphrase, or the file is damaged. Nothing was changed."
                        } else {
                            "Import failed: ${it.message ?: "unknown error"}"
                        }
                }
            }
        }

        fun dismissBackupNotice() {
            backupNotice.value = null
        }

        private companion object {
            const val MIN_PASSPHRASE = 8
        }
    }
