# Handoff — v0.6 autonomous run, 2026-07-19/20

Run COMPLETED in-session; this file is retained as the protocol requires.
Final state = the committed tree plus the session's final report. Key
documents this run: docs/audit-v05.md, docs/research-v6.md, ADR-018/019,
CHANGELOG.md, docs/release/{signing,closed-testing-plan}.md,
docs/manual-checklist-s24-v6.md, docs/store/store-listing-v6.md,
data-safety FINAL, threat-model addendum v0.6, tools/qwen-int4/.

- Phases 0–3 done; Phase 1's ARTIFACT itself blocked by environment (C:
  disk filled during toolchain install → WSL service wedged; host reboot
  required, out of session mandate). convert.sh is the one-command
  deliverable; ADR-018 outcome row 3 applies to this build (empty pack).
- v0.6 versionCode 6; release APK/AAB build SIGNED locally (upload key in
  ../anima-keys, docs/release/signing.md).
- CI is REAL now: .github/workflows/ci.yml (actionlint-clean). Badge URL
  in README needs the owner's GitHub org/repo substituted on first push.
- Run findings (the E2E discipline paying rent again):
  (1) performScrollTo DEADLOCKS under the paused-clock frame-pump regime —
  jdb-verified; semantics actions / raw swipes are the law now (recorded
  in DayInLifeTest comments).
  (2) BoxWithConstraints was replaced by LocalConfiguration in the home
  scaffold before (1) was proven the real culprit; the replacement is
  kept — it is simpler and subcomposition buys nothing there.
  (3) Two ideation cards (№5 petting haptics, №13 hatch day) were already
  shipped in v0.3 — stale backlog cards, remarked in ideation-v5.
  (4) The app had NO launcher icon through v0.5 (platform default robot);
  fixed only now. Audits: check the obvious next time.
- Environment lessons for future runs on this machine: WSL VHDX lives on
  an always-nearly-full C: — big toolchains go to /mnt/d (convert.sh now
  defaults there); GMD emulators are qemu-system-x86_64-HEADLESS.exe (the
  non-headless name misses them in tasklist); killed gradle invocations
  leave zombie executions holding the GMD device — always
  `gradlew --stop` + kill emulators + reset
  `gradle-managed/active_gradle_devices` (MDLockCount) before rerunning;
  TaskStop of a piped gradle run does NOT stop the daemon's execution.
- Unproven risks carried to S24 checklist v6: §0 int4-Qwen on Exynos
  (artifact first!), §0b wallpaper night battery (THIRD carry — v0.7 must
  cut the listing claim if unrun), §0c RU quality on int4, §0d live-data
  upgrade, §0e signed-release R8 behavior, §0f folds.
