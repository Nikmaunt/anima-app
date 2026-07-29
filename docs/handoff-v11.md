# Handoff after the v1.1 design run

You are a new Claude Code session with none of the previous context. This
file is written for you, not for the owner. Everything here is checkable —
follow the paths and commands rather than trusting the prose.

Read these three in this order before touching anything:
`docs/audit-v10.md` (what was broken and why), `docs/design-system-v11.md`
(the visual contract you must not casually violate), and this file.

---

## а) State

| Fact | Value | How to verify |
|---|---|---|
| Branch | `main` | `git rev-parse --abbrev-ref HEAD` |
| Remote | `https://github.com/Nikmaunt/anima-app.git`, **private** | `git remote -v`, `gh repo view Nikmaunt/anima-app --json visibility` |
| HEAD at handoff | `832bde2` | `git log --oneline -1` |
| Commits in history | **69** (the v1.1 run added the top 7) | `git rev-list --count HEAD` |
| versionName / versionCode | `1.0.0` / `10` — **not bumped by this run** | `app/build.gradle.kts:104-105` |
| Toolchain, pinned | AGP 8.13.2, Kotlin 2.2.0, composeBom 2025.06.01, navigation 2.9.8, compileSdk 36, **minSdk 31** | `gradle/libs.versions.toml:12,13,19,20`; `build-logic/src/main/kotlin/KotlinAndroid.kt:20,23` |

### Green

`gradlew check -Proborazzi.test.verify=true` → **BUILD SUCCESSFUL**,
**392 tests, 0 failures, 0 skipped** (count summed from
`*/build/test-results/**/TEST-*.xml`, excluding `tools/litertlm-smoke`
which is env-gated and contributes 27 more with 7 skipped).

**You must pass `-Proborazzi.test.verify=true`.** Without it the screenshot
assertions do nothing — see the traps section, this is the single most
important thing in this file.

- `NetworkIsolationTest` — 14 tests, 0 failures. The source really has 14
  `@Test` (`grep -c "@Test" app/src/test/kotlin/app/anima/NetworkIsolationTest.kt`).
  Older prompts and docs say 13; 13 is wrong.
- `app-release.aab` — 53 390 289 B, contains exactly one module, `base`,
  with no entry matching `mind`. Check with
  `python3 -c "import zipfile;print(sorted({n.split('/')[0] for n in zipfile.ZipFile('app/build/outputs/bundle/release/app-release.aab').namelist() if '/' in n}))"`
- `app-debug.apk` — ~201 MiB (211 021 147 B). Large because four ABIs of
  native inference engines ride along. Installing it to an emulator takes
  a while; that is normal, not a hang.

### Red / not green

Nothing in `check` is red at handoff. Two honest qualifiers:

1. `check` **was** red on untouched `main` before this run —
  `:core:body:lintDebug` failed `MissingPermission`. Fixed in `8b754c4`.
  See traps.
2. The rig goldens are only meaningful in verify mode. In a plain `check`
  they are decorative.

---

## б) What the v1.1 run did, and where the artefacts are

| Commit | Phase | Artefact |
|---|---|---|
| `0d7f185` | 0 — looked at the app on a real emulator for the first time | `docs/design/v11/phase0-emulator-eyes.md`, 27 screenshots in `docs/design/v11/before/` |
| `cb92977` | 1 — research | `docs/research-v11-design.md` |
| `3aa2dab` | 2 — design system | `docs/design-system-v11.md`, catalogue screenshots in `docs/design/v11/catalog/` |
| `8b754c4` | 3 — Home reworked, chat demoted | `docs/design/v11/phase3-home.md`, `docs/design/v11/after/` |
| `bd75452` | 3 follow-up — the four things the previous session called bad on Home | same doc, appended section |
| `265688e` | 5.1 — `StillRender` ran zero ticks | `docs/design/v11/phase5-1-stillrender.md`, before/after sheets in `docs/design/v11/rig/` |
| `832bde2` | 5.2–5.4 — one frame scale, micromotion, contact sheet | `docs/design/v11/phase5-2-scale.md`, `docs/design/v11/contact/` |

