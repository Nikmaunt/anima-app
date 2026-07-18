package app.anima.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.anima.core.cloudmind.CloudMindConfigStore
import app.anima.core.cloudmind.CloudMindEngine
import app.anima.core.cloudmind.KeyProbeResult
import app.anima.core.data.repo.JournalRepository
import app.anima.core.model.CloudMindConfig
import app.anima.core.model.CloudPreset
import app.anima.core.model.InstalledMindModel
import app.anima.core.model.JournalKind
import app.anima.core.model.MindEngine
import app.anima.core.model.MindInventory
import app.anima.core.model.MindLanguage
import app.anima.core.model.MindLanguageRouting
import app.anima.core.model.MindModelRegistry
import app.anima.core.model.MindModelSpec
import app.anima.core.model.MindStatus
import app.anima.core.model.MindTier
import app.anima.core.modeldelivery.DeliveryEvent
import app.anima.core.modeldelivery.DeliveryFailure
import app.anima.core.modeldelivery.MindModelDownloader
import app.anima.core.modeldelivery.MindModelImporter
import app.anima.core.modeldelivery.MindModelResolver
import app.anima.core.modeldelivery.MindModelStore
import app.anima.core.modeldelivery.PackModelSource
import app.anima.core.modeldelivery.PackPhase
import app.anima.core.modeldelivery.RamGate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class MindUiState(
    val tier: MindTier = MindTier.NONE,
    val nano: MindStatus = MindStatus.ASLEEP,
    val model: InstalledMindModel? = null,
    // v0.5 (ADR-017): everything switchable, each with its registry spec.
    val models: List<Pair<InstalledMindModel, MindModelSpec>> = emptyList(),
    /** The owner's explicit pick (file name); null = automatic order. */
    val selected: String? = null,
    /** Phase 1D honesty row: what language the active mind really speaks. */
    val uiLanguage: MindLanguage = MindLanguage.EN,
    val routing: MindLanguageRouting.Decision? = null,
    val freeBytes: Long = 0L,
    val busy: Boolean = false,
    val importing: Boolean = false,
    val progress: Pair<Long, Long?>? = null,
    val notice: String? = null,
    val urlDraft: String = "",
    val shaDraft: String = "",
    val allowMetered: Boolean = false,
    // ADR-010: Play pack + RAM gate.
    val packPhase: PackPhase = PackPhase.Absent,
    val ramGateAllows: Boolean = true,
    val forceGemma: Boolean = false,
    // ADR-011: cloud mind. The key itself never appears here.
    val cloud: CloudMindConfig = CloudMindConfig.Disabled,
    val cloudUrlDraft: String = "",
    val cloudModelDraft: String = "",
    /** Phase 1E: the "Check key" probe is in flight. */
    val checkingKey: Boolean = false,
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
        private val pack: PackModelSource,
        private val resolver: MindModelResolver,
        private val ramGate: RamGate,
        private val cloudStore: CloudMindConfigStore,
        private val cloudMind: CloudMindEngine,
    ) : ViewModel() {
        private val state = MutableStateFlow(MindUiState())
        val uiState: StateFlow<MindUiState> = state.asStateFlow()

        private var transfer: Job? = null

        init {
            pack.refresh()
            refresh()
            viewModelScope.launch {
                combine(
                    pack.phase,
                    resolver.forceGemma,
                    cloudStore.config,
                    resolver.available,
                    resolver.selectedModel,
                ) { phase, force, cloud, available, selected ->
                    state.value =
                        state.value.copy(
                            packPhase = phase,
                            ramGateAllows = ramGate.allowsSpontaneousGemma(),
                            forceGemma = force,
                            cloud = cloud,
                            cloudUrlDraft = state.value.cloudUrlDraft.ifEmpty { cloud.baseUrl },
                            cloudModelDraft = state.value.cloudModelDraft.ifEmpty { cloud.model },
                            models = available.map { it to MindModelRegistry.specFor(it) },
                            selected = selected,
                        )
                    refresh()
                }.collect {}
            }
        }

        fun refresh() {
            viewModelScope.launch {
                val snapshot = inventory.snapshot()
                val uiLanguage = MindLanguage.fromTag(Locale.getDefault().toLanguageTag())
                state.value =
                    state.value.copy(
                        tier = snapshot.activeTier,
                        nano = snapshot.nano,
                        model = snapshot.gemmaModel,
                        uiLanguage = uiLanguage,
                        routing =
                            MindLanguageRouting.decide(
                                uiLanguage = uiLanguage,
                                tier = snapshot.activeTier,
                                localSpec = snapshot.gemmaModel?.let(MindModelRegistry::specFor),
                            ),
                        freeBytes = store.freeBytes(),
                    )
            }
        }

        // --- ADR-017: multi-model selection ---

        /** Owner taps a mind card; null = back to the automatic order. */
        fun selectModel(fileName: String?) {
            viewModelScope.launch {
                resolver.setSelectedModel(fileName)
                refresh()
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

        /** Per-model deletion (v0.5). Pack models never reach this path. */
        fun deleteModel(fileName: String) {
            viewModelScope.launch {
                mind.releaseResources()
                store.deleteModel(fileName)
                state.value = state.value.copy(notice = "The mind file is gone. The creature stays.")
                refresh()
            }
        }

        fun dismissNotice() {
            state.value = state.value.copy(notice = null)
        }

        // --- ADR-010: pack + RAM gate ---

        /** WAITING_FOR_WIFI / REQUIRES_USER_CONFIRMATION: ask Play to go on. */
        fun requestPackFetch() = pack.requestFetch()

        fun setForceGemma(value: Boolean) {
            viewModelScope.launch {
                resolver.setForceGemma(value)
                refresh()
            }
        }

        // --- ADR-011: cloud mind ---

        fun onCloudUrlChange(value: String) {
            state.value = state.value.copy(cloudUrlDraft = value)
        }

        fun onCloudModelChange(value: String) {
            state.value = state.value.copy(cloudModelDraft = value)
        }

        /** Phase 1E: a preset chip fills the drafts — the owner still saves. */
        fun applyPreset(preset: CloudPreset) {
            state.value =
                state.value.copy(
                    cloudUrlDraft = preset.baseUrl,
                    cloudModelDraft = preset.defaultModel,
                )
        }

        /** Phase 1E: one explicit probe, no conversation content sent. */
        fun checkKey() {
            if (state.value.checkingKey) return
            viewModelScope.launch {
                state.value = state.value.copy(checkingKey = true)
                val result = cloudMind.probeKey()
                state.value =
                    state.value.copy(
                        checkingKey = false,
                        notice =
                            when (result) {
                                KeyProbeResult.OK -> "The key works."
                                KeyProbeResult.BAD_KEY -> "The provider rejected this key."
                                KeyProbeResult.UNREACHABLE -> "Could not reach the provider."
                                KeyProbeResult.NOT_CONFIGURED -> "Save the endpoint and key first."
                            },
                    )
            }
        }

        /** Key comes through here once, straight into the Keystore wrap. */
        fun saveCloudSetup(key: String) {
            viewModelScope.launch {
                val url = state.value.cloudUrlDraft.trim()
                if (!url.startsWith("https://")) {
                    state.value = state.value.copy(notice = "Cloud endpoints must start with https://")
                    return@launch
                }
                cloudStore.setEndpoint(url, state.value.cloudModelDraft)
                if (key.isNotBlank()) cloudStore.storeKey(key.trim().encodeToByteArray())
                refresh()
            }
        }

        fun setCloudEnabled(value: Boolean) {
            viewModelScope.launch {
                cloudStore.setEnabled(value)
                refresh()
            }
        }

        fun forgetCloudKey() {
            viewModelScope.launch {
                cloudStore.clearKeyAndDisable()
                state.value = state.value.copy(notice = "Cloud key erased. The mind is local again.")
                refresh()
            }
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
