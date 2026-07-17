# ADR-008: Soul search — in-memory filter, FTS5 rejected (for now)

Status: accepted, 2026-07-17.

## Question

"What do you know about me" browser needs search. The brief suggested FTS5
over SQLCipher, with LIKE as fallback.

## Findings (research-v2 §B.2)

- SQLCipher's own binaries DO compile FTS5 (`-DSQLITE_ENABLE_FTS5` verified
  in the repo's Android.mk; prebuilt-AAR parity is high-confidence but
  formally UNVERIFIED).
- **Room, however, has no FTS5 support** — `@Fts4`/`@Fts3` only. FTS5 would
  mean hand-written virtual tables, sync triggers against the append-only
  `soul_facts`, and raw queries around Room for a table that realistically
  holds **hundreds of rows** (the prompt uses 24 facts; a heavy user might
  reach a thousand).

## Decision

Search and category filtering run **in memory** over the already-loaded
live-facts flow (`contains(ignoreCase)`). At soul scale this is O(instant),
uses zero extra storage, and adds zero schema surface to the append-only
contract. Not even SQL LIKE is needed — the list is already in RAM for the
browser UI.

Revisit trigger: if the soul ever exceeds ~10 000 live facts (it should not
— it's a soul, not a log), move to FTS5 via raw queries and record the
migration here.
