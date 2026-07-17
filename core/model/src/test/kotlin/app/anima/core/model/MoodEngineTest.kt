package app.anima.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MoodEngineTest {
    private val base = BodySignals.Resting

    @Test
    fun `resting daylight is alert`() {
        assertThat(MoodEngine.derive(base, interactionIdleMillis = 0)).isEqualTo(Mood.ALERT)
    }

    @Test
    fun `charging is eating`() {
        val s = base.copy(charging = true, chargeKind = ChargeKind.AC)
        assertThat(MoodEngine.derive(s, 0)).isEqualTo(Mood.EATING)
    }

    @Test
    fun `low battery is sleepy`() {
        val s = base.copy(batteryPercent = 15)
        assertThat(MoodEngine.derive(s, 0)).isEqualTo(Mood.SLEEPY)
    }

    @Test
    fun `thermal throttling wins over everything`() {
        val s =
            base.copy(
                thermal = ThermalSense.HOT,
                charging = true,
                batteryPercent = 5,
                net = NetSense.OFFLINE,
            )
        assertThat(MoodEngine.derive(s, 0)).isEqualTo(Mood.HOT)
    }

    @Test
    fun `eating beats sleepy - creature wakes up to eat`() {
        val s = base.copy(charging = true, batteryPercent = 5)
        assertThat(MoodEngine.derive(s, 0)).isEqualTo(Mood.EATING)
    }

    @Test
    fun `offline is anxious`() {
        assertThat(MoodEngine.derive(base.copy(net = NetSense.OFFLINE), 0)).isEqualTo(Mood.ANXIOUS)
    }

    @Test
    fun `disk squeeze is anxious`() {
        val s = base.copy(diskFreeBytes = 1L * 1024 * 1024 * 1024) // ~0.8% of 128G
        assertThat(MoodEngine.derive(s, 0)).isEqualTo(Mood.ANXIOUS)
    }

    @Test
    fun `notification storm is anxious`() {
        val s = base.copy(notifRecentCount = MoodEngine.STORM_NOTIF_COUNT)
        assertThat(MoodEngine.derive(s, 0)).isEqualTo(Mood.ANXIOUS)
    }

    @Test
    fun `night is asleep`() {
        assertThat(MoodEngine.derive(base.copy(minuteOfDay = 23 * 60 + 30), 0)).isEqualTo(Mood.ASLEEP)
        assertThat(MoodEngine.derive(base.copy(minuteOfDay = 3 * 60), 0)).isEqualTo(Mood.ASLEEP)
        assertThat(MoodEngine.derive(base.copy(minuteOfDay = 7 * 60), 0)).isEqualTo(Mood.ALERT)
    }

    @Test
    fun `anxious wins over asleep - a storm wakes it`() {
        val s = base.copy(minuteOfDay = 2 * 60, notifRecentCount = 10)
        assertThat(MoodEngine.derive(s, 0)).isEqualTo(Mood.ANXIOUS)
    }

    @Test
    fun `ignored long enough is bored`() {
        assertThat(MoodEngine.derive(base, MoodEngine.BORED_AFTER_MILLIS)).isEqualTo(Mood.BORED)
        assertThat(MoodEngine.derive(base, MoodEngine.BORED_AFTER_MILLIS - 1)).isEqualTo(Mood.ALERT)
    }

    @Test
    fun `sleepy beats anxious - conserve first`() {
        val s = base.copy(batteryPercent = 10, net = NetSense.OFFLINE)
        assertThat(MoodEngine.derive(s, 0)).isEqualTo(Mood.SLEEPY)
    }
}
