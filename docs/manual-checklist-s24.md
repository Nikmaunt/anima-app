# Manual verification checklist — Galaxy S24

What automation cannot see; run on the physical device after `./gradlew installDebug`.

## Performance & thermals
- [ ] 10 minutes on the home screen, ALERT state: GPU profiling bars stay
      under 16.6 ms; no jank when the keyboard opens; phone doesn't warm
      noticeably (creature is small-canvas; if it heats, check frame-skip in
      low-power states first).
- [ ] Background test: press Home, watch `adb shell top` — the app must drop
      to zero CPU (frame loop stops when not RESUMED); return → creature
      resumes without a dt-jump twitch.

## Concepts × states (8 × 7)
- [ ] Gallery in Settings: all 8 concepts animate live, distinct silhouettes.
- [ ] For each concept confirm readable: ALERT (darting eyes), EATING (plug a
      charger — visible feeding + celebrate bounce in real time), SLEEPY
      (battery ≤20% unplugged), ANXIOUS (airplane mode → jitter within ~30 s),
      ASLEEP (device clock past 23:00; tap → one eye half-opens), HOT (hard
      to force honestly — game + charge; UNKNOWN thermal falls back to calm),
      BORED (leave untouched 2 min).
- [ ] Tap: anticipation dip → bounce + haptic tick. Pet (drag): lean-in,
      soft eyes, purr texture (LOW_TICK — S24 supports primitives).
- [ ] Same concept, different phone (or after wipe of ANDROID_ID via new
      profile): visibly different hue/size/blink cadence (seed variance).

## Mind (expected honest result on S24)
- [ ] Chat area shows the "mind sleeps on this phone" banner and NO input —
      ML Kit Prompt API reports UNAVAILABLE on S24 (research.md §C). The
      creature must keep living fully. NO fake replies anywhere.
- [ ] On a Prompt-API device (S25/S26/Fold7, Pixel 9+): feature check →
      download flow → first conversation streams; creature sways while
      thinking; a stated personal fact triggers a "Remember this?" bar; "yes"
      lands it in Soul.

## Notification sense
- [ ] Opt-in: explainer first, system screen second; toggle appears only
      after system grant.
- [ ] Allowlist an SMS/messenger app, send yourself a login code
      ("Your code is 123456") → event must NOT appear in today's counters
      (OTP gate drops before write).
- [ ] Storm test: script ≥8 notifications in 15 min from an allowlisted app →
      creature startles, then reads anxious; journal gets a storm entry.
- [ ] Reboot phone with app killed: listener still writes to DB (encrypted
      DB opens from a cold listener start — Keystore unwrap works before the
      first Activity ever runs).

## Soul
- [ ] Export → share sheet → file lands as readable markdown (name, days,
      facts by category, moments).
- [ ] Import: copy extractor prompt → run in any other AI → paste → facts
      appear as candidates → confirm a subset → they show in Soul with
      import source.
- [ ] Uninstall/reinstall: soul is gone (backup rules exclude everything) —
      this is by design; export is the only survival path.

## Reduced motion
- [ ] System "Remove animations" ON → creature is a calm static with rare
      blinks; no drift, no wander; taps still show a static response
      (no motion), haptics still work.
- [ ] In-app "Calm motion" toggle does the same with system animations on.

## Encryption spot-check
- [ ] `adb backup` yields nothing useful (allowBackup=false).
- [ ] Pull `files/../databases/soul.db` from a rooted test device or bug
      report: file must NOT start with "SQLite format 3" plaintext magic
      (SQLCipher header instead).
