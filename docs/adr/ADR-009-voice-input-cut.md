# ADR-009: Voice input — cut from v0.2

Status: accepted, 2026-07-17.

## Findings (research-v2 §B.4)

- `RecognizerIntent.EXTRA_PREFER_OFFLINE` is documented as a hint that "may
  have no effect" — it guarantees nothing on any OEM, Samsung included.
- The only app-verifiable offline path is
  `SpeechRecognizer.isOnDeviceRecognitionAvailable` +
  `createOnDeviceSpeechRecognizer` (API 31+), which binds the on-device
  service by construction — but its actual availability and quality on the
  base S24 is **UNVERIFIED** (no credible reports found; Samsung's offline
  language-pack behavior is notoriously flaky).
- RECORD_AUDIO would also be the app's first dangerous permission.

## Decision

Voice input is **cut from v0.2** — first in the brief's own cut order, and
the only feature whose privacy story ("offline, honestly") cannot be
verified from this desk. If it returns in v0.3 it comes back exactly as the
brief allowed: behind a toggle, `createOnDeviceSpeechRecognizer` only
(never the intent path), honest "voice unavailable" state, after the
on-device recognizer is proven offline on the owner's S24 (airplane-mode
test in the manual checklist).
