# Handoff — v0.7 autonomous run, 2026-07-24 (IN PROGRESS)

Mandate: deep audit of v0.6 + LiteRT-LM engine as additive second runtime
(ADR-016 executed, not revised) + freshness research + claim hygiene.

## Progress

- [x] Phase 0 — fresh read-only audit of v0.6: docs/audit-v06.md.
  First run with ZERO fabricated claims. Full local loop green (92 units
  forced re-run, 46 goldens verified, detekt/ktlint/lintVital/bundle).
  Live-emulator eyes pass (AVD `eyes34` google_apis-34 on D:) — launcher
  icon real, FLAG_SECURE live-proven (soul screencap = 0 bytes), two
  defects queued: ConceptGallery onboarding labels crushed to ~2dp;
  18 stale "~530 MB" strings (3 keys × 6 locales).
  GMD: first run FAILED — AVD went to full C: (needs 7.2 GB); rerun in
  flight with ANDROID_AVD_HOME=D:\Android\avd (the v0.6 lesson, now twice).
- [ ] Phase 1 — three research agents in flight (runtime/policy/deps).
  Results → docs/freshness-2026-07.md + ADR-020 + addenda.
- [ ] Phase 2 — LiteRtLmMindEngine (flag OFF, CPU-only enforced by test),
  NetworkIsolationTest coverage, JVM smoke (ANIMA_JVM_LLM_SMOKE=1).
- [ ] Phase 3 — fixes: ConceptGallery labels; 530 MB → registry-driven
  size; patch bumps per research; README/CHANGELOG 0.7.0/7.
  NOTE Phase 3.2: exhaustive search shows the live-wallpaper battery claim
  DOES NOT EXIST in docs/store/* (no "wallpaper" match in any listing file,
  6 locales, nor screenshot-scenario) — the §0b listing-claim obligation is
  satisfiable only vacuously; record in report, fix checklist wording.

## Environment facts (this session)

- C: is 100% full (2.1 GB free) — everything big goes to D:.
  GMD AVD: ANDROID_AVD_HOME=D:\Android\avd. Eyes-pass AVD `eyes34` also
  lives there (delete when done).
- Emulator for eyes pass: `emulator -avd eyes34 -no-window` + adb
  screencap/uiautomator works fine for screen review; FLAG_SECURE screens
  yield 0-byte screencaps (by design).

## Key architecture notes for Phase 2 (read before coding)

- MindEngine / LocalMindEngine in core/model/Mind.kt; tier ladder in
  core/mind/TieredMindEngine.kt injects GemmaMindEngine directly — the
  local-engine flag must swap the GEMMA-tier implementation, not the
  ladder (TierSelection stays engine-agnostic via locator.installed).
- Prompt budgets already registry-driven: MindModelSpec.{maxTokens,
  promptFormat, stopTokens, promptCharBudget} (MindModelRegistry.kt).
- Prefs: core/data/prefs/AnimaPrefs.kt (DataStore); no developer section
  exists yet in Settings.
- NetworkIsolationTest (app/src/test) walks ALL module sources excluding
  paths containing "test" and all *.gradle.kts for network needles — the
  JVM smoke's download code must live under src/test of its module.
- Direct-link download field already accepts .litertlm
  (`mind_url_hint`: "https://… (.task / .litertlm)").
