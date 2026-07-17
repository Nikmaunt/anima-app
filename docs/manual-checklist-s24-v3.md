# Manual checklist — Galaxy S24, v0.3

v1 (basics) and v2 (mind delivery, budgets, widget) still apply. v0.3 adds
the items below. **The key unproven product risk is unchanged: the first
live conversation and Gemma speed on the Exynos 2400 have never been
measured on the real device — v2 §1 and §2 remain the most important run.**

## 0. Carry-over key item (v2 §1–2) — STILL THE HEADLINE

- [ ] First live on-device conversation on the S24: latency, tokens/s feel,
      RAM, warmth. Nothing in v0.3 changed the inference path; the GMD
      emulator deliberately never runs the real model (research-v3 §A.6).
- [ ] NEW twist: SQLCipher was bumped 4.6.1 → 4.17.0. The soul must open
      transparently over an existing v0.2 install (upgrade in place, NOT a
      fresh install: install v0.2, talk, then install v0.3 on top).
- [ ] ADR-003 residue re-verification (threat-model.md): confirm against
      sqlcipher-android 4.17.0 that `SupportOpenHelperFactory` still
      retains the passed passphrase array (zero-share claim). If it copies
      now, update SoulKeyHolder's KDoc — behavior stays safe either way.

## 1. Zero-setup pack path (ADR-010) — needs bundletool or Play track

- [ ] `bundletool build-apks --local-testing` + `install-apks` on the S24
      (with the licensed `.task` in `mind-pack/src/main/assets/`):
      fast-follow degrades to on-demand under local testing — open Mind
      screen → "The mind waits for permission" → "Let it come" → progress →
      model shows as "Delivered with the app"; Delete button absent for the
      pack model.
- [ ] Internal-testing track (production-faithful): install from Play →
      open the app during pack fetch → Mind screen shows honest progress;
      after fetch the creature talks with ZERO manual steps.
- [ ] Sideload of the plain debug APK: no pack, chain falls to SAF/download
      exactly like v0.2.
- [ ] RAM gate sanity: S24 (8 GB) must NOT show the low-memory caution.

## 2. Cloud mind (ADR-011, BYOK)

- [ ] Settings shows "Mind: local" always; flip on without config → "Cloud
      is on but not set up — the local mind keeps speaking" and the LOCAL
      tier really answers.
- [ ] Configure a real OpenAI-compatible endpoint + key (owner's). Enable →
      home header shows "· cloud mind"; replies stream; Mind screen "Right
      now" names the host.
- [ ] Airplane mode with cloud enabled → reply comes from the LOCAL tier
      (degradation per call, no setting flip); back online → cloud again.
- [ ] Wrong key → "The thought slipped away" style failure, NOT a crash;
      key re-entry works; "Forget key" disables cloud and the key file is
      gone (`ls /data/data/app.anima/no_backup/` via run-as on debug).
- [ ] Fact extraction in cloud mode still lands as candidate chips only.
- [ ] Soul markdown + sealed backup export: grep the outputs for the key —
      must be absent (automated test exists; eyeball once on device).

## 3. v0.3 creature features

- [ ] Dreams: set clock ≥23:00, open the app → creature asleep, "Wake it
      gently" pill → a dream referencing yesterday's real events (charge/
      storm day → matching imagery); second wake same night → no new dream;
      next night → different dream.
- [ ] Birthday: set date to hatch anniversary → celebratory line once, rig
      celebrates, Story timeline gains the birthday moment.
- [ ] Seasonality: change device date across Dec/Apr/Jul/Oct → background
      tint shifts subtly (winter coldest); text stays readable everywhere.
- [ ] Haptics on a real vibrator: send (tick), fact confirm/reject
      (distinct), charge celebration (rise+click), storm (thud), long-press
      remember (long-press buzz), purr unchanged. On silent/haptics-off
      system setting the view-level ones must obey the system.
- [ ] Regeneration: "Say it differently" appends a NEW answer (old one
      stays); works on Gemma and cloud tiers.
- [ ] "Remember this": long-press any bubble → candidate chip → confirm →
      in Soul with source =~ chat.
- [ ] Charge chart: after a day of normal use, 24h view shows the line with
      honest gaps; storms as dots; correlation line appears only with
      enough data (else the honest not-enough-data words).

## 4. v0.3 security surfaces

- [ ] Soul screen: screenshot blocked (system toast) + app-switcher
      thumbnail blank; Settings toggle re-allows; chat screen screenshots
      still work.
- [ ] Crash log: `adb shell am crash app.anima` (or a debug crash) → Settings
      → "Last crash" shows it; Copy puts it on the clipboard; Clear empties.
- [ ] Onboarding is 3 screens (hatch → body → name+begin) and the first
      conversation is reachable in under 60 s on the stopwatch.
- [ ] Notification access: only reachable via Settings → Senses; the system
      screen opens from the creature-voiced explainer; denial changes
      nothing else.
- [ ] Trust page reads correctly in both cloud states (off: "It is OFF
      right now"; on: names the host).
