# ADR-013: Creature voice via system TTS — approved, offline-verified, default OFF

Status: accepted, 2026-07-18.

## Findings (research-v4 §6)

- `Voice.isNetworkConnectionRequired()` is the sanctioned per-voice offline
  check; `KEY_FEATURE_NETWORK_SYNTHESIS` is deprecated in its favor. The
  embedded-synthesis contract — "the engine must synthesize text on-device
  (without making network requests)" — is the platform's clearest privacy
  statement. Network voices are network-based synthesis: utterance text
  leaves the device (explicit official sentence not found → treated as
  engineering conclusion from the documented contract).
- Offline voice packs are installed by the USER in system settings; the app
  cannot download them.
- Samsung's own engine documents the install flow but not offline
  guarantees (UNVERIFIED) — which is why the Voice filter, not engine
  identity, is our guarantee.
- `setPitch`/`setSpeechRate` before each `speak()` + per-utterance
  volume/pan give enough axes for per-concept characters; single-voice
  locales rely on pitch/rate alone (distinctness is a design judgment).
- `UtteranceProgressListener.onStart/onDone` is the reliable animation
  envelope; `onRangeStart` is engine-dependent (enhancement only).

## Decision

The creature gets a voice in v0.4 under these rules:

- **Default OFF.** An explicit toggle in Settings; first enable runs an
  offline-voice check.
- **Offline voices only**: `getVoices()` filtered to
  `isNetworkConnectionRequired() == false`, ranked by `getQuality()`. The
  engine default voice is never used blindly. No offline voice for the
  creature's language → the toggle explains why and deep-links to system
  TTS settings; the creature stays mute. Text never reaches a network voice.
- **Character mapping**: pitch/rate derived deterministically from concept +
  personality (one table, unit-tested range clamps 0.5..2.0).
- **Scope**: chat replies and the morning greeting only. No background
  speech; speaking stops on screen-off/navigation (finite episodes).
- Mouth/glow animation driven by `onStart`/`onDone`; `onRangeStart` used
  only when the engine reports ranges.
- TTS engine runs in its own process; no INTERNET implications for Anima's
  manifest — NetworkIsolationTest is unaffected and re-checked.
