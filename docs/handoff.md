# Handoff — v0.4 autonomous run, 2026-07-18

Run COMPLETED in-session; this file is retained as the protocol requires.
Final state = the committed tree (per the v0.4 commit policy), plus the
session's final report. Key documents: docs/audit-v03.md, docs/research-v4.md,
ADR-012..015 (+ addenda to ADR-002/003/004/005/011/014), docs/store/*,
docs/privacy-policy.md, docs/manual-checklist-s24-v4.md.

- Phases 0-2 done; localization string-migration deferred to v0.5 by the
  declared cut order (ADR-014 amendment — the only scope deviation).
- THE finding of the run: v0.3's passphrase zero-after-warm crashed WAL
  pool growth (SQLiteNotADatabaseException on the first non-primary
  connection open under concurrent load). Found by the bundletool
  --local-testing E2E pass on the ATD emulator minutes into fresh use;
  root-caused via 4.17.0 bytecode (the library aliases the array and
  re-keys every physical connection from it); fixed by removing the ritual
  (key lives process-long, Keystore-wrapped at rest); pinned by
  WalPoolKeyDeviceTest. Upgrade-in-place check is S24 checklist v4 §0.
- GMD needs `ANDROID_AVD_HOME=D:/Android/avd` on this machine (C: is
  nearly full; AVDs live on D:). Not persisted — pass per invocation.
- The Gemma `.task` file is NOT in git; store bundles need it dropped into
  mind-pack/src/main/assets/ (owner's licensed download). The v0.4 run's
  local-testing dummy was deleted before the release builds.
- Key unproven risks: S24 checklist v4 §0 (first live conversation + Gemma
  speed on Exynos 2400 — unchanged) and §0b (a real night of battery with
  the live wallpaper set).
