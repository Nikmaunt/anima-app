# ADR-002: Mind — ML Kit GenAI Prompt API behind an interface; honest sleep on S24

Status: accepted, 2026-07-17.

## Decision

- `MindEngine` is an interface in :core:mind with exactly three implementations
  concernable: `NanoMindEngine` (ML Kit GenAI Prompt API,
  `com.google.mlkit:genai-prompt:1.0.0-beta3`), `FakeMindEngine`
  (deterministic, no clock/randomness — tests and previews), and nothing else.
- The Nano backend activates only when `checkStatus()` reports the feature
  usable (AVAILABLE, or DOWNLOADABLE→download with progress). UNAVAILABLE →
  the app runs the designed "mind asleep" mode: the creature lives fully
  (body, motion, journal, soul), chat input is replaced by an honest
  explanation, no fake responses ever.
- Prompt budget: ≤4 000 input tokens. The prompt is assembled as
  persona ("I am this phone") + BodyState snapshot + top-N soul facts +
  trailing dialogue window, trimmed in that priority order (facts before
  dialogue history).
- Streaming via `generateContentStream()` (Flow). Fact-candidate extraction is
  a separate structured call using prompt-disciplined JSON + tolerant parsing
  (genai-schema alpha requires KSP 2.3.6+, our pin is 2.2.0). Extracted
  candidates NEVER auto-persist — each requires explicit user confirmation.

## Why

Research (research.md §C, verified 2026-07-17): Prompt API is Beta and real,
but **Galaxy S24 is not a supported device** — Nano there serves only Samsung's
own features; `checkStatus()` returns UNAVAILABLE. The only stack that would
run an LLM on S24 (MediaPipe/LiteRT, Gemma ≈555 MB) requires either a network
download (violates the zero-network guarantee) or an unshippable APK. The
product spec already demands honest degradation without Nano; S24 becomes the
first-class case of that mode rather than a hack.

## Consequences

- On S25/S26/Fold7/Pixel-class devices the full chat works; on S24 the manual
  checklist expects feature-check = UNAVAILABLE and verifies the honesty of the
  sleep mode (no fake AI).
- AICore enforces per-app quotas and **foreground-only inference**
  (BACKGROUND_USE_BLOCKED) — which Anima's constraint #5 (no background life
  beyond the listener) already guarantees by design.
- BUSY/quota errors surface as the creature being "тired of thinking" with
  `getRetryDelay()`-driven retry hints — never as raw error codes.

## Errata (v0.4, 2026-07-18)

`getRetryDelay()`-driven retry hints were never implemented; download/inference
failures map to two fixed honest strings. Recorded as intentional scope,
not a bug.
