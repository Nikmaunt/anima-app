# Handoff after the v1.1c run

You are a new Claude Code session with none of the previous context. This file
is written for you. Everything in it is checkable — follow the commands rather
than trusting the prose, because **the prose in this repository has been wrong
before, and the biggest finding of the last run was exactly that.**

`docs/handoff-v11.md` is **superseded**. It describes HEAD `832bde2` and 69
commits, and one of its stated invariants was false when it was written.

Read in this order: this file, then `docs/design/v11/phase0-v11c-audit.md`
(what the documents claimed vs what the code did), then the phase reports
`phase1-wrong-body.md`, `phase3-identity-repair.md`, `phase4-moth.md`,
`phase6-widget-wallpaper.md`.

---

## а) State

| Fact | Value | How to verify |
|---|---|---|
| Branch | `main`, tracking `origin/main` | `git branch -vv` |
| Remote | `https://github.com/Nikmaunt/anima-app.git`, private | `git remote -v` |
| versionName / versionCode | `1.1.1` / `12` | `app/build.gradle.kts:104-105` |
| Toolchain | AGP 8.13.2, Kotlin 2.2.0, composeBom 2025.06.01, compileSdk 36, minSdk 31 | `gradle/libs.versions.toml` |
| Emulator | `look35`, API 35 google_apis, 1080×2340, 420 dpi | `D:/Android/avd/look35.avd/` |
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk`, **210 798 175 B (202 MiB)** | `ls -la` — large because four ABIs of native inference engines ride along; installing to an emulator is slow, not hung |

### Green

```
./gradlew -Panima.ci=true check --no-build-cache
python3 tools/ci-verify.py --log <that run's log> --min-executions 30 --min-goldens 40
```

→ 36 declared test tasks, 36 executed, 0 recalled; 92 goldens compared, 0
changed; **493 executions**, 0 failures, 7 skipped (all env-gated
`tools/litertlm-smoke`).

**Say "executions", not "tests".** Android modules run every test twice, debug
and release. 493 executions is 54 distinct (module, class) pairs. Three runs
have now quoted this number as "tests" and the numbers are not comparable.
ADR-024.

---

## б) The thing you most need to know

**Before this run the body was chosen from a menu, and four documents said it
was assigned.**

`docs/handoff-v11.md:415` — "The body is assigned, never chosen. No catalogue of
the eight concepts anywhere in the app." `docs/audit-v10.md:189` — the same.
`phase5-2-scale.md:30` — the same. The prompt for this run built two tasks on
the same premise.

The code showed a gallery of all eight at hatch (`OnboardingStage.CHOOSE`) and
replaced the body on one tap in Settings, and the onboarding string said so in
plain words. Nobody had ever run the grep.

It is true now: `CreatureConcept.assignedTo(seed)` in `core/model`. But the
lesson generalises and is the reason this file opens the way it does — **check
the claim against the code before you build on it.** The command that found it:

```bash
grep -rn "CreatureConcept.entries\|conceptFor" --include=*.kt core/ feature/ app/src | grep "/src/main/"
```

---

## в) Landmarks you will need

- **The CI contour**: `-Panima.ci=true` (root `build.gradle.kts`) +
  `tools/ci-verify.py` + `CiContourTest` + `roborazzi.test.verify=true` in
  `gradle.properties`. ADR-024 explains why all four exist.
- **Identity**: `core/model/BodyAssignment.kt` (seed → body, a tournament),
  `CreatureName.kt` (seed → name), `IdentityOrigin.kt` (who may be repaired),
  `core/data/identity/DeviceSeed.kt`, `core/data/identity/IdentityRepair.kt`.
- **Guards that hold claims about absence**: `NoDefaultBodyTest` (no default
  body, no default seed, no gallery on any screen, nothing leaves the phone
  carrying an invented body), `ChatDemotionTest`, `NetworkIsolationTest`,
  `CiContourTest`.
- **Surfaces**: `feature/wallpaper/WallpaperFrame.kt` is a pure function now —
  `WallpaperFrameDumpTest` and `WidgetFrameDumpTest` render every body to PNG on
  a device image so they can be opened.
- **Design**: `core/ui/components/Layout.kt` (`ActionRow`, `StatRow`,
  `EmptyState`, `ScrollableChipRow`, `Plate`, `Modifier.pressable`),
  `theme/SignatureHue.kt`, `docs/design-system-v11.md`.

---

## г) Environment traps — read twice

### 1. A green build that measured nothing (the one that keeps coming back)

Third occurrence in three runs. `-Proborazzi.test.verify=true` is not a task
input, so Gradle keeps every test task UP-TO-DATE and prints BUILD SUCCESSFUL in
six seconds. Reproduced again at the top of this run.

**Always** `-Panima.ci=true`, and **always** run `tools/ci-verify.py` on the log
afterwards. The verifier fails on a recalled task, a SKIPPED
`finalizeTestRoborazzi*`, a silently re-recorded golden, or no results at all.

### 2. Re-recording goldens now needs BOTH flags

`roborazzi.test.verify=true` is the repository default, and verify wins over
record. To re-record:

```
./gradlew :module:testDebugUnitTest -Proborazzi.test.verify=false -Proborazzi.test.record=true
```

### 3. A library's androidTest APK declares the library's services

`app.anima.feature.wallpaper.test` declares the **same** `WallpaperService` with
the same label. The system wallpaper picker then lists two Animas, and the test
one has no Hilt application, so it draws a black screen. This cost most of an
hour and nearly a false bug report. **Uninstall `*.test` packages before
touching the wallpaper or the widget.**

### 4. `adb pull /sdcard/...` from Git Bash

Becomes `C:/Program Files/Git/sdcard/...`. Pull from PowerShell.

### 5. Setting the live wallpaper without a system permission

`cmd wallpaper` cannot do it and `SET_WALLPAPER_COMPONENT` is not grantable.
What works: open `com.android.wallpaper.livepicker/.LiveWallpaperActivity`, then
**keyboard-navigate** — `input keyevent KEYCODE_DPAD_DOWN` then
`KEYCODE_DPAD_CENTER`. `input tap` on the list row does nothing, because the
ListView sits behind the action bar.

Do **not** `am force-stop app.anima` while its wallpaper is live — the system
falls back to the default wallpaper and you have to set it again.

### 6. The emulator dies

It hung and stopped answering `adb devices` at the end of this run; killed with
`Stop-Process` on `emulator` and `qemu-system-x86_64`. If it will not start,
delete `*.lock` and the `snapshots` folder in `D:/Android/avd/look35.avd/`.
Recreating the AVD is not needed. **Do not use `aosp_atd` or headless images to
judge appearance.** This emulator is AOSP, not One UI.

### 7. Compose rejects negative padding

`Modifier.padding(horizontal = (-24).dp)` throws "Padding must be non-negative"
at draw time, not compile time. It took out six Rest goldens at once.

### 8. Still true from before

`DayInLifeTest` hangs on soul import — do not use it to navigate the app.
`RigGoldenTest` cannot measure size. `GlowSkinFalloffTest` knows nothing about
the frame. The Soul screen screenshots as pure black unless Settings →
«Скриншоты экрана души» is on. Never run two Gradle invocations at once.

---

## д) Invariants you must not break

1. **Network belongs to exactly two modules** — `core:model-delivery` and
   `core:cloud-mind`. `NetworkIsolationTest` (14 tests) fails the build
   otherwise and pins the merged permission set as an exact allowlist.
2. **Zero analytics, zero crash reporting.**
3. **Animation only on a visible screen; wallpaper 0 fps at rest.**
4. **Untrusted text is never a command.**
5. **Chat and `:mind-pack` are demoted, never deleted.** `ChatDemotionTest`.
6. **Contracts evolve additively.**
7. **The body is assigned, never chosen** — and now this is true in the code.
   `NoDefaultBodyTest` fails if any screen composes `ConceptGallery`.
8. **A body is never substituted or defaulted.** Until identity is read, draw
   and write nobody.
9. **`IdentityRepair` may only touch a soul this build hatched.** A CHOSEN or
   TRANSFERRED body is reported and left alone. Getting this wrong deletes a
   lived identity.

Process: a commit per finished stage; a clean tree at handoff; every claim needs
an artefact; **look at screenshots with your own eyes**; do not decide product
questions — disagreeing on the merits, in writing, is welcome and has been right
five times now.

---

## е) Open questions for the owner

1. **Does One UI honour `HINT_SUPPORTS_DARK_TEXT`?** The wallpaper's light day
   frame leaves the launcher's labels nearly invisible on the emulator.
   `onComputeColors()` is implemented and changed nothing there. If One UI
   ignores it too, the day background has to be darkened — and that colour is
   the app's paper colour, so it is a product decision.
2. **`MOTH`.** It is no longer a butterfly and it is not clearly a moth. The
   recommendation is recorded with a correction to the premise: `SPROUT` (a
   daisy with a face) and `EMBER` (a kite with a face) now look weaker *as
   ideas* than the moth does. Composition of the set is yours; the mechanism is
   safe either way, because `assignedTo` is stable under set changes.
3. **The repair rule.** This run executed task 3 in substance and not literally:
   a soul whose body was CHOSEN is never repaired, because introducing
   `f(seed)` and applying it to everyone would have marked seven souls in eight
   corrupt, including yours. The v1.1 handoff carries your opposite instruction
   verbatim for the same case. Confirm the rule.
4. **The two-pane threshold on a Fold in portrait** is now single-column. If you
   want two panes on an unfolded Fold held upright, say so.

---

## ж) The independent design review, verbatim

A fresh read-only agent was given the screenshots and nothing else — no
documentation, no source, no history — and asked what looks dated, where the
hierarchy breaks, what reads as accidental, and what it would fix first. Its
verdict is quoted in full in the run report and summarised here as work:

- **Nav row on Home** — four bare text labels, no icons, no container, no
  selected state, ~40px touch targets. Called "a 2013 ActionBar tab strip". It
  also noticed that "Настройки" is a different hue from its three siblings; that
  is `GhostButton(quiet = true)` → `textDim`, which stayed neutral when the
  accent became creature-derived. **Not actioned.** Re-tinting the text tokens
  would put `SignatureHueContrastTest`'s 360-hue proof at risk, and that is not
  a change to make at the end of a run.
- **The energy chart in the Diary** — three hairlines, one dot, no axes, no
  labels. "Communicates nothing." **Not actioned.**
- **The passport's ТЕЛО card** — a two-line grey caption sits between "Геном"
  and its value, so the value column stops aligning. **Not actioned.**
- **Cards read as CardView-era** — one surface tone, one flat elevation,
  ALL-CAPS letterspaced section labels. **Not actioned.**
- Two things it found **were** actioned in this run: the Soul action row
  collapsing "Открытка" to one letter per line, and Rest's content packed into
  the top 45%.

One disagreement, recorded rather than hidden: it says Rest's "25 мин" chip is
clipped "with no fade, no scroll cue". There *is* a fade — `ScrollableChipRow`,
visible in `after-v11c/06-rest.png`. Either it read a stale copy of the file or
the fade is too subtle to register at that size. Worth checking on a device
before deciding which.

---

## з) Left unverified

- **Everything about One UI.** The emulator is AOSP.
- **The fixed Soul screen was not re-photographed** — the emulator hung right
  after the fix. It is a swap to two already-tested FlowRow primitives, but it
  is a layout claim with no picture behind it. Item 4 on the device checklist.
- **The widget has never been placed on a launcher.** Android has no shell
  command and Launcher3 needs a drag. The frames are rendered and looked at; the
  placement path (`requestPinAppWidget` from onboarding) is written and
  unexercised.
- **The wallpaper's frame budget in real life.** 0 fps at rest cannot be proven
  on an emulator.
- **The hardware `GlowSkin` branch.** `StillRender` — widget, wallpaper,
  goldens — takes the software path. The app's own screen takes the hardware
  one, where audit-v10 §D5 found the glow clipped by a rectangle. Unmeasured
  this run.
- **`DayInLifeTest` / the whole E2E floor.** Still hanging.
- **Nothing was checked on a physical device by this session.**
