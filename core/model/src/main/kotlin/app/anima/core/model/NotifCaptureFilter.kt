package app.anima.core.model

/**
 * Pure notification capture filter — the hermes-lens pattern re-implemented:
 * every gate runs BEFORE anything is persisted, and an OTP/2FA hit drops the
 * whole notification even from an allowlisted app. No Android imports, so the
 * heuristics are JVM-testable.
 */
object NotifCaptureFilter {
    const val TITLE_MAX_CHARS = 200
    const val BODY_MAX_CHARS = 1000

    /**
     * OTP keyword alternation (en/ru/pl + banking), substring on purpose:
     * `подтверж` catches every inflection, `verif` catches verify/verification.
     */
    private val otpKeyword =
        Regex(
            "code|код|kod|otp|2fa|verif|подтверж|pin|cvv|cvc|пароль|password|hasło",
            setOf(RegexOption.IGNORE_CASE),
        )

    /** An isolated 4–8 digit run — the shape of an OTP, not of a phone number. */
    private val isolatedDigits = Regex("(?<![0-9])[0-9]{4,8}(?![0-9])")

    /**
     * True iff the notification looks like an OTP/2FA message: BOTH a keyword
     * AND an isolated short digit run anywhere across title+text.
     */
    fun looksLikeOtp(
        title: String?,
        text: String?,
    ): Boolean {
        val haystack = listOfNotNull(title, text).joinToString("\n")
        if (haystack.isEmpty()) return false
        return otpKeyword.containsMatchIn(haystack) && isolatedDigits.containsMatchIn(haystack)
    }

    /** Package names are dot-joined identifiers; deliberately permissive. */
    private val packageShape = Regex("^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)+$")

    fun looksLikePackageName(value: String): Boolean = packageShape.matches(value.trim())

    /**
     * The full pre-persistence gate. Returns null when the notification must
     * be dropped; otherwise the capped fields safe to store.
     *
     * @param storeText whether the user opted in to storing notification text.
     */
    fun gate(
        enabled: Boolean,
        allowlist: Set<String>,
        packageName: String?,
        isGroupSummary: Boolean,
        isOngoing: Boolean,
        title: String?,
        text: String?,
        storeText: Boolean,
    ): CapturedNotif? {
        if (!enabled) return null
        if (packageName == null || packageName !in allowlist) return null
        if (isGroupSummary) return null
        if (isOngoing) return null
        if (looksLikeOtp(title, text)) return null
        val safeTitle = title?.trim()?.take(TITLE_MAX_CHARS).orEmpty()
        val safeText = if (storeText) text?.trim()?.take(BODY_MAX_CHARS) else null
        if (safeTitle.isEmpty() && safeText.isNullOrEmpty()) return null
        return CapturedNotif(packageName, safeTitle, safeText?.takeIf { it.isNotEmpty() })
    }
}

/** What survives the gate and may be written to the encrypted DB. */
data class CapturedNotif(
    val packageName: String,
    val title: String,
    val text: String?,
)

/**
 * Stable content identity (FNV-1a 64-bit, hex) so a reposted notification
 * dedups instead of double-counting in the storm detector.
 */
object NotifIdentity {
    fun entryId(
        packageName: String,
        postTimeMillis: Long,
        title: String,
        text: String?,
    ): String {
        val hash = fnv1a64("$title|${text.orEmpty()}")
        return "$packageName:$postTimeMillis:${hash.toULong().toString(16).take(12)}"
    }

    private fun fnv1a64(value: String): Long {
        var hash = -0x340d631b7bdddcdbL
        for (ch in value) {
            hash = hash xor ch.code.toLong()
            hash *= 0x100000001b3L
        }
        return hash
    }
}
