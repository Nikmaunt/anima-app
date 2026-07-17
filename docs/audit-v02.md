# Fresh v0.2 Audit (pre-v0.3 gate)

Date: 2026-07-17 · Read-only pass over HEAD `85d3896` (main) · Auditor:
independent subagent sweep + first-hand build verification in this session.

Reference repos checked first:

- **hermes-app** — clean tree (`git status` empty), history intact. Untouched.
- **hermes-lens** — same *pre-existing* state audit-v01 recorded: two
  Capacitor build-artifact files show as modified (content diff is empty —
  CRLF/LF line-ending noise only) and untracked `HERMES-LENS-OVERVIEW.md`
  dated Jul 12 (pre-dates this session). Not touched by anima work.
- **anima-app** — 8 commits (`c9d7638`→`85d3896`), tree clean at audit start.

Build verification executed THIS session (not cached claims):

- `detekt ktlintCheck testDebugUnitTest assembleDebug` — green (initially
  up-to-date from v0.2's run; then **forced re-run** `testDebugUnitTest
  --rerun-tasks`: 451 tasks executed, BUILD SUCCESSFUL).
- Test XMLs on disk: 12 test classes, **93 unique @Test methods** (135
  suite-level results counting debug+release variant duplicates), 0 failures.

## Verdicts

| # | Item | Verdict |
|---|------|---------|
| 1 | Network isolation v2 (INTERNET only via :core:model-delivery; 6-test guard over source manifests, merged manifest, sources, catalog, build files, listener allowlist) | PASS |
| 2 | Manifest hygiene (exported surface = launcher activity + widget receiver; listener & FileProvider non-exported; telemetry components stripped; allowBackup=false) | PASS |
| 3 | Append-only soul (no @Delete/@Update on facts; guarded marker UPDATEs; double-supersede no-op proven by test; only physical DELETE is the notif 7-day privacy prune) | PASS |
| 4 | Passphrase zeroing in place (`bytes.fill(0)` scrubs the same array the SQLCipher factory holds; native-residue caveat documented; corrupt `soul.key` fails loudly, GCM tag failures throw) | PASS |
| 5 | Tiered mind honesty (Nano probe → Gemma file → honest sleep; never fabricates) | PASS |
| 6 | Model delivery (SAF import + https-only resumable download; staging→commit atomic; noBackupFilesDir) | PASS |
| 7 | Reference repos untouched | PASS |
| 8 | Full build + tests green (re-executed, not cached) | PASS |
| 9 | Release lint | **GAP** — `checkReleaseBuilds = false` globally (documented MediaPipe lintVital hang). v0.3 Phase 0 item. |
| 10 | Instrumented coverage | **GAP** — zero androidTest sources; SQLCipher open/Keystore wrap/migrations/listener/widget are manual-checklist-only. v0.3 Phase 0 item (GMD). |

## Discrepancies vs the v0.2 report

1. **"147 unit tests" (handoff.md) vs 93 actual @Test methods.** The source
   tree contains 93 unique test methods; 135 is the double-counted
   debug+release suite total. Neither is 147. The v0.2 number appears to be a
   Gradle-execution count or stale; the honest figure going forward is
   **93 unique tests, all green**.
2. **README still claims v0.1 guarantees.** "No INTERNET permission in the
   merged manifest; grep-test in CI" — false since v0.2 (INTERNET exists via
   model-delivery; there is no CI at all). Fix in v0.3.
3. **No CI exists** — every guard is a local unit test that additionally
   requires a prior `assembleDebug` for the merged-manifest check.
4. **Encryption stack has no automated test** — `AnimaDatabaseTest` runs
   plain in-memory Room (its KDoc says so honestly); SQLCipher+Keystore is
   device-checklist-only. Prime GMD candidate (Phase 0.3).
5. **Model hash verification is optional** — a download without a supplied
   SHA-256 installs past only the 64 MB size floor (Gemma hashes are
   license-gated; documented owner step). Soft spot, acceptable; the PAD
   pack path (v0.3) sidesteps it for the store flow.

## Notes (no action needed)

- `@SuppressLint("HardwareIds")` in OnboardingViewModel: ANDROID_ID (per-app
  signing scoped, no permission) → FNV-1a hash → creature seed; value never
  leaves the device, degrades to a fixed seed on null. Benign; traced.
- Swallowing catches in the notification listener are deliberate (listener
  content must never reach logs) and commented; `runCatching` around
  MediaPipe close calls likewise.
- No TODO/FIXME/HACK markers anywhere in source.
- Widget battery-LOW edge still waits for the 30-min heartbeat (ADR-007
  honest gap, unchanged).
