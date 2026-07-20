# Anima

[![ci](https://github.com/Nikmaunt/anima-app/actions/workflows/ci.yml/badge.svg)](../../actions/workflows/ci.yml)

A creature lives in your phone. It *is* your phone.

Anima is a standalone Android app: an on-screen companion whose body reflects
the real state of the device (battery, charging, storage, connectivity,
thermal, barometer, ringer), who talks via **on-device** LLM inference by
default (Gemini Nano where the device has it; a registry of local models —
default Qwen2.5-1.5B-Instruct, which speaks all six product languages —
everywhere else), remembers only what you confirm, **strictly locally** in an
encrypted database, and whose "soul" exports as a single file.

**No server of ours. No telemetry. No accounts. No ads.**

Localized end to end: EN · RU · PL · DE · ES · JA (per-app language switch,
localized mind routing with an honest English-fallback badge).

## Hard guarantees (v0.6)

| Guarantee | Mechanism |
|---|---|
| Two-module network world | `INTERNET` originates from exactly `:core:model-delivery` (model bytes in) and `:core:cloud-mind` (opt-in BYOK) — enforced by `NetworkIsolationTest` v6 (13 tests over manifests, merged manifest, sources, version catalog, build files); no third-party HTTP stack exists in the catalog at all |
| Local-first mind | Nano → registry model (pack/download/SAF) → honest sleep; the CLOUD tier speaks **only** when the user enables it with their own endpoint + key, is always labeled on screen, and degrades to local when offline |
| Zero-setup model | Play Asset Delivery fast-follow pack (ADR-010/017/018); sideloads fall through to download/SAF |
| Local encrypted storage | SQLCipher (4.17.0), random passphrase wrapped by Android Keystore; corrupt key files fail loudly; migrations device-tested on a ciphered lived-in v1 database (`EncryptedUpgradeDeviceTest`) |
| Minimal permissions | Notification Access strictly opt-in, asked in context, never in onboarding |
| Background entities | The listener + widget triggers, nothing more (no push, no services) |
| Untrusted text is never a command | Notification/fact/import text is data by contract; facts enter the soul only after explicit user confirmation |
| Screens protect the soul | Soul screen, capsule delivery and capsule drafting set `FLAG_SECURE` by default (one `SecureWhile` implementation; toggle in Settings) |
| AI replies answer to you | Long-press any creature reply → report: the reply is deleted and the event journaled locally (Play GenAI policy) |
| Crash data stays home | Local crash file + "show & copy" screen; no crash reporting SDK |

## CI

`.github/workflows/ci.yml`: unit suites (incl. NetworkIsolationTest and the
soul-backup round-trip), detekt + ktlint, Roborazzi golden verification,
license-export freshness, `lintVitalRelease`, debug APK + unsigned release
bundle, and the instrumented suite on a gradle-managed `aosp-atd` emulator
(KVM). Release artifacts sign only on machines that hold the upload key —
see [docs/release/signing.md](docs/release/signing.md).

## Structure

See [docs/](docs/) — research (v1–v6), product research, ADRs (001–019),
threat model, motion bible, audits (v01–v05), the closed-testing plan
(docs/release/), store package, S24 manual checklists, handoff notes.

## Build

```
./gradlew assembleDebug          # debug APK (no asset packs)
./gradlew test                   # unit suites incl. NetworkIsolationTest
ANDROID_AVD_HOME=<see docs>  ./gradlew atd34DebugAndroidTest   # GMD suites
./gradlew bundleRelease          # AAB incl. the :mind-pack slot (signed if
                                 # ../anima-keys exists; unsigned otherwise)
./gradlew :app:exportLibraryDefinitions   # refresh the OSS licenses JSON
```

Model weights are **not** in git: the pack slot takes
`qwen2.5-1.5b-instruct-int4.litertlm` produced by
[tools/qwen-int4/convert.sh](tools/qwen-int4/convert.sh) (ADR-018), or stays
empty — the app falls through to download/SAF paths honestly
(see `mind-pack/src/main/assets/README.md`).
