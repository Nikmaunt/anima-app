package app.anima.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Schema conventions (hermes-app donor, ADR-003): string PKs `prefix-hex`,
 * epoch-millis UTC, enums stored as wire strings (safe fallback on read),
 * soul_facts append-only + supersede/forget — no physical deletes anywhere
 * except notification pruning, which is a privacy cap, not an edit.
 */

@Entity(tableName = "soul_facts")
data class SoulFactEntity(
    @PrimaryKey val id: String,
    val category: String,
    val text: String,
    val source: String,
    val createdAtMillis: Long,
    val supersededById: String?,
    val forgottenAtMillis: Long?,
)

@Entity(tableName = "chat_messages", indices = [Index("atMillis")])
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val role: String,
    val text: String,
    val atMillis: Long,
)

@Entity(tableName = "body_journal", indices = [Index("atMillis")])
data class BodyJournalEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val atMillis: Long,
    val detail: String?,
)

@Entity(tableName = "notif_events", indices = [Index("postedAtMillis"), Index("packageName")])
data class NotifEventEntity(
    /** Stable content id (NotifIdentity) — reposts dedup via IGNORE insert. */
    @PrimaryKey val id: String,
    val packageName: String,
    val postedAtMillis: Long,
    val title: String,
    val text: String?,
)

/** Single-row-per-key store for creature identity + counters. */
@Entity(tableName = "meta")
data class MetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)
