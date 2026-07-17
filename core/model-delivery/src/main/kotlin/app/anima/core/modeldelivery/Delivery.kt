package app.anima.core.modeldelivery

import app.anima.core.model.InstalledMindModel

/** Progress language shared by the downloader and the SAF importer. */
sealed interface DeliveryEvent {
    data class Progress(
        val bytesDone: Long,
        val bytesTotal: Long?,
    ) : DeliveryEvent

    data class Done(
        val model: InstalledMindModel,
    ) : DeliveryEvent

    data class Failed(
        val reason: DeliveryFailure,
        val detail: String? = null,
    ) : DeliveryEvent
}

enum class DeliveryFailure {
    /** Wi-Fi required but the active network is metered/cellular. */
    NEEDS_WIFI,

    /** Only https:// sources are accepted. */
    NOT_HTTPS,

    /** Server said no (auth-gated URL, 4xx/5xx). */
    HTTP_ERROR,

    /** Stream broke mid-way; staging file kept for resume. */
    INTERRUPTED,

    /** Finished file failed SHA-256 verification; staging deleted. */
    CHECKSUM_MISMATCH,

    /** Not a .task/.litertlm, or too small to be a real model. */
    NOT_A_MODEL,

    /** Not enough free space for the expected size. */
    NO_SPACE,
}
