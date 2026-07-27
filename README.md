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

## Hard guarantees (v0.9)

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

Why empty, as of v0.9 — and this is now a **choice, not an obstacle**
(ADR-022). Both blockers are gone:

- **Size** was never one (ADR-021): the artifact measures ~8% *under*
  Play's 1.5 GB per-pack limit once compressed.
- **The runtime refusal was about the CONTAINER, not the model.**
  `tasks-genai` 0.10.35 refuses any `.litertlm` — but reads the *same*
  Qwen family in a `.task` container and answers. Measured this run:
  `Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.task` initializes and
  replies on the shipped default engine, and packs to 1,377,210,476 bytes
  (8.2% margin). The misleading `SentencePiece tokenizer is not found`
  text was the engine failing to parse the container, not a tokenizer
  requirement.

So the six-language model is available today on the default engine, with no
engine change and no conversion — and v1.0 then asked the next question and
got a harder answer.

**What the model SAYS is not yet the creature (v1.0, ADR-023).** Measured
with product prompts, five scenarios per language, verbatim replies in
[docs/lang-matrix-2026-07.md](docs/lang-matrix-2026-07.md): English holds
character; Russian, German, Spanish and Japanese are understandable but
thin; **Polish is ungrammatical**, so the registry no longer claims it and
Polish users get English behind a visible badge. Eight prompt revisions,
each measured, took the acceptance score from 164/180 to 173/180 and
eliminated the two v0.9 defects (a leaked system prompt, and the creature
telling people it had saved a fact it had not). What no revision fixed is
Qwen's assistant boilerplate — that is the model, not the wording.

So before the pack slot is filled there is now a product decision as well
as the measurements: **speed on real silicon** (S24 checklist §0) and
**whether a creature that sometimes says "how can I assist you" is worth
shipping 1.4 GB for** (§0g).
[tools/qwen-int4/convert.sh](tools/qwen-int4/convert.sh) is closed for good:
mixed-int4 `.litertlm` was refused exactly like q8, proving the conversion
never addressed the real cause.
