# ADR-011: Cloud mind — user-keyed (BYOK), opt-in, honest, minimal

Status: accepted, 2026-07-17. Amends ADR-004: `:core:cloud-mind` is the
second and LAST sanctioned network module.

## Problem

Some phones will never run a local model well (4 GB RAM, no Nano). The only
honest alternative to "the mind sleeps" is a cloud model — but a cloud
default would betray the product's core promise. v0.2 shipped no cloud at
all; v0.3 adds it as an explicit user choice.

## Decision

- **OpenAI-compatible chat-completions endpoint + user's own key (BYOK).**
  One wire format covers OpenAI, Groq, Mistral, OpenRouter, Together, local
  LM Studio/Ollama servers (research-v3 §B). No vendor SDK; implemented over
  `HttpsURLConnection` with SSE streaming — the version-catalog ban on HTTP
  client libraries stays intact.
- **Opt-in only, never a silent fallback.** The tier order becomes:
  cloud IF AND ONLY IF the user enabled it → else Nano → Gemma → sleep.
  Offline or on error the reply degrades to the local tier for THAT reply,
  with the failure surfaced; the app never flips the setting itself.
- **Honest enable screen** (Settings → Mind): plain text — "in this mode
  your messages and the facts needed for the answer are sent to
  {host you configured}" — plus exactly what is sent (see below). The
  "Mind: local / cloud" switch is always visible in Settings; cloud state
  is also visibly labeled in the chat (creature wears a tiny antenna badge —
  no dark patterns, the user always knows which mind is speaking).
- **Minimum data out.** The cloud request carries: the system prompt
  (persona + body report + top-N live facts — the same PromptBuilder output
  the local tiers get), the trailing dialogue window and the new message.
  NEVER the whole soul, never the journal/notification tables. Fact
  extraction in cloud mode returns candidates through the SAME user
  confirmation flow — nothing writes itself into the soul.
- **Key custody:** the API key is encrypted with an Android Keystore
  AES-GCM key (same pattern as the soul passphrase wrap, separate alias)
  and stored in `noBackupFilesDir`. The key is never logged, never included
  in the soul export, never shown back in full (masked tail in UI).
  Enforced by test: NetworkIsolationTest v3 greps `:core:cloud-mind` for
  logging calls, and a unit test proves the config store round-trips
  without the plaintext touching DataStore/export surfaces.
- **Bigger context when cloud speaks:** PromptBuilder budget for the CLOUD
  tier is 24 000 chars (~8k tokens conservative) vs Gemma's 5 100 —
  still window-limited, not "the whole soul".
- **Streaming, timeouts, degradation:** SSE chunks stream into the same
  `MindEvent.Chunk` flow; connect/read timeouts 10 s/30 s; any failure ends
  the reply with a mapped `MindFailure` and the caller falls back to the
  local tier for the next attempt.

## Consequences

- New module `:core:cloud-mind` (Hilt, INTERNET manifest, zero third-party
  deps). `TieredMindEngine` grows a CLOUD branch; `MindTier` gains CLOUD;
  `MindSnapshot` reports cloud config state.
- NetworkIsolationTest v3 enforces the two-module network world order.
- The trust page ("Why Anima doesn't need the internet") gains an honesty
  section: cloud mode exists, what it sends, and that it is off by default.
- Play Data safety declaration changes IF the user enables cloud: the app
  itself still collects nothing; user-directed BYOK traffic goes to the
  user's own provider. Wording lands in the store-listing checklist.

## Errata + hardening (v0.4, 2026-07-18)

- **Data-scope violation fixed (audit-v03 F1):** the notification digest fed
  titles+packages into the tiered engine, which picked CLOUD when enabled —
  contradicting this ADR's "never the journal/notification tables". The
  digest (and any future body-adjacent summary) now depends on
  `LocalMindEngine`, a DI type that cannot select the cloud tier; pinned by
  a unit test on the tier ladder and a NetworkIsolationTest v4 tripwire.
- "Masked tail in UI" was never built: the key is simply never displayed
  (placeholder text only) — stronger than documented; the entry field is
  now password-masked while typing (audit-v03 F5).
- "Tiny antenna badge on the creature" shipped as the "· cloud mind" header
  label — the label IS the indicator of record.
- Mid-stream degradation: an offline/misconfigured cloud falls through to
  local tiers before the call; a FAILED mid-stream reply does NOT retry
  locally for that reply (honest error instead). The "falls back by itself"
  settings copy covers the pre-call path only.
