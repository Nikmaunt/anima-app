# Manual checklist — Galaxy S24, v0.4

v1-v3 still apply where not superseded. v0.4's §0 is UNCHANGED by demand:
the first live conversation and Gemma speed on the Exynos 2400 remain
unmeasured on the real device. §0b is new: a real night of battery with the
live wallpaper set.

## 0. Carry-over headline (v2 §1-2, v3 §0) — STILL THE HEADLINE

- [ ] First live on-device conversation on the S24: latency, tokens/s feel,
      RAM, warmth. Nothing in v0.4 changed the inference path.
- [ ] Upgrade-in-place over v0.3: soul opens transparently; **v0.4 removed
      the passphrase zeroing ritual (ADR-003 addendum v0.4)** — after
      upgrade, use the app for 10+ minutes with the diary AND chat open
      (concurrent DB load): no SQLiteNotADatabaseException, ever. This is
      the on-device confirmation of the WAL pool-growth fix.
- [ ] The 4.17.0 bytecode re-verification is DONE this run (audit-v03 §1) —
      nothing left to check here; item retired.

## 0b. NEW HEADLINE №2 — a real night with the live wallpaper

- [ ] Set "Anima — your creature" as the wallpaper (system picker → Live
      wallpapers). Confirm the still pose matches the current mood.
- [ ] Battery baseline night (wallpaper NOT set): note % at bedtime and at
      wake. Then a comparable night WITH the wallpaper set. Expected delta:
      ≈0 (the engine draws only on state changes; steady state is 0 fps).
      If the wallpaper night is >1-2% worse, capture `adb shell dumpsys
      batterystats` and file it — the budget failed and the feature must
      be pulled per ADR-012.
- [ ] AOD on (Samsung): wallpaper must NOT appear on AOD; after unlocking,
      one redraw happens. Watch for the Samsung missed-visibility pitfall:
      leave AOD on for an hour — the app process must not accumulate CPU
      (Settings → Battery → app usage).
- [ ] Rotate + theme switch: exactly one redraw each (no flicker loops).

## 1. Rest together (the v0.4 core)

- [ ] Start a 5-minute rest; screen dims; timer runs; creature sleeps.
- [ ] Leave the app at ~2 min → return after 5+ min: session WAITS (no
      loss, no guilt copy), "you're back — I kept your place" appears,
      timer continues from 2:00, completes at 5:00 total rest time.
- [ ] Kill the app mid-session → relaunch: session is simply gone; no
      trace in the diary, no sad state anywhere.
- [ ] Complete a session: diary week shows "Rests together 1" + minutes;
      totals only ever grow across sessions.
- [ ] "keep the screen awake" toggle works; brightness restores on exit.
- [ ] While charging, the pick panel says "rest with me while I eat?".

## 2. QS tile + shortcuts (ADR-015)

- [ ] Add the "Rest together" tile from the QS edit sheet; tap → app opens
      on the rest screen (cold start AND warm).
- [ ] During a running session the tile shows active + "N min".
- [ ] Long-press launcher icon: Talk / Diary / Rest shortcuts; each lands
      on the right screen; Rest works from a cold start.

## 3. Creature voice (ADR-013) — Samsung TTS reality check

- [ ] Settings → Voice: toggle ON → availability check runs. On the S24
      confirm which engine serves (Samsung TTS default): if no offline
      voice for the UI language, the honest mute card + settings deep link
      must show (verify the deep link opens TTS settings on One UI).
- [ ] With an offline voice (airplane mode ON to prove it): chat reply is
      spoken; pitch differs between two concepts (switch body, speak
      again).
- [ ] Screen off / navigate away mid-speech → speech stops.
- [ ] Default state after fresh install is OFF.

## 4. Battery care card (research-v4 §9)

- [ ] After a week of normal use, diary shows care points + at most ONE
      advice line; wording matches the shipped strings (no doom, no
      percentages).
- [ ] If you charge overnight ≥3 nights: the advice names the OS feature
      ("Battery protection") — confirm the wording matches One UI 6.1+
      naming on the S24.

## 5. Wardrobe / milestones (if shipped in the final tree)

- [ ] Unlocks reflect real counters (days/facts/rests); nothing ever
      re-locks; no timers, no FOMO copy anywhere.

## 6. Postcard (if shipped in the final tree)

- [ ] Share sheet gets a PNG: creature + mood caption + small wordmark;
      no other app data in the image; cache file cleaned on next launch.

## 7. Regression sweep

- [ ] NetworkIsolationTest guarantees hold live: airplane mode → everything
      works; the only network touches remain model delivery + BYOK.
- [ ] Widget still renders after the StillRender refactor (add fresh
      widget, force a charge edge).
- [ ] Notification digest with BYOK cloud ENABLED and online: digest must
      still work and the reply must come from the LOCAL tier (check the
      Mind screen tier indicator stays local for the digest; cloud replies
      in chat may still be cloud).
- [ ] Локализация (если вошла в финальное дерево): переключить язык
      системы на RU — все экраны на русском, псевдолокали en-XA/ar-XB в
      debug-сборке не ломают вёрстку ключевых экранов.
