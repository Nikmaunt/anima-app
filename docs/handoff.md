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
- [x] Phase 1 — research done and committed (fb9daa9):
  docs/freshness-2026-07.md (§A runtime, §B policy, §C deps) + ADR-020.
- [x] Phase 2 — BUILT, verification loop in flight at handoff-write time:
  LiteRtLmMindEngine in core/mind/src/debug (CPU-only; API verified
  against the actual AAR via javap — Engine/EngineConfig/Conversation/
  Contents.of, package com.google.ai.edge.litertlm); debugImplementation
  only; @BindsOptionalOf @AltLocalEngine + LocalEngineChoice routing in
  TieredMindEngine; MindEngineSwitch over AnimaPrefs (default OFF,
  pinned by AnimaPrefsDefaultsTest); developer section in Settings
  (BuildConfig.DEBUG-gated, strings ×6); tests: LiteRtLmCpuOnlyTest,
  LocalEngineChoiceTest, NetworkIsolationTest mind-allowlist (now 14).
  JVM smoke PASSED live this session: tools/litertlm-smoke,
  litertlm-jvm 0.14.0 + CPU + Qwen2.5-1.5B q8 .litertlm (1 597 931 520
  bytes; gemma3-270m is HF-license-gated → 401, the v0.2 lesson) →
  reply "Hello there! How can I assist you today?" (4m incl. download).
  Model cache: D:\Android\llm-cache (C: full!), env ANIMA_JVM_LLM_CACHE.
- [~] Phase 3 — 3.1 done in tree: ConceptGallery labels (weight(1f) fix),
  530MB → registry-driven size (%1$s + mind_size_gb ×6 locales,
  packDefaultSize() in MindScreen, Formatter in HomeScreen), GMD
  DayInLifeTest safety-net 60s→240s. 3.2 vacuous (claim absent — see
  NOTE). Remaining: 3.3 patch bumps (AGP 8.13.2; lifecycle 2.9.4;
  navigation 2.9.8; work 2.10.5; truth 1.4.5; turbine 1.2.1 — three
  batches, full loop each), exportLibraryDefinitions refresh (litertlm
  added!), 3.4 version 0.7.0/7 + README/CHANGELOG; GMD re-run; DoD
  subagent. manual-checklist-s24-v7.md already written.
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
