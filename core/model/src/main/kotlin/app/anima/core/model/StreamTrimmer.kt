package app.anima.core.model

/**
 * Streaming-safe stop-token trimming (ADR-016). ChatML models emit literal
 * `<|im_end|>` through MediaPipe (which has no stop-token option), and a
 * token boundary can split the marker across chunks — so the trimmer holds
 * back any tail that could still grow into a stop token and releases it once
 * it provably can't.
 */
class StreamTrimmer(
    private val stopTokens: List<String>,
) {
    private val held = StringBuilder()
    private var stopped = false

    /** Feed one streamed chunk; returns the text that is safe to show. */
    fun feed(chunk: String): String {
        if (stopped) return ""
        held.append(chunk)
        val text = held.toString()
        val stopAt = stopTokens.mapNotNull { text.indexOf(it).takeIf { i -> i >= 0 } }.minOrNull()
        if (stopAt != null) {
            stopped = true
            held.setLength(0)
            return text.substring(0, stopAt)
        }
        val hold = longestSuspiciousTail(text)
        val safe = text.substring(0, text.length - hold)
        held.setLength(0)
        held.append(text.takeLast(hold))
        return safe
    }

    /** Terminal flush: whatever was held back and never became a stop token. */
    fun flush(): String {
        if (stopped) return ""
        val rest = held.toString()
        held.setLength(0)
        return rest
    }

    /** Longest text suffix that is a proper prefix of some stop token. */
    private fun longestSuspiciousTail(text: String): Int {
        var longest = 0
        stopTokens.forEach { token ->
            val max = minOf(token.length - 1, text.length)
            for (len in max downTo 1) {
                if (text.regionMatches(text.length - len, token, 0, len)) {
                    if (len > longest) longest = len
                    break
                }
            }
        }
        return longest
    }

    companion object {
        /** One-shot trim for non-streamed full replies. */
        fun trimFinal(
            text: String,
            stopTokens: List<String>,
        ): String {
            val stopAt = stopTokens.mapNotNull { text.indexOf(it).takeIf { i -> i >= 0 } }.minOrNull()
            return (if (stopAt != null) text.substring(0, stopAt) else text).trimEnd()
        }
    }
}
