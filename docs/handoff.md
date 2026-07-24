# Handoff — v0.7 autonomous run, 2026-07-24

Run COMPLETED in-session; this file is retained as the protocol requires.
Final state = the committed tree plus the session's final report. Key
documents this run: docs/audit-v06.md, docs/freshness-2026-07.md,
ADR-020, docs/manual-checklist-s24-v7.md, CHANGELOG 0.7.0.

- Phase 0: first audit with ZERO fabricated v0.6 claims; first
  live-emulator eyes pass (AVD google_apis-34 headless + adb
  screencap/uiautomator — the recipe works and found what goldens
  can't: crushed onboarding labels). FLAG_SECURE live-proven (0-byte
  screencap). GMD DayInLifeTest red was diagnosed to cold-boot slowness
  (warm connected runs pass twice) — safety net raised 60s→240s.
- Phase 2: LiteRT-LM second engine (debug-only, flag OFF, CPU-only by
  test). API was written against the actual AAR (javap) — the Kotlin
  API is Engine/EngineConfig/Conversation/Contents.of, NOT the blog
  shapes. JVM smoke green: litertlm-jvm 0.14.0 + Qwen2.5-1.5B q8
  (model cache D:\Android\llm-cache, gemma repos are HF-gated → 401).
- Phase 3: ConceptGallery weight(1f) fix; 530MB→registry-fed size (one
  Formatter path, live "~1.2 GB"); patch bumps ×3 batches each with a
  full green loop; licenses export 227 libs (litertlm included);
  version 0.7.0/7.
- §0b wallpaper listing claim: verified ABSENT from all store texts ×6
  locales — obligation vacuous; measurement stays on the S24 list.

Environment lessons (this machine, this session):
- C: is 100% full: GMD AVD → ANDROID_AVD_HOME=D:\Android\avd (without it
  GMD fails needing 7.2 GB on C:); LLM smoke cache → ANIMA_JVM_LLM_CACHE.
- Windows file-lock transients (classes.jar, lint-cache jars held by a
  process) intermittently fail big loops: `gradlew --stop`, delete the
  named cache dir, rerun — three occurrences, all cleared this way.
- Cold-emulator first runs are >60s to first frame from D: — any
  wall-clock deadline in instrumented tests must assume a cold boot.

Unproven risks carried to S24 checklist v7: §0 int4-Qwen artifact
(convert.sh still blocked by env — host reboot), §0b wallpaper night
battery (fourth carry, now measurement-only), §0c RU quality on int4,
§0d live-data upgrade v0.6→v0.7, §0e signed-release R8, §0f folds,
§0.8 NEW: engine comparison tasks-genai vs LiteRT-LM on one model
(the ADR-020 default-flip gate).
