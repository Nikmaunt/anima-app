# Fresh v0.1 Audit (pre-v0.2 gate)

Date: 2026-07-17 · Auditor: independent read-only pass over HEAD `c79aeb1` ·
Build verified: `assembleDebug test` green on this machine before any v0.2 change.

The previous DoD audit ended without a completion record; this is the fresh
re-run required before any new code. Reference repos checked first:

- **hermes-app** — clean tree, history intact. Untouched.
- **hermes-lens** — history intact; the working tree carries *pre-existing*
  uncommitted Capacitor build-artifact changes (`android/app/capacitor.build.gradle`,
  `android/capacitor.settings.gradle`) and an untracked `HERMES-LENS-OVERVIEW.md`.
  Not produced by anima work (different stack, different dates); left untouched.
- **anima-app** — 6 commits (`c9d7638`→`c79aeb1`), tree clean.

## Verdicts

| # | Item | Verdict |
|---|------|---------|
| 1 | Manifest: no INTERNET, DataTransport telemetry stripped | PASS |
| 2a | No network deps in catalog or build files | PASS |
| 2b | NetworkIsolationTest efficacy | **CONCERN** (below) |
| 3 | Background entities = notification listener only | PASS |
| 4a | Soul DB encrypted (SQLCipher, Keystore-wrapped random key) | PASS |
| 4b | Passphrase zeroing after DB open | **CONCERN** (below) |
| 5 | detekt/ktlint | absent — acknowledged debt |
| 6 | R8/minify + proguard | disabled — acknowledged debt |
| 7 | Baseline profile | absent |
| 8 | Compose UI tests | absent (catalog declares test libs nobody consumes) |
| 9 | Reduced-motion: system setting | **CONFIRMED debt** — read once in `remember`, reacts only via recreation; in-app `calm_motion` toggle is reactive |
| 10 | Frame loop: RESUMED-gated, dt-capped (100 ms clamp + max 4 substeps) | PASS |
| 11 | Docs vs code | **2 overclaims** (below) |

## Discrepancies vs the v0.1 report (fixed in v0.2, commits referenced there)

1. **ADR-003 overclaim — passphrase zeroing.** ADR-003 says the passphrase is
   "zeroed after opening the DB". App code never zeroes it: `DataModule.database()`
   passes the array to the single-arg `SupportOpenHelperFactory(byte[])` and drops
   the reference; `KeystoreSoulKeySource` returns fresh/unwrapped arrays unscrubbed.
   Mitigating fact found during this audit: the single-arg factory constructor
   delegates with `clearPassphrase = true`, so SQLCipher zeroes the array *lazily at
   first open* — the guarantee held by an unstated library default, not by our code.
2. **ADR-004 overclaim — test efficacy.** The merged-manifest assertion in
   `NetworkIsolationTest` filters candidate files with `.exists()`: on a bare
   `gradlew test` with no prior `assembleDebug` the list is empty and the test
   passes vacuously. Also, the "no network dependency" grep covers only the
   version catalog and the notifications module's build file — any *other* module
   could add a hardcoded `implementation("com.squareup.okhttp3:...")` coordinate
   without failing a test.
3. **UI-test debt was not explicitly recorded** in v0.1 docs (the other debts
   were). Recorded here: zero Compose UI tests existed at `c79aeb1`.

Everything else matches the v0.1 report: core guarantees hold in shipped code.

## Actions taken in v0.2 (Phase 0.2)

- Explicit, eager passphrase zeroing + ADR-003 corrected to describe the real
  mechanism and its residue window (see ADR-003 addendum).
- NetworkIsolationTest rewritten: merged-manifest check **fails** when no build
  output exists (test asserts at least one manifest candidate is present after
  `assembleDebug`; `check` task ordering enforced in Gradle), and the dependency
  grep extended to every `build.gradle.kts` in the repo, with the network
  exception scoped to `:core:model-delivery` only (Phase 1).
- detekt + ktlint wired; R8 + proguard keeps; Compose UI tests added;
  reduced-motion made observable. Baseline profile: see ADR-006 (device-bound
  generation, honest deferral if no device available on this machine).
