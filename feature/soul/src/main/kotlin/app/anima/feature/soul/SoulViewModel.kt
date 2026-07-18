package app.anima.feature.soul

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.anima.core.data.backup.SoulBackup
import app.anima.core.data.backup.SoulBackupCodec
import app.anima.core.data.prefs.AnimaPrefs
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
    val concept: CreatureConcept = CreatureConcept.SPIRIT_ORB,
    val seed: Long = 0L,
    val stats: RelationshipStats? = null,
    val importCandidates: List<FactCandidate> = emptyList(),
    val importText: String = "",
    val query: String = "",
    val categoryFilter: FactCategory? = null,
    val editing: SoulFact? = null,
    val editDraft: String = "",
    val backupNotice: BackupNotice? = null,
)

/**
 * Closed set of backup/migration outcomes. The UI maps each case onto
 * localized resources; [ExportFailed]/[ImportFailed] carry an optional
 * technical detail that rides inside the localized sentence as-is.
 */
sealed interface BackupNotice {
    data class PassphraseTooShort(
        val minChars: Int,
    ) : BackupNotice

    data object ExportDone : BackupNotice

    data class ExportFailed(
        val detail: String?,
    ) : BackupNotice

    data class ImportDone(
        val creatureName: String,
        val imported: Int,
        val skipped: Int,
    ) : BackupNotice

    data object WrongPassphraseOrCorrupt : BackupNotice

    data class ImportFailed(
        val detail: String?,
    ) : BackupNotice
}

