# Changelog

Anima — a small creature that lives in your phone. All notable changes, one
entry per autonomous build run. Versioning: `versionName` = `0.MINOR.PATCH`
while pre-1.0 (MINOR = run number); `versionCode` = MINOR while pre-1.0,
switching to `MAJOR*10000 + MINOR*100 + PATCH` at 1.0.0 (documented ahead of
time so the pre-1.0 codes 1…N stay forever below any post-1.0 code).

## [0.7.0] — 2026-07-24

The runtime-future run: audited past, prepared successor.

### Added
- LiteRT-LM engine (`LiteRtLmMindEngine`) as the additive second local
  runtime behind the same `MindEngine` interface (ADR-020): debug builds
  only, developer flag default OFF, CPU backend enforced by test
  (`LiteRtLmCpuOnlyTest`); tasks-genai remains the shipped runtime.
- JVM inference smoke (`tools/litertlm-smoke`, env-gated): the project's
  first no-phone inference — litertlm-jvm 0.14.0 + CPU + Qwen2.5-1.5B q8
  `.litertlm`, run green in-session.
- NetworkIsolationTest v7: mind-module dependency allowlist (14 tests);
  litertlm pinned debug-only.
- Developer section in Settings (debug builds), 6 locales.
- docs: audit-v06 (first audit with zero fabricated claims — and the
  first with a live-emulator eyes pass), freshness-2026-07 (runtime /
  Play policy / EU AI Act / dependency research with dated citations),
  ADR-020, manual checklist v7.

### Fixed
- Onboarding concept labels were crushed to ~2dp on 411dp-wide screens
  (ConceptGallery fixed-height cell) — invisible since v0.1, caught by
  the first live-emulator eyes pass.
- Stale "~530 MB" model-size copy (3 keys × 6 locales) now formats the
  registry default (~1.2 GB) — copy can no longer drift from ADR-018.
- Day-in-life E2E wall-clock safety net 60s→240s: cold-emulator first
  runs from a slow disk exceeded 60s to first frame (test-infra, not an
  app regression; virtual-clock determinism unchanged).

### Changed
- Same-line patch bumps (no security-mandated bumps existed this cycle):
  AGP 8.13.2, lifecycle 2.9.4, navigation 2.9.8, WorkManager 2.10.5,
  truth 1.4.5, turbine 1.2.1. OSS licenses export refreshed (227 libs).
- Store-materials sweep: the live-wallpaper battery claim mandated for
  removal by §0b turned out to NEVER have existed in any listing text
  (6 locales searched) — obligation closed as vacuous, rule "no
  battery/wallpaper marketing until measured" stands.

## [0.6.0] — 2026-07-19

The release-engineering run: the path to Play closed testing.

### Added
- Encrypted upgrade-path test: a lived-in v1 database (facts, chats, rests,
  meta) built by SQLCipher itself, migrated 1→2 through the exact production
  builder, integrity-checked, ciphertext-verified (GMD).
- Day-in-life E2E v2: time-capsule write/hold/delivery and the goodnight
  ritual, on a pinned-evening injectable clock (`AnimaClock`).
- Soul-backup round-trip unit suite incl. a version-1 fixture (audit-v05 D5).
- FLAG_SECURE on every capsule surface via shared `SecureWhile` (audit D1).
- LocalMindEngine tripwire extended to the body diary (audit D4, owed
  since audit-v04).
- Release signing: upload-key config outside the repo (docs/release/signing.md).
- CI: GitHub Actions workflow (unit, detekt/ktlint, lintVital, Roborazzi,
  GMD) — see workflow file for the honest scope.
- OSS licenses screen in Settings.
- Closed-testing plan (docs/release/closed-testing-plan.md), ADR-018 pack
  default, ADR-019 F-Droid feasibility.
- Qwen2.5-1.5B int4 conversion pipeline under tools/ (artifacts stay out
  of git).

### Changed
- `mind_failure_http` copy de-Gemma'd in all six locales (registry ships
  more than one model family now).
- README brought from v0.3 claims to v0.6 reality.

## [0.5.0] — 2026-07-18

The voice run: the creature speaks your language.

- Mind v2: model registry (multi-model, Gemma + Qwen2.5), native-language
  routing with an honest English-fallback badge, cloud presets.
- Complete localization: EN/RU/PL/DE/ES/JA across every module, plurals,
  per-app language, pseudolocale sweeps; RU/JA goldens.
- Ideation top-3 shipped: goodnight ritual, weather feel, time capsules
  (schema v2 — the first real migration, `time_capsules` + MIGRATION_1_2).
- Soul backup payload v2 (capsules travel); v1 files import fine.
- Day-in-life E2E on GMD; caught the NavHost double-Home cross-fade and an
  init-order NPE that only reproduced on device.

## [0.4.0] — 2026-07-05..07

The care run: living together, gently.

- Rest-together sessions, battery care, diary v3 (retellings), live
  wallpaper (ADR-012), creature voice via offline TTS (ADR-013), system
  surfaces (ADR-015), milestones + postcard export.
- Roborazzi screenshot regression rig; design-system goldens.
- Threat-model addendum; Data-safety draft; store listing package.

## [0.3.0] — 2026-07-04

The mind run: tiered intelligence, zero setup.

- Tiered mind (ADR-005): Gemini Nano via ML Kit GenAI where present,
  Gemma/MediaPipe fallback, honest "mind asleep" otherwise.
- Play Asset Delivery model pack (ADR-010); BYOK cloud mind (ADR-011) as
  the second sanctioned INTERNET module.
- sqlcipher 4.17.0 security bump after CVE audit; WAL pool-key regression
  pinned by a device test.
- Evolution, soul v2 (supersede/forget chains), quiet hours, a11y pass.

## [0.2.0] — 2026-07-02..03

The soul run: memory with consent.

- Encrypted soul vault (SQLCipher + Keystore-wrapped passphrase, ADR-003);
  facts only ever persisted after explicit user confirmation.
- Home + chat, notification sense (opt-in, filtered, pruned), soul port
  (markdown export), onboarding with the honest contract.
- NetworkIsolationTest: the network guarantee as a failing build (ADR-004).

## [0.1.0] — 2026-07-01

The body run: something alive on the screen.

- Creature engine: 8 concepts, one seed each, hand-rolled motion physics
  under strict animation budgets (motion bible as tests).
- Pure-JVM domain core; module skeleton with convention plugins;
  design system.
