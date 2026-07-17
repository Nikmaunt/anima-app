package app.anima.core.modeldelivery

import android.content.Context
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Honest phase of the Play-delivered model pack (ADR-010). */
sealed interface PackPhase {
    /** No Play delivery here (sideload/debug) or state not yet known. */
    data object Absent : PackPhase

    data class Downloading(
        val bytesDone: Long,
        val bytesTotal: Long,
    ) : PackPhase

    /** Play wants explicit user consent for a large/cellular download. */
    data object WaitingForConsent : PackPhase

    data class Ready(
        val model: InstalledPackModel,
    ) : PackPhase

    data class Failed(
        val errorCode: Int,
    ) : PackPhase
}

data class InstalledPackModel(
    val fileName: String,
    val sizeBytes: Long,
    val path: String,
)

/**
 * The `mind_pack` fast-follow asset pack, as a StateFlow. Play fetches the
 * pack by itself after a store install; this class only OBSERVES state and
 * locates the delivered file — the single push-button ([requestFetch]) exists
 * for the WAITING_FOR_WIFI / sideload-then-store-install edges. All Play
 * Core failures degrade to [PackPhase.Absent]: a phone without Play (or a
 * plain debug APK) simply behaves like v0.2.
 */
@Singleton
class PackModelSource
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val manager: AssetPackManager? =
            runCatching { AssetPackManagerFactory.getInstance(context) }.getOrNull()

        private val state = MutableStateFlow<PackPhase>(PackPhase.Absent)
        val phase: StateFlow<PackPhase> = state.asStateFlow()

        private val listener =
            AssetPackStateUpdateListener { packState ->
                if (packState.name() == PACK_NAME) state.value = phaseOf(packState)
            }

        /** Call once at app start (and on Mind-screen open) — cheap, async. */
        fun refresh() {
            val mgr = manager ?: return
            // A completed pack is visible synchronously via its location.
            locateModelFile()?.let {
                state.value = PackPhase.Ready(it)
                return
            }
            mgr.registerListener(listener)
            mgr
                .getPackStates(listOf(PACK_NAME))
                .addOnSuccessListener { states ->
                    states.packStates()[PACK_NAME]?.let { state.value = phaseOf(it) }
                }.addOnFailureListener {
                    // No Play / no bundle install: honest absence, not an error.
                    state.value = PackPhase.Absent
                }
        }

        /** WAITING_FOR_WIFI or user-visible retry; Play shows its own UI. */
        fun requestFetch() {
            manager?.fetch(listOf(PACK_NAME))
        }

        private fun phaseOf(packState: AssetPackState): PackPhase =
            when (packState.status()) {
                AssetPackStatus.COMPLETED ->
                    locateModelFile()?.let { PackPhase.Ready(it) } ?: PackPhase.Absent
                AssetPackStatus.DOWNLOADING, AssetPackStatus.TRANSFERRING, AssetPackStatus.PENDING ->
                    PackPhase.Downloading(packState.bytesDownloaded(), packState.totalBytesToDownload())
                AssetPackStatus.REQUIRES_USER_CONFIRMATION, AssetPackStatus.WAITING_FOR_WIFI ->
                    PackPhase.WaitingForConsent
                AssetPackStatus.FAILED -> PackPhase.Failed(packState.errorCode())
                else -> PackPhase.Absent
            }

        private fun locateModelFile(): InstalledPackModel? {
            val mgr = manager ?: return null
            val location = runCatching { mgr.getPackLocation(PACK_NAME) }.getOrNull() ?: return null
            val assetsPath = location.assetsPath() ?: return null
            return File(assetsPath)
                .listFiles()
                ?.filter {
                    it.isFile &&
                        it.extension in MindModelStore.MODEL_EXTENSIONS &&
                        it.length() >= MindModelStore.MIN_MODEL_BYTES
                }?.maxByOrNull { it.length() }
                ?.let { InstalledPackModel(it.name, it.length(), it.absolutePath) }
        }

        companion object {
            const val PACK_NAME = "mind_pack"
        }
    }
