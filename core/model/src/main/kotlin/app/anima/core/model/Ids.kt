package app.anima.core.model

import java.util.UUID

/**
 * App-generated string primary keys: `prefix-` + 20 hex chars from a UUID.
 * No auto-increment anywhere — ids are creatable before persistence and
 * stable across export/import.
 */
object Ids {
    fun new(prefix: String): String {
        val hex =
            UUID
                .randomUUID()
                .toString()
                .replace("-", "")
                .take(20)
        return "$prefix-$hex"
    }
}
