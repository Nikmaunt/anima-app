# Anima

A creature lives in your phone. It *is* your phone.

Anima is a standalone Android app: an on-screen companion whose body reflects the
real state of the device (battery, charging, storage, connectivity, thermal),
who talks via **on-device** LLM inference by default (Gemini Nano where the
device has it, bundled Gemma 3 1B everywhere else), remembers everything
**strictly locally** in an encrypted database, and whose "soul" exports as a
single file.

**No server of ours. No telemetry. No accounts. No ads.**

## Hard guarantees (v0.3)

| Guarantee | Mechanism |
|---|---|
| Two-module network world | `INTERNET` originates from exactly `:core:model-delivery` (model bytes in) and `:core:cloud-mind` (opt-in BYOK) — enforced by `NetworkIsolationTest` v3 (10 tests over manifests, merged manifest, sources, version catalog, build files); no third-party HTTP stack exists in the catalog at all |
| Local-first mind | Nano → bundled/downloaded Gemma → honest sleep; the CLOUD tier speaks **only** when the user enables it with their own endpoint + key, is always labeled on screen, and degrades to local when offline |
| Zero-setup model | Play Asset Delivery fast-follow pack (ADR-010); sideloads fall through to download/SAF |
| Local encrypted storage | SQLCipher (4.17.0), random passphrase wrapped by Android Keystore; corrupt key files fail loudly; verified on-device by the GMD suite |
| Minimal permissions | Notification Access strictly opt-in, asked in context, never in onboarding |
| Background entities | The listener + widget triggers, nothing more (no push, no services) |
| Untrusted text is never a command | Notification/fact text is data by contract; facts enter the soul only after explicit user confirmation |
| Screens protect the soul | Soul screen sets `FLAG_SECURE` by default (toggle in Settings) |
| Crash data stays home | Local crash file + "show & copy" screen; no crash reporting SDK |

Note: there is no CI in this repo — every guarantee above is a local test;
run the build to enforce them (`test` requires a prior `assembleDebug` for
the merged-manifest check, wired automatically).

## Structure

See [docs/](docs/) — research (v1–v3), product research, ADRs (001–011),
threat model, motion bible, audits, store concepts, handoff notes.

## Build

```
./gradlew assembleDebug          # debug APK (no asset packs)
./gradlew test                   # unit suites incl. NetworkIsolationTest
./gradlew :core:data:atd34DebugAndroidTest \
          :feature:widget:atd34DebugAndroidTest   # GMD (headless emulator)
./gradlew bundleRelease          # AAB incl. the :mind-pack slot
```

The Gemma weights are **not** in git: place the licensed `.task` file under
`mind-pack/src/main/assets/` before building a store bundle
(see `mind-pack/src/main/assets/README.md`).