Phase 4 was **never started**. Phase 5 was deliberately run before Phase 4
on the owner's instruction, because body scale is a cross-cutting value and
screens cannot be laid out finally until it is settled.

### Code landmarks you will need

- Design tokens: `core/ui/src/main/kotlin/app/anima/core/ui/theme/` —
  `AnimaTypography.kt` (type scale, roles named by job),
  `SignatureHue.kt` (accent derived from the creature's genome),
  `AnimaMotion.kt` (M3 Expressive spring numbers, transcribed),
  `AnimaSpacing.kt`.
- Layout primitives that exist to make defect D1 unrepeatable:
  `core/ui/src/main/kotlin/app/anima/core/ui/components/Layout.kt` —
  `HeroStat`, `StatRow`, `ActionRow`, `LabeledControl`, `EmptyState`,
  `Plate`, `Group`/`GroupColumn`, `Modifier.pressable`.
  `Enter.kt` has `Modifier.enterStaggered`.
- Body scale calibration: `core/creature/.../render/RigScale.kt`,
  enforced by `core/creature/src/test/.../render/FrameScaleTest.kt`.
- Chat demotion is held by `app/src/test/kotlin/app/anima/ChatDemotionTest.kt`
  (6 tests). It reads sources as text, on purpose: these are claims about
  what is *absent*.
- A debug-only design catalogue lives at
  `app/src/debug/kotlin/app/anima/DesignCatalogActivity.kt`. Launch:
  `adb shell am start -n app.anima/app.anima.DesignCatalogActivity`,
  optional extras `--ei hue 280`, `--ez dark false`. It cannot ship: the
  activity is declared only in `app/src/debug/AndroidManifest.xml`.

---

## в) Open work — the owner's decisions, verbatim

These are quoted, not paraphrased. Do not re-decide them.

### Grounding the three transparent bodies — owner named the task

Owner's wording for this item: **«заземление трёх обликов (SPIRIT_ORB,
PIXEL_PET, MOTH)»**. Read that as the decision: they are to be grounded,
i.e. made to read as a silhouette against any backdrop. It is not an open
question about *whether*.

