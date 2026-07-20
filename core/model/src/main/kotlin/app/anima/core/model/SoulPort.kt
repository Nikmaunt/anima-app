package app.anima.core.model

/**
 * Soul export/import: one readable markdown file. Pure formatting/parsing —
 * JVM-testable, no Android imports. Import output is candidates only; every
 * fact still passes explicit user confirmation before persistence.
 */
object SoulPort {
    const val FORMAT_MARKER = "<!-- anima-soul v1 -->"

    fun export(
        creatureName: String,
        concept: CreatureConcept,
        stats: RelationshipStats,
        facts: List<SoulFact>,
        journal: List<BodyJournalEntry>,
        nowMillis: Long,
    ): String =
        buildString {
            appendLine(FORMAT_MARKER)
            appendLine("# The soul of $creatureName")
            appendLine()
            appendLine("A creature of the ${concept.wire.replace('_', ' ')} kind.")
            appendLine()
            appendLine("## Together")
            appendLine()
            appendLine("- Days together: ${stats.daysTogether(nowMillis)}")
            appendLine("- Conversations: ${stats.conversationCount}")
            appendLine("- Things remembered: ${stats.liveFactCount}")
            appendLine("- Meals (charges): ${stats.chargeCount}")
            val live = facts.filter { it.isLive }
            if (live.isNotEmpty()) {
                appendLine()
                appendLine("## What I remember")
                FactCategory.entries.forEach { category ->
                    val inCategory = live.filter { it.category == category }
                    if (inCategory.isNotEmpty()) {
                        appendLine()
                        appendLine("### ${categoryTitle(category)}")
                        appendLine()
                        inCategory.forEach { appendLine("- ${it.text}") }
                    }
                }
            }
            val moments = journal.filter { it.kind == JournalKind.HATCHED || it.detail != null }
            if (moments.isNotEmpty()) {
                appendLine()
                appendLine("## Key moments")
                appendLine()
                moments.take(MAX_EXPORT_MOMENTS).forEach {
                    appendLine("- ${it.kind.wire}${it.detail?.let { d -> ": $d" } ?: ""}")
                }
            }
            appendLine()
            appendLine("---")
            appendLine("Exported from Anima. Everything above lived only on one phone.")
        }

    /**
     * Parses fact lines from an export or from another AI's answer to the
     * extractor prompt. Tolerant: accepts `- [category] text` and plain
     * `- text` bullets; ignores everything else. Returns candidates, never
     * persists.
     */
    fun parseCandidates(raw: String): List<FactCandidate> {
        val result = mutableListOf<FactCandidate>()
        var currentCategory = FactCategory.OTHER
        raw.lineSequence().forEach { line ->
            val trimmed = line.trim()
            headerCategory(trimmed)?.let { currentCategory = it }
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                val body = trimmed.drop(2).trim()
                if (body.isEmpty() || body.length > MAX_FACT_CHARS) return@forEach
                val tagged = tagPattern.find(body)
                if (tagged != null) {
                    val (tag, text) = tagged.destructured
                    if (text.isNotBlank()) {
                        result += FactCandidate(FactCategory.fromWire(tag.lowercase()), text.trim())
                    }
                } else {
                    result += FactCandidate(currentCategory, body)
                }
            }
        }
        return result.distinctBy { it.text.lowercase() }.take(MAX_IMPORT_FACTS)
    }

    // v0.6: the extractor prompt moved to feature:soul string resources so
    // it speaks the user's language (l10n) — the parser here stays
    // language-tolerant via headerCategory.

    private val tagPattern = Regex("^\\[(\\w+)]\\s*(.+)$")

    // v0.6: the localized extractor prompt demands English headings, but
    // assistants answering in the user's language often localize them
    // anyway — recognize all six product languages (l10n-glossary).
    private val headingMarkers: List<Pair<FactCategory, List<String>>> =
        listOf(
            FactCategory.IDENTITY to
                listOf("identity", "личност", "identität", "identidad", "tożsamoś", "アイデンティティ", "自分"),
            FactCategory.PREFERENCE to
                listOf("preference", "предпочт", "vorlieben", "präferenz", "preferencia", "preferencj", "好み"),
            FactCategory.PEOPLE to
                listOf("people", "люди", "menschen", "leute", "persona", "gente", "ludzie", "人"),
            FactCategory.WORK to
                listOf("work", "работ", "arbeit", "trabajo", "praca", "仕事"),
            FactCategory.MOMENT to
                listOf("moment", "момент", "chwile", "思い出", "瞬間"),
        )

    private fun headerCategory(line: String): FactCategory? {
        if (!line.startsWith("#")) return null
        val title = line.trimStart('#').trim().lowercase()
        return headingMarkers
            .firstOrNull { (_, markers) -> markers.any { it in title } }
            ?.first
    }

    private fun categoryTitle(category: FactCategory): String =
        when (category) {
            FactCategory.IDENTITY -> "Identity"
            FactCategory.PREFERENCE -> "Preferences"
            FactCategory.PEOPLE -> "People"
            FactCategory.WORK -> "Work"
            FactCategory.MOMENT -> "Moments"
            FactCategory.OTHER -> "Other"
        }

    const val MAX_FACT_CHARS = 300
    const val MAX_IMPORT_FACTS = 200
    const val MAX_EXPORT_MOMENTS = 30
}
