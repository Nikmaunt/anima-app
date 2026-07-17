package app.anima.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Ported case-for-case from the hermes-lens OTP filter test suite intent. */
class NotifCaptureFilterTest {
    // --- looksLikeOtp ---

    @Test
    fun `classic otp is caught`() {
        assertThat(NotifCaptureFilter.looksLikeOtp("Verification code", "Your code is 482913")).isTrue()
        assertThat(NotifCaptureFilter.looksLikeOtp(null, "OTP: 4821")).isTrue()
        assertThat(NotifCaptureFilter.looksLikeOtp("Код подтверждения", "832145 — никому не сообщайте")).isTrue()
        assertThat(NotifCaptureFilter.looksLikeOtp("2FA", "Use 99120meow".replace("meow", ""))).isTrue()
    }

    @Test
    fun `bank transfer confirmation code is dropped even from allowlisted bank`() {
        assertThat(
            NotifCaptureFilter.looksLikeOtp("Перевод", "Код для подтверждения перевода: 5512"),
        ).isTrue()
    }

    @Test
    fun `keyword alone is kept - password changed no code`() {
        assertThat(NotifCaptureFilter.looksLikeOtp("Security", "Your password was changed")).isFalse()
    }

    @Test
    fun `digits alone are kept - parcel number`() {
        assertThat(NotifCaptureFilter.looksLikeOtp("Delivery", "Parcel 84213 arrived")).isFalse()
    }

    @Test
    fun `nine plus digit runs are not otp shaped`() {
        assertThat(NotifCaptureFilter.looksLikeOtp("Order", "Code for order 123456789 ready")).isFalse()
    }

    @Test
    fun `digits embedded in longer runs are not isolated`() {
        assertThat(NotifCaptureFilter.looksLikeOtp("Info", "code ref 12345678901")).isFalse()
    }

    @Test
    fun `null fields never throw`() {
        assertThat(NotifCaptureFilter.looksLikeOtp(null, null)).isFalse()
    }

    @Test
    fun `polish keyword works`() {
        assertThat(NotifCaptureFilter.looksLikeOtp("Twoje hasło", "Kod: 5567")).isTrue()
    }

    // --- gate chain ---

    private fun gate(
        enabled: Boolean = true,
        allowlist: Set<String> = setOf("com.example.app"),
        pkg: String? = "com.example.app",
        groupSummary: Boolean = false,
        ongoing: Boolean = false,
        title: String? = "Hello",
        text: String? = "World",
        storeText: Boolean = true,
    ) = NotifCaptureFilter.gate(enabled, allowlist, pkg, groupSummary, ongoing, title, text, storeText)

    @Test
    fun `happy path captures`() {
        val captured = gate()
        assertThat(captured).isNotNull()
        assertThat(captured!!.title).isEqualTo("Hello")
        assertThat(captured.text).isEqualTo("World")
    }

    @Test
    fun `disabled drops`() {
        assertThat(gate(enabled = false)).isNull()
    }

    @Test
    fun `not in allowlist drops`() {
        assertThat(gate(pkg = "com.other.app")).isNull()
        assertThat(gate(pkg = null)).isNull()
    }

    @Test
    fun `allowlist match is exact not prefix`() {
        assertThat(gate(pkg = "com.example.app.evil")).isNull()
    }

    @Test
    fun `group summary drops`() {
        assertThat(gate(groupSummary = true)).isNull()
    }

    @Test
    fun `ongoing drops`() {
        assertThat(gate(ongoing = true)).isNull()
    }

    @Test
    fun `otp drops whole notification even when allowlisted`() {
        assertThat(gate(title = "Code", text = "Your code is 4821")).isNull()
    }

    @Test
    fun `storeText false strips the body but keeps the event`() {
        val captured = gate(storeText = false)
        assertThat(captured).isNotNull()
        assertThat(captured!!.text).isNull()
    }

    @Test
    fun `title capped at 200 chars`() {
        val captured = gate(title = "x".repeat(500))
        assertThat(captured!!.title.length).isEqualTo(NotifCaptureFilter.TITLE_MAX_CHARS)
    }

    @Test
    fun `empty title and text drops`() {
        assertThat(gate(title = "  ", text = null)).isNull()
    }

    // --- package shape ---

    @Test
    fun `package name validation`() {
        assertThat(NotifCaptureFilter.looksLikePackageName("com.example.app")).isTrue()
        assertThat(NotifCaptureFilter.looksLikePackageName("org.a.b_c.d1")).isTrue()
        assertThat(NotifCaptureFilter.looksLikePackageName("nodots")).isFalse()
        assertThat(NotifCaptureFilter.looksLikePackageName("1com.example")).isFalse()
        assertThat(NotifCaptureFilter.looksLikePackageName("com..example")).isFalse()
    }

    // --- identity ---

    @Test
    fun `same content same id - repost dedups`() {
        val a = NotifIdentity.entryId("com.x", 1000L, "T", "B")
        val b = NotifIdentity.entryId("com.x", 1000L, "T", "B")
        val c = NotifIdentity.entryId("com.x", 1000L, "T", "C")
        assertThat(a).isEqualTo(b)
        assertThat(a).isNotEqualTo(c)
    }
}