The finding it comes from: the contact sheet
(`docs/design/v11/contact/`) shows all three failing the criterion "the
silhouette reads on all three backdrops without leaning on the background"
— over a busy photo the wallpaper becomes part of the body, because all
three are built on transparency (the orb by design, the pixel pet's grid
cells, the moth's wings). `JELLY` is borderline: the bell reads, the
tentacles do not. Separately, `EMBER`'s `asleep` state is half the size of
its own other three states.

What was NOT settled and what you should still ask: *how* to ground them.
The previous session declined to invent it, because opacity changes what
the orb *is*, and that is a character decision. Bring the owner a proposal,
do not silently pick one.

### The wrong body flashing at launch — treat it as a class

Owner's wording: **«чужое тело при запуске и класс дефолтных концептов»**.
The second half is the instruction: this is not one Home bug, it is the
class of places that fall back to a hardcoded default concept while real
identity loads. Find all of them before fixing any.

The class was enumerated for you. Command:

```bash
grep -rn "SPIRIT_ORB" --include=*.kt core/ feature/ app/src/main | grep -v "/test"
```

Nine non-test fallback sites, all `?: SPIRIT_ORB`:

| Site | Note |
|---|---|
| `feature/home/.../HomeViewModel.kt:761` | this is the one that produces the visible flash on Home |
| `feature/home/.../BodyDiaryScreen.kt:150` | |
| `feature/rest/.../RestViewModel.kt:67` | |
| `feature/settings/.../SettingsScreen.kt:161` | `effectiveConcept` |
| `feature/settings/.../WardrobeScreen.kt:82` | plus the `WardrobeUiState` field default |
| `feature/onboarding/.../OnboardingScreen.kt:86` | legitimate — nothing is hatched yet |
| `core/data/.../SoulBackup.kt:55,132` | **export and restore.** A wrong default here does not flash, it *persists* — worth looking at first |
| `core/model/.../CreatureConcept.kt:37` | `fromWire` fallback for unknown wire values |
| `WidgetSnapshot` | `runCatching` fallbacks to `SPIRIT_ORB` / seed `0L`, recorded in `docs/audit-v10.md` §0.4 |

Not every one is wrong — onboarding genuinely has no body yet. The two in
`SoulBackup` deserve the most suspicion, because there the fallback is
written to durable data rather than to one frame.

Reproduce the flash by screenshotting Home within ~12 s of `am start`;
documented in `docs/design/v11/phase5-2-scale.md`, final section. For a
product whose promise is "this body belongs to this phone", the first frame
showing someone else's body is a product defect, not a cosmetic one.

### Home layout

Owner: *"Home принимаю условно — окончательный вердикт после того, как
масштаб тела будет решён."*

The vertical void above and below the creature did **not** close after the
scale work, and the reason is now known: renderers size from
`size.minDimension`, so on a portrait screen the body scales to WIDTH and
surplus height becomes margin at any calibration factor. Raising the factor
further is not available — `PIXEL_PET` already drops a pixel outside the
frame. Two options were put to the owner (cap the creature box height and
give the surplus to content; or seat the body lower); **no answer yet.**

### FrameScaleTest only measures ALERT

`FrameScaleTest` renders `Mood.ALERT` only, which is why it did not catch
`EMBER`'s undersized `asleep`. Owner listed extending it to four states as
open work. Straightforward, uncontroversial, do it.

### The creature passport, replacing the body grid

Owner, verbatim:

> Паспорт существа вместо сетки. Read-only, ничего выбрать нельзя.
>
> Состав секции ТЕЛО:
> — крупный живой портрет назначенного тела (тот же движок, не статика);
> — имя, переименование остаётся;
> — дата вылупления;
> — отпечаток генома (короткая строка, читаемая, не hex-простыня);
> — устройство, которому принадлежит тело.
>
> Под этим — текст, дословно:
> «Это тело — тело этого телефона. Другой телефон — другое тело.»
>
> Внизу секции — ссылка в раздел Души (перенос, реестр аппаратов), а не в
> выбор облика и не в покупку. Тело неотчуждаемо и бесплатно.
>
> Каталог восьми обликов не показывать НИГДЕ в приложении.
>
> Существующая настройка выбора остаётся в коде, но недостижима из UI.
> Миграция: если в состоянии сохранён облик, отличный от сидового,
> оставить как есть, не переписывать.

The grid to remove is the `ТЕЛО` section of
`feature/settings/src/main/kotlin/app/anima/feature/settings/SettingsScreen.kt`
(before-state screenshot: `docs/design/v11/before/12-settings.png`).

### Forms (`WardrobeScreen`)

Checked by reading the code, not guessing: `WardrobeUiState.concept` is a
**single** concept from `identity.concept()`, and the screen draws that one
body six times at different `paletteShiftDeg`, gated by
`Milestones.board` (`feature/settings/.../WardrobeScreen.kt:47-88`,
screenshot `docs/design/v11/before/19-wardrobe.png`). It is **not** a
catalogue of the eight bodies.

Owner's decisions, verbatim:

> — это сдвиги генома назначенного тела, значит принцип «ничего чужого не
>   выбирается» не нарушен; выбор варианта разрешён;
> — акцент приложения следует за НАДЕТЫМ вариантом, не за базовым геномом;
> — вход в них переезжает ВНУТРЬ паспорта, отдельной кнопки в Настройках
>   нет;
> — название «Формы» неверно, меняется тон, а не форма — переименовать
>   в Фазе 4 вместе с локализацией вех из Milestones.kt.

Note the second bullet is a concrete instruction about `SignatureHue`:
resolve the app accent from the **worn** palette variant, i.e. include
`paletteShiftDeg`, not just `signatureHue(concept) + genome.hueShiftDeg`.

### Phase 4 in full — blocked

Owner: *"Фаза 4 — после 5.4 и моего вердикта по пересмотренному Home."*

Scope: Отдых, Душа, Дневник тела, «Я и Anima», Настройки, онбординг, plus
defects D2, D4, D6, D9. Onboarding target shape was fixed earlier:
вылупление → явление тела и имени → установка виджета.

**D4 is bigger than it looks.** Only four keys are missing from
`values-ru`, and they are harmless (`app_name`,
`notif_package_hint`, `mind_url_placeholder`, `mind_cloud_url_placeholder`).
The real problem is **23 user-visible English literals hardcoded in
`core/model`, a module with no `res/` at all**:
`core/model/src/main/kotlin/app/anima/core/model/Evolution.kt:116-157`
(story milestones) and `.../Milestones.kt:21-66` (palette names and unlock
conditions). Visible in `docs/design/v11/before/15-story.png` and
`19-wardrobe.png`.

### Also open, smaller

- The app accent is derived per-creature and proven safe across all 360
  hues (`SignatureHueContrastTest`), but **is not switched on for real
  screens**: `AnimaTheme` is called without `creatureHueDeg`. Turning it on
  recolours every screen at once, which is why the previous session, scoped
  to Home only, did not.
- `SharedTransitionLayout` is available on the pinned BOM (verified by
  unpacking `animation-release.aar`, see `docs/research-v11-design.md`
  §1.2), so the creature can travel between screens as a shared element.
  Not done — needs Rest and Soul touched.
- `Modifier.pressable` exists in `core:ui` but is not wired anywhere.

---

## г) Environment traps — read this section twice

### Goldens are not compared unless you ask

`captureRoboImage` asserts **nothing** in a normal run. Proof from this
run: `:core:ui:testDebugUnitTest` passed with deliberately changed
typography; the same task with `-Proborazzi.test.verify=true` failed
`DesignSystemGoldenTest`.

- verify: `gradlew check -Proborazzi.test.verify=true`
- re-record: `gradlew check -Proborazzi.test.record=true`

Any claim of the form "N goldens green" is meaningless without the verify
flag. Historic run reports in this repo were written without it.

### lint was failing on untouched `main`, hidden by Gradle's task cache

`:core:body:lintDebug` failed `MissingPermission` on
`BodySensors.kt:106-107` on a clean tree — verified by
`git stash -u && gradlew :core:body:lintDebug --rerun-tasks`. Fixing it
surfaced four more errors of the same age behind it (VIBRATE in
`core:creature`, `NewApi` on `RuntimeShader`, `ImpliedQuantity` in
`values-ru`/`values-pl`, `MissingClass` on `tools:node="remove"`). All
fixed in `8b754c4`; the reasoning per error is in
`docs/design/v11/phase3-home.md`.

**Generalise the lesson:** when a task looks green, check it actually ran.
`--rerun-tasks` is how you find out.

Library modules that had **no** `AndroidManifest.xml` at all had their lint
silently under-informed. `core/body` and `core/creature` now have one whose
only content is a `uses-permission` the module genuinely needs.

### DayInLifeTest hangs

`app`'s E2E hangs on soul import right after `Espresso.closeSoftKeyboard()`
(`DayInLifeTest.kt:258`). Full trace and reproduction in
`docs/e2e-defect-2026-07.md`. The 240 s `TIMEOUT_MILLIS` guards only
`waitForCondition`, so the hang is outside it and the test sat for 13
minutes instead of failing in 4. **Do not use it to navigate the app** —
drive the UI over `adb` instead.

### Emulator

Use **`look35`**: `system-images;android-35;google_apis;x86_64`,
1080×2340, 420 dpi, `hw.gpu.enabled=yes`, `hw.gpu.mode=host`, RAM 4096.
Config: `D:\Android\avd\look35.avd\config.ini`. Created during this run;
the emulator package had to be updated 32.1.13 → 36.6.11 first, because the
old one cannot boot API 35.

```
$env:ANDROID_AVD_HOME="D:\Android\avd"
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd look35 -no-audio -no-boot-anim -gpu host
```

If it will not start: delete `*.lock` and the `snapshots` folder inside
`D:\Android\avd\look35.avd\`. Recreating the AVD is **not** needed. Shut
down with `adb emu kill` first; only escalate to `Stop-Process` if it
survives 30 s.

**Do not use `aosp_atd` or headless images to judge appearance.** They are
stripped. Also: this emulator is AOSP, not One UI — system font, switch
shape and wallpaper behaviour differ from the owner's S24, so "how it looks
in One UI" is something only the owner can confirm.

### Screenshots on the emulator

- **The Soul screen and capsules screenshot as pure black.** That is
  `FLAG_SECURE`, not a render bug — `SoulScreen.kt:69-70`,
  `SecureWhile(!screenshotsAllowed)`. Turn on Settings → «Скриншоты экрана
  души» first.
- `exec-out screencap` **races the staggered entrance**. A blank or
  half-drawn Home is usually a capture taken while `enterStaggered` was
  still animating alpha. Wait, or re-take, and confirm with
  `uiautomator dump` before believing a screenshot is empty.
- To reproduce the defect class that started this run (per-letter text
  wrapping), you need `adb shell settings put system font_scale 1.3`
  **or** `adb shell wm density 480`. At 420 dpi with font 1.0 the Home
  header looks fine. Reset with `wm density reset`.

### Test-instrument limits you will otherwise trust wrongly

- **`RigGoldenTest` cannot measure size.** It captures a Compose `Image`
  node inside a Robolectric window, so all 32 goldens measure the same box
  regardless of content (measured: solid-alpha bbox is exactly 62.5 % for
  all eight concepts). Use `FrameScaleTest` / `ContactSheetDumpTest`, which
  render real `StillRender.tile` output.
- **`FrameScaleTest` measures `Mood.ALERT` only.** That is why `EMBER`'s
  undersized `asleep` slipped through.
- **`GlowSkinFalloffTest` knows about the rect, not the frame.** It stayed
  green while the glow overflowed the frame and the frame edge cropped it
  into the same hard rectangle the rect used to — i.e. it stayed green
  through a visual regression of the very defect it exists for. The contact
  sheet caught that, not the test.
- `RigGoldenTest` is one `@Test` with a loop inside, so Roborazzi stops at
  the first mismatched image. A "1 compare file" result does **not** mean
  the diff is small — measure with `git status` after re-recording.

### Gradle on this machine

- **Never run two Gradle invocations against this repo at once.** It
  produces "file is being used by another process" and spurious task
  failures. One such failure (`:app:lintAnalyzeDebugAndroidTest`) was
  transient and passed on a serial re-run.
- `gradle.properties` already sets `-Xmx6g`; `lintVital` needs it.
- Prefer the Bash tool for `gradlew`. In PowerShell the wrapper works too,
  but heredocs and quoting for commit messages are much easier in Bash.
- Git on this machine converts LF→CRLF; every commit prints warnings. They
  are noise.

### Secrets

Signing material lives in `../anima-keys/` — **outside** the repository
(`anima-upload.keystore`, `keystore.properties`). `.gitignore` carries a
second fence (`*.keystore`, `*.jks`, `keystore.properties`,
`local.properties`). Release builds are deliberately **unsigned** when the
properties file is absent (`docs/release/signing.md`). A full-history audit
was run before publication: 1090 blobs across 69 commits scanned for 12
credential patterns plus a Shannon-entropy sweep; zero findings. If you add
anything key-shaped, keep it out of the tree.

---

## д) Invariants you must not break

Product:

1. **Network belongs to exactly two modules** — `core:model-delivery` and
   `core:cloud-mind`. `INTERNET` may be declared nowhere else.
   `NetworkIsolationTest` fails the build otherwise, and it also pins the
   merged permission set as an exact allowlist.
2. **Zero analytics, zero crash reporting.** The `tools:node="remove"`
   directives in `app/src/main/AndroidManifest.xml` exist to strip Play
   telemetry if a dependency reintroduces it. They now carry
   `tools:ignore="MissingClass"` — do not delete them because the class is
   absent; absent is the desired state.
3. **Animation only on a visible screen; wallpaper 0 fps at rest.**
   `WallpaperBudget` is stricter than Google's docs. Screen transitions are
   fine because they are finite; do not introduce a standing frame loop.
4. **Untrusted text is never a command.**
5. **Chat and `:mind-pack` are demoted, never deleted.** Chat is a route
   reachable only from Settings behind `experimental_chat`, default OFF
   (`core/data/.../AnimaPrefs.kt`, default pinned by
   `AnimaPrefsDefaultsTest`). `:mind-pack` stays in
   `settings.gradle.kts` but must not return to `assetPacks`.
   `ChatDemotionTest` holds all of this.
6. **Contracts evolve additively.** Concrete precedent from this run:
   ambient breathing squash was published as a new `pose.breathSquash`
   channel rather than folded into `pose.squash`, because `pose.squash`
   means "the event episode" and `CreatureEngineTest` asserts it settles to
   zero. Folding them broke that test — which was the signal they are two
   things, not a reason to edit the test.
7. **The body is assigned, never chosen.** No catalogue of the eight
   concepts anywhere in the app. Palette variants of the *assigned* body
   are allowed.

Process:

8. **Do not push, merge or open a PR without the owner asking.** Pushing
   the seven v1.1 commits was explicitly authorised and is done; that
   authorisation does not extend to anything further, and explicitly
   excluded merge and release.
9. **A commit per finished stage; a clean tree at handoff.**
10. **Every claim needs an artefact** — a path, a log, a screenshot, or
    command output. "Implemented" without one counts as not done. Take test
    counts from run output, never from memory or from a previous doc.
11. **Look at screenshots with your own eyes before reporting.** Two real
    bugs in this run were found only that way (content sliding under the
    status bar; a swatch invisible against the background), and one visual
    regression that all tests passed through (the glow clipped by the frame).
12. **Do not decide product questions.** When you hit a "how should it be"
    fork, ask and wait. Disagreeing with an instruction on the merits, with
    reasoning, is welcome — the owner accepted one such push-back this run
    (leaving the 44 sp `hero` step unused on Home, because the hero of that
    screen is the body).

---

## е) Left unverified, and why

- **Everything about One UI.** The emulator is AOSP. System font, switch
  shape, wallpaper behaviour, and Samsung's app-sleep timings (3 / 16 days,
  `docs/design-2026-07.md` §1.2) are unverified on the actual S24.
- **That the creature looks alive.** A screenshot is one frame. Breathing,
  blinking and the new microsaccades are asserted at the engine level
  (`MicroMotionTest`) but nobody has watched them on a device.
- **D5 on Home.** The rectangle-around-the-creature fix is tested and was
  eventually seen on `SPIRIT_ORB` in the transient launch frame, but it was
  never deliberately photographed on Home, because this emulator's body is
  `FOX_KIT`, which does not use `GlowSkin`, and the body-picking surface is
  being removed by owner decision.
- **The widget and the live wallpaper were never placed on a home screen**
  during this run. `StillRender` is what they draw with and it changed
  substantially in `265688e` and `832bde2`. Both surfaces are unexercised.
- **`DayInLifeTest` / the whole E2E floor.** Still hanging, still unfixed.
- **Contact-sheet backdrops are synthetic.** The "busy photo" is a
  generated blob field, not a photograph. It is a deliberately hard case,
  but it is not a real wallpaper.
- **The 5.2 calibration was tuned on one seed** (`909_090L` in
  `FrameScaleTest`). `CreatureGenome.sizeScale` varies 0.92–1.08 per
  device, so real phones sit slightly either side of the measured band. The
  test's band is wide enough to absorb that, but it was not swept.
- **Nothing was checked on a physical device by this session at all.**
