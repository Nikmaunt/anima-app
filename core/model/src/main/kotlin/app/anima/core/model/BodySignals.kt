package app.anima.core.model

/** How the creature is being fed right now. */
enum class ChargeKind { NONE, AC, USB, WIRELESS }

/** What the creature can "hear" of the world. Transport kind only — never SSID. */
enum class NetSense { OFFLINE, WIFI, CELLULAR, OTHER }

/**
 * The creature's temperature sense, mapped from PowerManager thermal statuses.
 * UNKNOWN means the device doesn't report thermal state (the sense is absent,
 * not "cool") — some OEMs return NONE even under throttling, so consumers
 * treat this as advisory.
 */
enum class ThermalSense { UNKNOWN, CALM, WARM, HOT, BURNING }

/**
 * Raw interoception: everything the creature can feel about the phone body
 * without a single permission dialog. Produced by :core:body, consumed by the
 * mood engine and the renderer. Plain data — no derivation here.
 *
 * @property minuteOfDay local wall-clock minute 0..1439; day/night is derived
 *   from clock bands because a permissionless sunrise API does not exist.
 * @property notifRecentCount notifications captured in the trailing storm
 *   window (0 when the notification sense is off).
 */
data class BodySignals(
    val batteryPercent: Int,
    val charging: Boolean,
    val chargeKind: ChargeKind,
    val net: NetSense,
    val thermal: ThermalSense,
    val diskFreeBytes: Long,
    val diskTotalBytes: Long,
    val lowMemory: Boolean,
    val awakeMillis: Long,
    val sinceBootMillis: Long,
    val minuteOfDay: Int,
    val notifRecentCount: Int,
) {
    /** 0.0 (nothing free) .. 1.0 (empty burrow). */
    val diskFreeFraction: Float
        get() = if (diskTotalBytes <= 0L) 1f else diskFreeBytes.toFloat() / diskTotalBytes.toFloat()

    companion object {
        /** A neutral daylight resting state; previews and tests start here. */
        val Resting =
            BodySignals(
                batteryPercent = 80,
                charging = false,
                chargeKind = ChargeKind.NONE,
                net = NetSense.WIFI,
                thermal = ThermalSense.CALM,
                diskFreeBytes = 64L * 1024 * 1024 * 1024,
                diskTotalBytes = 128L * 1024 * 1024 * 1024,
                lowMemory = false,
                awakeMillis = 4L * 60 * 60 * 1000,
                sinceBootMillis = 12L * 60 * 60 * 1000,
                minuteOfDay = 14 * 60,
                notifRecentCount = 0,
            )
    }
}