@HiltViewModel
class SoulViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val soul: SoulRepository,
        private val identity: IdentityRepository,
        private val journal: JournalRepository,
        private val backup: SoulBackup,
        private val prefs: AnimaPrefs,
    ) : ViewModel() {
        /** FLAG_SECURE gate for this screen (threat-model.md; default ON). */
        val screenshotsAllowed: StateFlow<Boolean> =
            prefs
                .soulScreenshotsAllowed()
                .stateIn(viewModelScope, SharingStarted.Eagerly, false)

        private val importCandidates = MutableStateFlow<List<FactCandidate>>(emptyList())
        private val importText = MutableStateFlow("")
        private val stats = MutableStateFlow<RelationshipStats?>(null)
        private val query = MutableStateFlow("")
        private val categoryFilter = MutableStateFlow<FactCategory?>(null)
        private val editing = MutableStateFlow<Pair<SoulFact, String>?>(null)
        private val backupNotice = MutableStateFlow<BackupNotice?>(null)
        private val identityBits = MutableStateFlow(CreatureConcept.SPIRIT_ORB to 0L)

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
                        concept = identityBits.value.first,
                        seed = identityBits.value.second,
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
            val notice: BackupNotice?,
        )

        init {
            viewModelScope.launch {
                stats.value = identity.stats(System.currentTimeMillis())
                identityBits.value =
                    (identity.concept() ?: CreatureConcept.SPIRIT_ORB) to (identity.seed() ?: 0L)
            }
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
                // Plaintext export residue (audit-v03 F2): previous exports
                // are removed before writing a new one, and AnimaApp sweeps
                // this dir on every app start.
                dir.listFiles()?.forEach { it.delete() }
                val file = File(dir, "soul-of-${name.lowercase().replace(Regex("[^a-zа-яё0-9]+"), "-")}.md")
                file.writeText(markdown)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                val intent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/markdown"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        // l10n: context-bound — share-sheet extras leave the app at creation time.
                        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.soul_export_subject, name))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                onReady(Intent.createChooser(intent, context.getString(R.string.soul_export_chooser)))
            }
        }

        /**
         * v0.4 postcard (reinstated from the v0.3 cut): one still frame of
         * the creature + a caption, through the system share sheet. No
         * social SDKs — ACTION_SEND and nothing else. The PNG lands in the
         * same cache/exports dir the F2 sweep cleans.
         */
        fun buildPostcardIntent(onReady: (Intent) -> Unit) {
            viewModelScope.launch {
                val now = System.currentTimeMillis()
                val name = identity.name() ?: "Anima"
                val (concept, seed) = identityBits.value
                val liveStats = identity.stats(now)
                val shift =
                    app.anima.core.model.Milestones.effectiveShiftDeg(
                        prefs.paletteVariant().first(),
                        liveStats,
                        now,
                    )
                val hour =
                    java.util.Calendar
                        .getInstance()
                        .get(java.util.Calendar.HOUR_OF_DAY)
                val night = hour >= 22 || hour < 7
                val tile =
                    app.anima.core.creature.render.StillRender.tile(
                        concept = concept,
                        seed = seed,
                        mood = app.anima.core.model.Mood.ALERT,
                        batteryPercent = 80,
                        charging = false,
                        night = night,
                        growth = 0.5f,
                        sizePx = POSTCARD_TILE_PX,
                        paletteShiftDeg = shift,
                    )
                val card =
                    android.graphics.Bitmap
                        .createBitmap(POSTCARD_W, POSTCARD_H, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(card)
                canvas.drawColor(if (night) NIGHT_BG else DAY_BG)
                canvas.drawBitmap(tile, (POSTCARD_W - POSTCARD_TILE_PX) / 2f, TILE_TOP, null)
                val ink = if (night) INK_NIGHT else INK_DAY
                val paint =
                    android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = ink
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = NAME_TEXT_PX
                    }
                canvas.drawText(name, POSTCARD_W / 2f, NAME_Y, paint)
                paint.textSize = CAPTION_TEXT_PX
                paint.alpha = CAPTION_ALPHA
                // l10n: context-bound — the caption is baked into the PNG at
                // creation time (current locale), like a diary entry.
                val days = liveStats.daysTogether(now).toInt()
                canvas.drawText(
                    context.resources.getQuantityString(R.plurals.soul_postcard_caption, days, days),
                    POSTCARD_W / 2f,
                    CAPTION_Y,
                    paint,
                )
                paint.textAlign = android.graphics.Paint.Align.RIGHT
                paint.textSize = MARK_TEXT_PX
                paint.alpha = MARK_ALPHA
                canvas.drawText("Anima", POSTCARD_W - MARK_MARGIN, POSTCARD_H - MARK_MARGIN, paint)

                val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                val file = File(dir, "postcard-of-${name.lowercase().replace(Regex("[^a-zа-яё0-9]+"), "-")}.png")
                file.outputStream().use { out ->
                    card.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                val intent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                // l10n: context-bound — chooser title for the system share sheet.
                onReady(Intent.createChooser(intent, context.getString(R.string.soul_postcard_chooser)))
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
                backupNotice.value = BackupNotice.PassphraseTooShort(MIN_PASSPHRASE)
                return
            }
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val payload = backup.exportPayload(System.currentTimeMillis())
                    val sealed = SoulBackupCodec.seal(payload, passphrase.toCharArray())
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(sealed) }
                        ?: error("cannot open the chosen location")
                }.onSuccess {
                    backupNotice.value = BackupNotice.ExportDone
                }.onFailure {
                    backupNotice.value = BackupNotice.ExportFailed(it.message)
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
                        BackupNotice.ImportDone(
                            creatureName = summary.creatureName,
                            imported = summary.factsImported,
                            skipped = summary.factsSkipped,
                        )
                    stats.value = identity.stats(System.currentTimeMillis())
                }.onFailure {
                    backupNotice.value =
                        if (it is SoulBackupCodec.WrongPassphraseOrCorrupt) {
                            BackupNotice.WrongPassphraseOrCorrupt
                        } else {
                            BackupNotice.ImportFailed(it.message)
                        }
                }
            }
        }

        fun dismissBackupNotice() {
            backupNotice.value = null
        }

        private companion object {
            const val MIN_PASSPHRASE = 8

            // Postcard geometry (v0.4): 4:5 portrait, creature centered.
            const val POSTCARD_W = 1080
            const val POSTCARD_H = 1350
            const val POSTCARD_TILE_PX = 720
            const val TILE_TOP = 180f
            const val NAME_Y = 1060f
            const val CAPTION_Y = 1130f
            const val NAME_TEXT_PX = 72f
            const val CAPTION_TEXT_PX = 44f
            const val MARK_TEXT_PX = 36f
            const val CAPTION_ALPHA = 170
            const val MARK_ALPHA = 90
            const val MARK_MARGIN = 40f
            const val NIGHT_BG = 0xFF0B0E14.toInt()
            const val DAY_BG = 0xFFF4F0E9.toInt()
            const val INK_NIGHT = 0xFFE8E4DC.toInt()
            const val INK_DAY = 0xFF23262E.toInt()
        }
    }
