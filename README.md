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

## Hard guarantees (v0.8)

| Guarantee | Mechanism |
|---|---|
| Two-module network world | `INTERNET` originates from exactly `:core:model-delivery` (model bytes in) and `:core:cloud-mind` (opt-in BYOK) — enforced by `NetworkIsolationTest` v7 (14 tests over manifests, merged manifest, sources, version catalog, build files, per-module dependency allowlists); no third-party HTTP stack exists in the catalog at all |
| One shipped local runtime | The GEMMA tier runs on MediaPipe tasks-genai; the LiteRT-LM successor exists as a debug-only second engine behind a developer flag (default OFF, CPU-only by test — ADR-020). Release APKs carry no LiteRT-LM runtime: the dependency is `debugImplementation` and the DI binding lives in the debug source set |
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

Model weights are **not** in git (`.gitignore` fences `*.litertlm`,
`*.task`, `*.gguf`, `*.tflite` repo-wide, and `mind-pack/.gitignore` fences
the asset dir again). **The pack slot currently ships EMPTY** and the app
falls through to the download/SAF paths honestly
(see `mind-pack/src/main/assets/README.md`).

Why empty, as of v0.8 (ADR-021 — the reason changed, the outcome did not):
size is no longer the obstacle — the official q8 measures 8.1% *under*
Play's 1.5 GB per-pack limit once compressed. The obstacle is the runtime:
`tasks-genai` 0.10.35, the shipped default engine, refuses to initialize on
a Qwen `.litertlm` (`SentencePiece tokenizer is not found in the model`).
[tools/qwen-int4/convert.sh](tools/qwen-int4/convert.sh) therefore no longer
gates a release — an int4 build would carry the same tokenizer.
