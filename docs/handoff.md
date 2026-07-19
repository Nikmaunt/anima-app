# Handoff — v0.5 autonomous run, 2026-07-18

Run COMPLETED in-session; this file is retained as the protocol requires.
Final state = the committed tree, plus the session's final report. Key
documents this run: docs/audit-v04.md, docs/research-v5.md,
docs/model-matrix.md, ADR-016/017 (+ ADR-014 amendment), docs/ideation-v5.md,
docs/l10n-glossary.md, docs/store/store-listing-v5.md,
docs/manual-checklist-s24-v5.md, threat-model addendum v0.5.

- Phases 0–3 done in full: day-in-life E2E on GMD, mind v2 (registry /
  multi-model / language routing / cloud presets), COMPLETE localization
  (EN/RU/PL/DE/ES/JA — every module resourced, plurals, per-app language
  config), ideation (16 cards) with top-3 shipped (goodnight ritual,
  weather feel, time capsules).
- Findings of the run: (1) AnimaRoot flipped NavHost startDestination when
  the onboarding flag landed — two Home compositions cross-faded at once;
  frozen at first composition, caught by DayInLifeTest's first GMD run.
  (2) HomeViewModel init-order NPE (StateFlow declared below init) — only
  reproducible on device; GMD caught it. Both are why the E2E discipline
  exists.
- DB is at schema v2 (time_capsules, MIGRATION_1_2, device-tested);
  soul-backup payload v2 (v1 imports fine). Soul export now carries
  capsules.
- Pack default is CONFIGURED for Qwen2.5-1.5B (ADR-017); the int4 (~1.1 GB)
  artifact must be produced/downloaded by the owner — the int8 (1.6 GB)
  does NOT fit Play's 1.5 GB fast-follow pack limit. Until the int4 file
  exists, the slot still accepts the old Gemma file; the registry handles
  both. Model files are never in git.
- GMD needs `ANDROID_AVD_HOME=D:/Android/avd/gradle-managed` per
  invocation on this machine.
- Bake-off harness: core/mind androidTest, models pushed to
  /sdcard/Android/data/app.anima.core.mind.test/files/bakeoff/ — S24
  checklist v5 §0 runs it for Gemma-vs-Qwen on live silicon.
- Key unproven risks (S24 checklist v5): §0 Qwen2.5 speed/RAM on Exynos
  2400, §0b wallpaper night battery (carried from v4, still unproven),
  §0c native-speaker RU/JA conversation quality of Qwen2.5-1.5B.
