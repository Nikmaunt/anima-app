package app.anima.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.anima.core.data.repo.JournalRepository
import app.anima.core.model.InstalledMindModel
import app.anima.core.model.JournalKind
import app.anima.core.model.MindEngine
import app.anima.core.model.MindInventory
import app.anima.core.model.MindStatus
import app.anima.core.model.MindTier
import app.anima.core.modeldelivery.DeliveryEvent
import app.anima.core.modeldelivery.DeliveryFailure
import app.anima.core.modeldelivery.MindModelDownloader
import app.anima.core.modeldelivery.MindModelImporter
import app.anima.core.modeldelivery.MindModelStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MindUiState(
    val tier: MindTier = MindTier.NONE,
    val nano: MindStatus = MindStatus.ASLEEP,
    val model: InstalledMindModel? = null,
    val freeBytes: Long = 0L,
    val busy: Boolean = false,
    val importing: Boolean = false,
    val progress: Pair<Long, Long?>? = null,
    val notice: String? = null,
    val urlDraft: String = "",
    val shaDraft: String = "",
    val allowMetered: Boolean = false,
)

@HiltViewModel
class MindViewModel
    @Inject
    constructor(
        private val inventory: MindInventory,
        private val mind: MindEngine,
        private val store: MindModelStore,
        private val downloader: MindModelDownloader,
        private val importer: MindModelImporter,
        private val journal: JournalRepository,
    ) : ViewModel() {
        private val state = MutableStateFlow(MindUiState())
        val uiState: StateFlow<MindUiState> = state.asStateFlow()

        private var transfer: Job? = null

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                val snapshot = inventory.snapshot()
                state.value =
                    state.value.copy(
                        tier = snapshot.activeTier,
                        nano = snapshot.nano,
                        model = snapshot.gemmaModel,
                        freeBytes = store.freeBytes(),
                    )
            }
        }

        fun onUrlChange(value: String) {
            state.value = state.value.copy(urlDraft = value)
        }

        fun onShaChange(value: String) {
            state.value = state.value.copy(shaDraft = value)
        }

        fun onAllowMeteredChange(value: Boolean) {
            state.value = state.value.copy(allowMetered = value)
        }

        fun download() {
            if (transfer?.isActive == true) return
            val url = state.value.urlDraft.trim()
            if (url.isEmpty()) {
                state.value = state.value.copy(notice = "Paste a direct https link to the model file first.")
                return
            }
            transfer =
                viewModelScope.launch {
                    state.value = state.value.copy(busy = true, importing = false, progress = null)
                    downloader
                        .download(
                            url = url,
                            expectedSha256 =
                                state.value.shaDraft
                                    .trim()
                                    .ifEmpty { null },
                            allowMetered = state.value.allowMetered,
                        ).collect(::onDeliveryEvent)
                }
        }

        fun importModel(uri: Uri) {
            if (transfer?.isActive == true) return
            transfer =
                viewModelScope.launch {
                    state.value = state.value.copy(busy = true, importing = true, progress = null)
                    importer.import(uri).collect(::onDeliveryEvent)
                }
        }

        fun deleteModel() {
            viewModelScope.launch {
                mind.releaseResources()
                store.deleteInstalled()
                state.value = state.value.copy(notice = "The mind file is gone. The creature stays.")
                refresh()
            }
        }

        fun dismissNotice() {
            state.value = state.value.copy(notice = null)
        }

        private fun onDeliveryEvent(event: DeliveryEvent) {
            when (event) {
                is DeliveryEvent.Progress ->
                    state.value = state.value.copy(progress = event.bytesDone to event.bytesTotal)

                is DeliveryEvent.Done -> {
                    state.value =
                        state.value.copy(
                            busy = false,
                            progress = null,
                            notice = "The mind is here. Say hello.",
                        )
                    // "My mind woke up" — a moment on the Our-story timeline.
                    viewModelScope.launch {
                        journal.record(JournalKind.MIND_AWAKENED, System.currentTimeMillis())
                    }
                    refresh()
                }

                is DeliveryEvent.Failed -> {
                    state.value =
                        state.value.copy(
                            busy = false,
                            progress = null,
                            notice = failureText(event),
                        )
                    refresh()
                }
            }
        }

        private fun failureText(event: DeliveryEvent.Failed): String =
            when (event.reason) {
                DeliveryFailure.NEEDS_WIFI ->
                    "Waiting for Wi-Fi — it's a big file. Flip the switch to use mobile data."
                DeliveryFailure.NOT_HTTPS -> "Only direct https:// links work here."
                DeliveryFailure.HTTP_ERROR ->
                    "The server refused (${event.detail ?: "no detail"}). Official Gemma links " +
                        "need a license acceptance — download in the browser and use " +
                        "\"Choose mind file\" instead."
                DeliveryFailure.INTERRUPTED ->
                    "The transfer broke off. Try again — it resumes where it stopped."
                DeliveryFailure.CHECKSUM_MISMATCH ->
                    "The file failed its integrity check and was discarded. " +
                        "(got ${event.detail?.take(SHA_PREVIEW) ?: "?"}…)"
                DeliveryFailure.NOT_A_MODEL ->
                    "That doesn't look like a mind file (.task / .litertlm, hundreds of MB)."
                DeliveryFailure.NO_SPACE -> "Not enough free space for the mind file."
            }

        private companion object {
            const val SHA_PREVIEW = 12
        }
    }
