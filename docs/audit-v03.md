# Audit v0.3 (pre-v0.4 run) — 2026-07-18

Read-only audit of the tree at commit `af01131` ("v0.3: full state from previous
session"). Two tracks: (A) machine-verifiable checks re-run on this machine,
(B) full code audit (findings below, §3+). Method notes inline; every claim in
this file was produced by a command executed in THIS session.

## 1. SQLCipher 4.17.0 passphrase handling — bytecode re-verification

Re-run of the v0.2 `javap` analysis against the actual artifact this build
resolves: `net.zetetic:sqlcipher-android:4.17.0` (AAR in the local Gradle
cache, `classes.jar` disassembled with JDK 21 `javap -p -c`).

Findings (all confirmed from bytecode, not source):

1. **The passphrase is never zeroed by the library.** The only `Arrays.fill`
   in the entire jar is `SQLiteProgram.clearBindings()` (nulls the
   `mBindArgs` object array — bind arguments, not key material).
2. **The library aliases the caller's array.** The
   `SQLiteDatabaseConfiguration(String, int, byte[], SQLiteDatabaseHook)`
   constructor stores the `byte[]` with a plain `putfield` — no `clone()`,
   no `arraycopy`. `updateParametersFrom` likewise copies only the reference.
   `SupportOpenHelperFactory` holds `private final byte[] password` for its
   own lifetime.
3. **The key is retained for the connection lifetime by design.**
   `SQLiteConnection.open()` reads `configuration.password` on every physical
   connection open and passes it to `private static native int nativeKey(long,
   byte[])`. Zeroing our copy after first open would corrupt every subsequent
   pool (re)open — so the v0.2 decision stands: the passphrase copy is
   long-lived in process memory; mitigation remains "Keystore-wrapped key,
   process-private memory, no swap on Android".
4. **The key is not logged.** After `nativeKey` only the integer return code
   is formatted into `"Database keying operation returned:%s"` via
   `Logger.i`; keying is then validated by `SELECT COUNT(*) FROM
   sqlite_schema;`.

Verdict: **unchanged posture vs 4.6.1**; the 4.17.0 bump introduced no new
key-handling behavior. Threat-model section on in-memory key lifetime remains
accurate.

Note: §1 closes audit finding F4 below — 4.17.0 retains the caller's array
exactly as 4.6.1 did, so `SoulKeyHolder.zero()` still scrubs the only copy;
the "verified against 4.6.1" code comments are updated to 4.17.0 in this run.

## 2. Fast-follow mind-pack — bundletool --local-testing on emulator

Executed in this session on the GMD ATD image (`dev34_aosp_atd_x86_64_
Pixel_6`, API 34, booted headless outside Gradle): `bundletool 1.18.1
build-apks --local-testing` on the debug AAB (with a 65 MB dummy `.task` in
the pack slot — gitignored, local only), `install-apks`, then a scripted
UI walk (uiautomator dumps as evidence).

Results:

1. **The E2E pass immediately caught a shipping crash** — see F11 below
   (WAL pool growth vs passphrase zeroing). v0.3 as committed dies minutes
   into real use. Fixed in this run; the walk was repeated on the fixed
   build: onboarding → home → Mind screen, process alive throughout.
2. **UX gap found and fixed:** a fast-follow pack Play knows about but has
   not delivered (`NOT_INSTALLED` — exactly what local testing simulates,
   and the real sideload-then-store-install edge) rendered NO card: the
   `requestFetch()` affordance was unreachable from `Absent`. Added
   `PackPhase.NotFetched` + a Mind-screen card ("A mind is packed with the
   app… Let it come").
3. **The seam works end-to-end:** NotFetched card → "Let it come" →
   pack delivered by Play Core local-testing → `Ready` → the RAM-gate
   caution card ("The bundled mind is here… Try anyway") on the 2 GB ATD —
   honest gating confirmed. The extracted pack file appeared at
   `files/assetpacks/mind_pack/.../gemma3-1b-local-test.task` (68,157,440
   bytes — exact dummy size). The `Downloading` blink was too fast to
   capture with a 65 MB local copy; its UI branch ("The mind is on its
   way") is exercised by the existing compose state rendering, and the
   real ~530 MB store download will hold the state long enough on device
   (S24 checklist v4 re-checks it live).

## 3. Full read-only code audit (subagent, this session)

Scope: all 5 source manifests, every build.gradle.kts + version catalog,
NetworkIsolationTest, soul-vault crypto, cloud-mind, mind pipeline, data
layer, feature layer, docs-vs-code. Verified-fact inventory retained in the
session log; below are the findings and verdicts.

### Findings

- **F1 [major] Notification digest can reach the BYOK cloud endpoint.**
  `feature/notifications/.../NotificationsViewModel.kt` `buildDigest()` feeds
  up to 40 notification titles + package names into the injected
  `MindEngine` = `TieredMindEngine`, which picks CLOUD first when the cloud
  mind is enabled and online. Contradicts ADR-011 ("NEVER … the
  journal/notification tables"), threat-model.md, and two in-app claims
  (NotificationsScreen "neither can see this data", MindScreen consent card
  "Nothing else does. The soul stays here."). → FIXED in this run: digest
  pinned to local tiers; regression test added.
- **F2 [minor] Plaintext soul export lingers in cacheDir/exports** after the
  share sheet; never deleted. → FIXED: cleanup on app start + after share.
- **F3 [minor] SAF import trusts DISPLAY_NAME** (`MindModelImporter`):
  hostile `../../x.task` display name writes the staging `.part` outside
  mind-models (sandbox-bounded; commit() check fires late). → FIXED:
  sanitize to `File(name).name`, reject separators.
- **F4 [minor] Stale bytecode-verification claim** (comments say "verified
  against 4.6.1", shipped 4.17.0). → CLOSED by §1: same aliasing behavior
  re-verified on 4.17.0 bytecode; comments updated.
- **F5 [minor] Cloud API key field is not password-treated** (plain
  BasicTextField; visible + keyboard-learnable). → FIXED: password visual
  transformation + password keyboard type.
- **F6 [minor] NetworkIsolationTest scope gaps**: debug-variant only; needle
  list omits `openStream`/`SocketChannel`/`DatagramSocket`/`WebView`/
  `DownloadManager`; allowlists parse only `implementation(`/`api(` lines;
  coordinate ban lists six known libs. → PARTIALLY FIXED this run (v4 of the
  test: release manifest when present, broader needles, digest-tier pin);
  residue documented in §6.
- **F7 [note] Stale listener KDoc** ("ONLY background entity", "no INTERNET
  permission" — both false since v0.2). → FIXED: KDoc updated.
- **F8 [note] Merged manifest carries five library permissions** beyond the
  documented budget: FOREGROUND_SERVICE(_DATA_SYNC), WAKE_LOCK,
  RECEIVE_BOOT_COMPLETED, AICore BIND_SERVICE. → FIXED in docs: ADR-004
  addendum documents merged reality.
- **F9 [note] Sealed-backup restore bypasses per-fact confirmation** — the
  one bulk write path into soul_facts (defensible: "restore my own soul").
  → Documented as sanctioned exception in ADR-003 addendum.
- **F10 [note] Crash log may embed content via exception messages**
  (plaintext, local-only, user-viewed). → Accepted; truncation noted as
  future hardening.
- **F11 [critical, found by E2E in this run] Passphrase zeroing crashes WAL
  pool growth.** The v0.2/v0.3 ritual (`SoulVaultWarmer.warmUpAndScrub()`)
  zeroed the passphrase after the eager first open, assuming "the Room
  singleton keeps its connection for the process lifetime". In WAL mode the
  pool opens non-primary connections under concurrent load, and each
  physical open re-keys from the ALIASED array (§1 bytecode) — after the
  scrub that array is all zeros. Observed live on the local-testing
  emulator pass minutes after fresh onboarding:
  `SQLiteNotADatabaseException: file is not a database` in
  `SQLiteConnectionPool.tryAcquireNonPrimaryConnectionLocked` → process
  death. Sequential GMD suites never grew the pool, which is why v0.3
  shipped green. → FIXED: zeroing removed (key lives for process lifetime,
  Keystore-wrapped at rest), ADR-003 addendum v0.4, threat-model updated,
  pinned by `WalPoolKeyDeviceTest` (held write transaction + concurrent
  read forces a second connection).

### Invariant compliance (7/7 with caveats)

| # | Invariant | Verdict |
|---|-----------|---------|
| 1 | Network only in model-delivery + cloud-mind | PASS source-level; INTERNET in exactly 2 source manifests; residue = closed-source SDKs inside the app process (ADR-005 honest) |
| 2 | Memory local, DB encrypted, no plaintext leaks | PASS with caveats F2/F10 (fixed/accepted); zero Log calls repo-wide |
| 3 | Facts confirmed-only, append-only | PASS; DAO structurally append-only, device-tested on real cipher; sanctioned exception F9 |
| 4 | Never dies/guilts/blackmails | PASS; no decay/punishment in Evolution; no guilt language found; note: offline→ANXIOUS contradicts TrustScreen copy (D6, fixed) |
| 5 | No UsageStats/contacts/location | PASS; zero references, zero permissions |
| 6 | Animation visible-only, finite | PASS; repeatOnLifecycle(RESUMED)-gated loop, dt-clamped, episodes settle; widget renders one tick |
| 7 | Untrusted text never a command | PASS with documented probabilistic residue (no tools, facts gated, anti-injection prompt layers) |

### Docs-vs-code discrepancies (D1-D12)

D1 = F1 (the material one, fixed). D2 threat-model says "10 tests", file has
9 (fixed by v4 test growth — recounted). D3 ADR-005 "probes once per process"
vs per-call probing (ADR errata added). D4 ADR-011 "masked tail" vs actual
never-shown (ADR errata: behavior stronger than documented). D5 ADR-011
"antenna badge" vs "· cloud mind" text label (errata). D6 TrustScreen
"airplane mode: won't even notice" vs offline→ANXIOUS mood (copy fixed to be
honest). D7 ADR-002 getRetryDelay hints unimplemented (errata). D8 ADR-011
mid-stream error does not degrade to local for that reply (errata; offline
path does degrade). D9 = F8 (ADR-004 addendum). D10 audit-v01 vs ADR-003
addendum historical contradiction (noted, no action — history stands). D11 =
F7 (fixed). D12 handoff/audit-v02 build claims consistent with disk state.

### Coverage gaps (inherited into v0.4 DoD)

Release-variant manifest unasserted (now added when present); runtime traffic
never observed (static guarantees only); grep needles finite; build-logic
injected deps invisible to allowlists; digest-tier pin (now tested);
prompt-injection behavior probabilistic, untested; FLAG_SECURE/quiet-hours
manual-only.

