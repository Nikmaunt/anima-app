package app.anima.core.body

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import app.anima.core.model.BodySignals
import app.anima.core.model.ChargeKind
import app.anima.core.model.NetSense
import app.anima.core.model.ThermalSense
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The creature's senses (research.md §D): battery, storage, connectivity
 * transport, thermal status, clock — all without a single permission dialog.
 *
 * No background polling exists: every callbackFlow registers its receiver on
 * collection and unregisters in awaitClose, so sensing runs exactly while the
 * UI (or an on-screen recorder) is subscribed — the "life only on screen"
 * constraint holds by structured concurrency, not by discipline.
 */
@Singleton
class BodySensors @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Battery level + charge kind, seeded by the sticky broadcast. */
    fun battery(): Flow<Battery> = callbackFlow {
        fun parse(intent: Intent?): Battery {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val percent = if (level >= 0 && scale > 0) level * 100 / scale else 50
            val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, 0) ?: 0
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL && plugged != 0
            val kind = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> ChargeKind.AC
                BatteryManager.BATTERY_PLUGGED_USB -> ChargeKind.USB
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargeKind.WIRELESS
                else -> ChargeKind.NONE
            }
            return Battery(percent, charging && plugged != 0, kind)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                trySend(parse(intent))
            }
        }
        val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        trySend(parse(sticky))
        awaitClose { context.unregisterReceiver(receiver) }
    }.conflate()

    /** Transport kind only — never SSID, never location. */
    fun net(): Flow<NetSense> = callbackFlow {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        fun senseOf(caps: NetworkCapabilities?): NetSense = when {
            caps == null -> NetSense.OFFLINE
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetSense.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetSense.CELLULAR
            else -> NetSense.OTHER
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(senseOf(caps))
            }

            override fun onLost(network: Network) {
                trySend(NetSense.OFFLINE)
            }
        }
        trySend(senseOf(cm.getNetworkCapabilities(cm.activeNetwork)))
        cm.registerDefaultNetworkCallback(callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    /** Thermal status; UNKNOWN when the device doesn't report (advisory). */
    fun thermal(): Flow<ThermalSense> = callbackFlow {
        val pm = context.getSystemService(PowerManager::class.java)
        fun map(status: Int): ThermalSense = when (status) {
            PowerManager.THERMAL_STATUS_NONE, PowerManager.THERMAL_STATUS_LIGHT -> ThermalSense.CALM
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalSense.WARM
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalSense.HOT
            else -> ThermalSense.BURNING
        }

        val listener = PowerManager.OnThermalStatusChangedListener { status -> trySend(map(status)) }
        val initial = runCatching { pm.currentThermalStatus }.getOrNull()
        trySend(initial?.let(::map) ?: ThermalSense.UNKNOWN)
        val registered = runCatching { pm.addThermalStatusListener(listener) }.isSuccess
        awaitClose { if (registered) runCatching { pm.removeThermalStatusListener(listener) } }
    }.distinctUntilChanged()

    /** Minute-of-day heartbeat: ACTION_TIME_TICK while subscribed (1/min). */
    fun minuteOfDay(): Flow<Int> = callbackFlow {
        fun now(): Int {
            val cal = Calendar.getInstance()
            return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                trySend(now())
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        trySend(now())
        awaitClose { context.unregisterReceiver(receiver) }
    }.conflate()

    /** Sampled cheap reads, refreshed by the minute heartbeat + battery events. */
    private fun snapshot(): Snapshot {
        val stat = StatFs(context.filesDir.path)
        val am = context.getSystemService(android.app.ActivityManager::class.java)
        val mem = android.app.ActivityManager.MemoryInfo()
        runCatching { am.getMemoryInfo(mem) }
        return Snapshot(
            diskFree = stat.availableBytes,
            diskTotal = stat.totalBytes,
            lowMemory = mem.lowMemory,
            awakeMillis = SystemClock.uptimeMillis(),
            sinceBootMillis = SystemClock.elapsedRealtime(),
        )
    }

    /**
     * The combined interoception stream. [notifRecentCount] comes from the
     * notification repository (0 when the sense is off) so a storm can make
     * the creature anxious.
     */
    fun signals(notifRecentCount: Flow<Int>): Flow<BodySignals> =
        combine(battery(), net(), thermal(), minuteOfDay(), notifRecentCount) {
                battery, net, thermal, minute, notifCount ->
            val snap = snapshot()
            BodySignals(
                batteryPercent = battery.percent,
                charging = battery.charging,
                chargeKind = battery.kind,
                net = net,
                thermal = thermal,
                diskFreeBytes = snap.diskFree,
                diskTotalBytes = snap.diskTotal,
                lowMemory = snap.lowMemory,
                awakeMillis = snap.awakeMillis,
                sinceBootMillis = snap.sinceBootMillis,
                minuteOfDay = minute,
                notifRecentCount = notifCount,
            )
        }.distinctUntilChanged()

    data class Battery(val percent: Int, val charging: Boolean, val kind: ChargeKind)

    private data class Snapshot(
        val diskFree: Long,
        val diskTotal: Long,
        val lowMemory: Boolean,
        val awakeMillis: Long,
        val sinceBootMillis: Long,
    )
}
