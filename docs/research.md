# Anima — Phase 0 Research

Date: 2026-07-17. Every external claim carries a source link. Claims that could
not be confirmed against a primary source are marked **UNVERIFIED**.

---

## A. Character technology on Android (2025–2026)

### A1. Compose Canvas procedural drawing

- The #1 jank pitfall for continuous animation is reading animated state during
  *composition*; defer reads to the draw phase (`drawBehind`, lambda
  `graphicsLayer`, `Modifier.offset {}`), letting Compose skip composition and
  layout entirely.
  [Compose performance best practices](https://developer.android.com/develop/ui/compose/performance/bestpractices)
- `withFrameNanos` is the primitive under all Compose animation APIs; it gives
  exact Choreographer timestamps and delta-time control.
  [Compose Animation Under The Hood pt.2](https://sagarviradiya.dev/posts/compose-animation-part-02/),
  [Await next frame](https://jorgecastillo.dev/jetpack-compose-await-next-frame)
- Production pattern (RevenueCat animated paywall): one `withFrameNanos` loop,
  delta-time capped (~100 ms) to survive background→foreground jumps, a single
  time value as source of truth, particle state pre-generated in `remember`,
  zero per-frame allocation.
  [RevenueCat: custom paywalls in Compose](https://www.revenuecat.com/blog/engineering/custom-paywalls-compose/)
- `rememberInfiniteTransition` cannot pause and runs as long as it's composed;
  for pausable/stoppable loops use a raw frame loop or `Animatable`.
  [InfiniteTransition API](https://developer.android.com/reference/kotlin/androidx/compose/animation/core/InfiniteTransition)
- Full stop off-screen: gate the loop with `repeatOnLifecycle(RESUMED)` /
  `LifecycleResumeEffect` — recommended by Google for "resources that need to be
  active only when the user is interacting with the app… for example…
  animations". [Lifecycle docs](https://developer.android.com/topic/libraries/architecture/lifecycle)
  The "zombie animation" Choreographer failure class is documented for the View
  era ([androidperformance.com](https://androidperformance.com/en/2019/10/24/Android-Background-Animation/));
  whether modern Compose fully parks the frame clock in background is
  **UNVERIFIED** → we treat lifecycle gating as mandatory.
- Isolating the creature in its own small layer helps twice: only the dirty
  node's display list is re-recorded
  ([hardware acceleration](https://developer.android.com/topic/performance/hardware-accel)),
  and shader cost scales with shaded pixels
  ([using AGSL](https://developer.android.com/develop/ui/views/graphics/agsl/using-agsl)).

### A2. AGSL RuntimeShader (API 33+)

- Stable since Android 13; still invested in (Android 16 added
  `RuntimeColorFilter`/`RuntimeXfermode`).
  [Android 16 Beta 2](https://android-developers.googleblog.com/2025/02/second-beta-android16.html)
- Two Compose paths: `ShaderBrush(RuntimeShader)` inside a draw scope (cheap,
  uniform updates trivial: set → draw), and
  `graphicsLayer.renderEffect = createRuntimeShaderEffect(...)` (post-processes
  the whole layer, "more expensive" per official docs, and the effect object
  must be re-created per frame for uniform changes).
  [Using AGSL in your app](https://developer.android.com/develop/ui/views/graphics/agsl/using-agsl),
  [Metaballs with RuntimeShaders (2025)](https://medium.com/@off.mind.by/metaballs-with-runtimeshaders-bb7e5f6b27c2)
- Metaballs/SDF/noise/glow recipes verified in Compose:
  [shady gallery](https://github.com/drinkthestars/shady) (note: some ports are
  CC BY-NC-SA — do not copy shader source from there),
  [Jellyfish in Compose + AGSL RenderEffect](https://medium.com/androiddevelopers/making-jellyfish-move-in-compose-animating-imagevectors-and-applying-agsl-rendereffects-3666596a8888)
  (Android DevRel; the closest published precedent to Anima's creature).
- First-compile cost of a shader: documented for Skia pipelines generally
  ([Flutter shader jank](https://liudonghua123.github.io/flutter_website/perf/shader/));
  AGSL-specific numbers **UNVERIFIED** → we pre-warm shaders once at startup.
- Quantitative S24 benchmarks for Compose/AGSL: none found — 60fps headroom on
  flagship silicon is directional consensus of 2025 sources, **UNVERIFIED**
  quantitatively → manual S24 checklist includes a 10-minute fps/thermal run.

### A3. Compose spring physics

- `spring()` is interruptible with velocity preservation — ideal for a creature
  that gets poked mid-motion.
  [Customize animations](https://developer.android.com/develop/ui/compose/animation/customize)
- `Animatable`: `snapTo` for 1:1 drag tracking, `animateTo(..., initialVelocity)`
  on release with gesture velocity.
  [Value-based animations](https://developer.android.com/develop/ui/compose/animation/value-based)
- Squash & stretch with elastic spring-back is a documented worked example
  ([Animating Inside and Outside the Box](https://proandroiddev.com/animating-inside-and-outside-the-box-with-jetpack-compose-a56eba1b6af6));
  volume preservation via `scaleX = 1/scaleY` in lambda `graphicsLayer`.
- Chained springs for secondary motion (tentacles, ears): each link an
  `Animatable` targeting its predecessor; engine-agnostic math:
  [Trailing Joints](https://blog.littlepolygon.com/posts/trail/). No packaged
  Compose library exists (**UNVERIFIED** as a package; we implement the chain
  in the pure motion core).

### A4. Rive / Lottie — honest assessment

- Rive's Compose API is beta, "production ready" per Rive, with frequent
  releases and a Vulkan renderer.
  [Rive Android docs](https://rive.app/docs/runtimes/android/android)
  BUT: .riv artboards/rigs are authored in the Rive editor; the runtime
  controls state machines, it does not construct rigs from code
  ([Rive runtimes](https://rive.app/runtimes)). **Fails the "authored in code
  in this session" requirement.** Rejected (ADR-001).
- Lottie: compositions are AE/Bodymovin JSON; dynamic properties only modify,
  never author; no physics/gesture interruption.
  [lottie-compose docs](https://github.com/airbnb/lottie/blob/master/android-compose.md).
  Rejected (ADR-001).
- No open-source Compose virtual pet exists (GitHub sweep) — the niche is empty.

### A5. Frame loop & battery

- One `withFrameNanos` clock for all ambient motion; springs coexist on the
  same frame clock via `Animatable`. Multiple `infiniteTransition`s multiply
  clocks and drift ([RevenueCat](https://www.revenuecat.com/blog/engineering/custom-paywalls-compose/)).
- Continuous 60fps keeps the pipeline awake every 16.6 ms; Android's adaptive
  refresh rate work exists precisely because small animations don't need high
  Hz. [Optimize power](https://developer.android.com/games/optimize/power),
  [Adaptive refresh rate](https://developer.android.com/develop/ui/views/animations/adaptive-refresh-rate)
  → Anima self-throttles: sleep/idle states animate at reduced internal tick
  (frame-skip in our own loop), full stop when not RESUMED. Quantitative
  battery cost of a small 60fps canvas: **UNVERIFIED** (no published numbers)
  → manual S24 checklist covers it.

---

## B. Animacy: what makes a creature feel alive

### B1. Disney's 12 principles → UI creature

Sources: [IxDF on the 12 principles in UI](https://ixdf.org/literature/article/ui-animation-how-to-apply-disney-s-12-principles-of-animation-to-ui-design),
[UX Collective examples](https://uxdesign.cc/disneys-12-principles-of-animation-exemplified-in-ux-design-5cc7e3dc3f75),
[Uxcel guide](https://uxcel.com/blog/12-principles-of-animation-a-guide-to-motion-design-133),
counterpoint [Disney is Dead](https://medium.com/ux-in-motion/ui-animation-principles-disney-is-dead-8bf6c66207f9)
(don't over-animate chrome; the *creature* is the show, the UI stays quiet).

Load-bearing for a creature: **anticipation** (wind-up before hop),
**squash & stretch** (mass and softness), **secondary action** (ears/tentacles
layered on the main move), **follow-through** (belly settles after the body
stops), **slow in/out** on everything idle, **arcs** (organic reads curved).

### B2. Idle-life numbers (adopted into the motion bible)

- Human blink: 15–20/min average, i.e. every 3–4 s; 4–7/min under focus;
  blink lasts ~100–150 ms (full range 100–400 ms).
  [scienceinsights](https://scienceinsights.org/how-often-do-people-blink-the-science-explained/),
  [lens.com](https://www.lens.com/questions-answered/what-is-the-average-time-it-takes-to-blink/)
- Animator convention: randomized 3–4 s mean interval, ~250 ms blink, occasional
  double blink; never sync blink to the loop boundary.
  [IndieDB dynamic blink](https://www.indiedb.com/games/a-little-less-desperation/tutorials/dynamic-character-blink-on-idle)
- Eye darts: ~80–125 ms, ~80% of travel in one step, tiny settle; dart frequency
  communicates mental state (rapid = excitement, sparse = pondering).
  [AnimSchool eye darts](https://blog.animschool.edu/2022/08/31/animating-the-eye-dart/)
- Breathing: calm ~12–16/min (4–5 s cycle), inhale longer than exhale, motion
  starts low and travels up; sleeping slower; excited faster with amplitude
  change (exact excited BPM convention **UNVERIFIED** — we use 20–28/min).
  [Second Life animation wiki](https://wiki.secondlife.com/wiki/Animating_Breathing_and_Other_Subtle_Motion),
  [procedural breathing](https://palospublishing.com/simulating-breathing-and-idle-motion-procedurally/)
- Idle cycles: 2–4 s base cycles; 8–12 s for a big weight shift.
  [MoCap Online idle guide](https://mocaponline.com/blogs/mocap-news/idle-animation-game-dev-guide)
- Gaze: eyes-follow-pointer via atan2 + clamped radius
  ([kirupa](https://www.kirupa.com/codingexercises/eyes_follow_mouse.htm));
  the Yeti login form is the canonical UI gaze-mascot
  ([codepen](https://codepen.io/m3eu/pen/VwYBbwO));
  Duolingo blends gaze/lean/expression live instead of pre-baked clips
  ([Rive blog on Duolingo](https://rive.app/blog/duolingo-s-ai-powered-video-call-brings-lily-to-life)).

### B3. Virtual-pet design lessons

- Tamagotchi pinged only every few hours with a tiny closed set of needs —
  restraint is the etiquette lesson.
  [The Walrus](https://thewalrus.ca/how-tamagotchis-trained-millennials-for-the-era-of-needy-media/)
- Neediness sweet spot: "suffers but recovers" — stakes plus hope; a pet that
  can't be affected is boring, one that dies is harsh.
  [Yu-kai Chou](https://yukaichou.com/advanced-gamification/the-pet-companion-design-in-gamification/)
- Finch: no guilt mechanics, skipping has no penalty — the modern non-dark
  pattern. [Deconstructor of Fun](https://www.deconstructoroffun.com/blog/x0hd2ssr80y5n7gv0w967pg7hwd7tl)
- Nintendogs: location-sensitive petting with visible lean-in made touch feel
  alive. [The Game Hoard](https://thegamehoard.com/2022/05/05/50-years-of-video-games-nintendogs-chihuahua-friends-ds/)
- Uncanny valley: stay stylized; conflicting realism cues trigger it,
  deliberately fantastical designs sidestep it.
  [ACM Interactions](https://interactions.acm.org/archive/view/september-october-2018/avoiding-the-uncanny-valley-in-virtual-character-design)
- **Anima consequence:** the creature never nags (no notifications from Anima
  at all — the app posts zero), never dies, visibly recovers; all bodies are
  abstract/stylized.

### B4. Haptics for petting

- No-permission path: `View.performHapticFeedback` with
  `HapticFeedbackConstants` (CONFIRM, CONTEXT_CLICK, CLOCK_TICK…), respects
  system settings.
  [Haptic feedback guide](https://developer.android.com/develop/ui/views/haptics/haptic-feedback)
- Rich path (normal `VIBRATE` permission): `VibrationEffect.startComposition`
  (API 30+); `PRIMITIVE_LOW_TICK` (API 31+, soft, low-frequency, explicitly
  intended for repetitive dynamic feedback) is the purr building block;
  spacing 30–60 ms reads as texture.
  [Custom haptics](https://developer.android.com/develop/ui/views/haptics/custom-haptic-effects),
  [AOSP primitives](https://source.android.com/docs/core/interaction/haptics/haptics-constants-primitives)
- **Mandatory check:** if any primitive in a composition is unsupported the
  whole composition plays nothing → `arePrimitivesSupported()` first; accurate
  per-primitive reporting starts Android 12.
  [Haptics UX foundation](https://source.android.com/docs/core/interaction/haptics/haptics-ux-foundation)
- Per-constant API levels of some HapticFeedbackConstants: **UNVERIFIED**
  (conflicting fetches) — minSdk 31 makes most of this moot; we gate LOW_TICK
  on `arePrimitivesSupported` at runtime, not on SDK level alone.

---

## C. On-device LLM: ML Kit GenAI / AICore / Gemini Nano (July 2026)

- **Prompt API exists and is Beta**: `com.google.mlkit:genai-prompt`
  **1.0.0-beta3** (2026-07-14). Timeline: alpha 2025-10-29 → beta 2026-01-28 →
  beta2 (model selection) → beta3 (structured output, system instructions,
  4K output). [Release notes](https://developers.google.com/ml-kit/release-notes),
  [Maven index](https://dl.google.com/android/maven2/com/google/mlkit/group-index.xml)
- API shape: `Generation.getClient()`; `checkStatus()` → `FeatureStatus`
  (UNAVAILABLE=0 / DOWNLOADABLE=1 / DOWNLOADING=2 / AVAILABLE=3); `download()`;
  `generateContent()`; `generateContentStream()` (Kotlin Flow); `warmup()`;
  `countTokens()`; params temperature/seed/topK/candidateCount/maxOutputTokens.
  [Get started](https://developers.google.com/ml-kit/genai/prompt/android/get-started),
  [FeatureStatus](https://developers.google.com/android/reference/com/google/mlkit/genai/common/FeatureStatus)
- **Token budget: max input 4 000 tokens (~3 000 English words); output up to
  4K since beta3.** Per-model (nano-v2/v3) differences **UNVERIFIED**.
  [Get started](https://developers.google.com/ml-kit/genai/prompt/android/get-started)
- Errors (`GenAiException.ErrorCode`): BUSY=9 (quota, use `getRetryDelay()`),
  NOT_AVAILABLE=8, NOT_SUPPORTED=16, REQUEST_TOO_LARGE=12,
  PER_APP_BATTERY_USE_QUOTA_EXCEEDED=27, **BACKGROUND_USE_BLOCKED=30**
  (inference is foreground-only — which matches Anima's constraint #5 by
  construction), AICORE_INCOMPATIBLE=-101, NOT_ENOUGH_DISK_SPACE=501…
  [ErrorCode reference](https://developers.google.com/android/reference/com/google/mlkit/genai/common/GenAiException.ErrorCode)
- Numeric quota limits are not published — **UNVERIFIED** beyond existence of
  per-app inference quotas. [ML Kit GenAI overview](https://developers.google.com/ml-kit/genai)
- Structured output: `genai-schema` 1.0.0-alpha1 with `@Generable`/`@Guide` +
  KSP 2.3.6+ — our toolchain pins KSP 2.2.0, so Anima uses prompt-disciplined
  JSON + tolerant parsing instead (extraction candidates are user-confirmed
  anyway). [Structured output](https://developers.google.com/ml-kit/genai/prompt/android/structured-output)
- **Galaxy S24 is NOT in the supported-device list** (S25/S26 series, Z Fold7,
  Z TriFold for Samsung; Prompt API even narrower). S24 ships Gemini Nano for
  Samsung's own features only. On S24 `checkStatus()` returns UNAVAILABLE.
  [Supported devices](https://developers.google.com/ml-kit/genai)
- Fallback assessed and rejected: MediaPipe LLM Inference
  (`tasks-genai:0.10.27`) is in maintenance mode, migration advised to
  LiteRT-LM; Gemma 3 1B ≈ 555 MB — bundling is unshippable, runtime download
  violates the no-network constraint.
  [MediaPipe LLM inference](https://developers.google.com/edge/mediapipe/solutions/genai/llm_inference/android),
  [Gemma3-1B-IT LiteRT](https://huggingface.co/litert-community/Gemma3-1B-IT)
- **Anima consequence (ADR-002):** MindEngine behind an interface; Nano backend
  activates only where `checkStatus() != UNAVAILABLE`; on S24 the app runs in
  the honest "mind asleep" mode that the product already requires for offline
  degradation. The manual S24 checklist documents the expected feature-check
  result as UNAVAILABLE.

---

## D. Body signals without permission prompts

All of these need **zero runtime permissions**; two need normal install-time
manifest entries (`ACCESS_NETWORK_STATE`, `VIBRATE`) that never show a dialog.

| Sense | API | Notes |
|---|---|---|
| Energy / eating | `BatteryManager`: sticky `ACTION_BATTERY_CHANGED` (level/scale/status/plugged), `getIntProperty(BATTERY_PROPERTY_CAPACITY)`, `isCharging()` | Don't poll; register in-process only (manifest receivers dead since 8.0). [Battery monitoring](https://developer.android.com/training/monitoring-device-state/battery-monitoring) |
| Cluttered burrow | `StatFs(filesDir.path)`: `availableBytes`/`totalBytes` (API 18+) | [StatFs](https://developer.android.com/reference/android/os/StatFs.html) |
| Hearing the world | `ConnectivityManager.registerDefaultNetworkCallback`; `NetworkCapabilities.hasTransport(WIFI/CELLULAR)` | Needs only normal `ACCESS_NETWORK_STATE`. SSID is location-gated — we never touch it. [Connectivity status](https://developer.android.com/training/monitoring-device-state/connectivity-status-type) |
| Fever | `PowerManager.getCurrentThermalStatus()` + `addThermalStatusListener` (API 29+), statuses NONE..SHUTDOWN; `getThermalHeadroom` (API 30+, ≤1 call/10 s) | No permission. Some devices report NONE under throttling → treat as advisory; Samsung flagship support **UNVERIFIED** → degrade to "no thermal sense". [Thermal API](https://developer.android.com/games/optimize/adpf/thermal) |
| Day/night | Local clock bands + `ACTION_TIME_TICK` (context-registered, 1/min, no permission); `uiMode` night flag as a hint | No permissionless sunrise API exists; clock heuristic is the correct call. [UiModeManager](https://developer.android.com/reference/android/app/UiModeManager) |
| Feeling squeezed | `ActivityManager.getMemoryInfo`: `availMem`, `totalMem`, `lowMemory` | No permission. [MemoryInfo](https://developer.android.com/reference/android/app/ActivityManager.MemoryInfo) |
| Awake time | `SystemClock.elapsedRealtime()` (incl. sleep) vs `uptimeMillis()` (awake only); difference = naps | [SystemClock](https://developer.android.com/reference/android/os/SystemClock) |

---

## E. Donor patterns (read-only extraction; re-implemented, never copied)

### From hermes-app (D:\Hermes\hermes-app)

- Module discipline: `build-logic` included build with convention plugins by id;
  pure-JVM `:core:model`; `FAIL_ON_PROJECT_REPOS`; single version catalog.
- Toolchain pins proven on this machine: AGP 8.13.0, Kotlin 2.2.0,
  KSP 2.2.0-2.0.2, Gradle 8.14.3, compileSdk 36 / minSdk 31, JDK 21 → 17
  bytecode, Compose BOM 2025.06.01, Room 2.7.2, Hilt 2.57.
- Schema conventions: string PKs `prefix-hex`, epoch-millis UTC, enums as wire
  strings with safe fallback on read, append-only + supersede for memory facts
  (no `@Delete` in the DAO type), exportSchema + committed schema JSON.
- Privacy posture: `allowBackup=false` + `dataExtractionRules` excluding all
  domains for cloud-backup AND device-transfer (API 31+).
- Test stack: JUnit4 + Truth + Turbine + coroutines-test; Robolectric with
  `sdk=34` + `sqliteMode=NATIVE` for DAO tests; deterministic fakes that read
  no clock and no randomness.
- **Encrypted SQLite is NOT implemented in hermes-app** (verified by grep:
  no SQLCipher/SupportFactory/Keystore code; DB is plain Room + backup
  exclusion). Anima implements encryption fresh: SQLCipher for Android +
  Keystore-wrapped passphrase (ADR-003).

### From hermes-lens (D:\Hermes\hermes-lens)

- Animation lifecycle: finite episodes that run to equilibrium and cancel the
  frame loop entirely (`settled` flag, `running=false`); no repeating timers
  (test-enforced "no setInterval" rule); wall-clock fixed-substep integration
  (16.7 ms substeps, max 4) so throttled frame rates keep the same trajectory;
  `lastT = null` on resume to avoid dt spikes; pure engine / impure driver
  split (physics files import no DOM/timers); seeded PRNG (FNV-1a + mulberry32)
  for deterministic composition; reduced-motion → compute the settled state
  synchronously and draw one static frame, never start the loop.
- NotificationListener: service `exported=false` +
  `BIND_NOTIFICATION_LISTENER_SERVICE`; whole handler wrapped in
  swallow-everything (a listener exception kills the process; content is never
  logged); filter chain BEFORE any write: enabled gate → allowlist (exact
  package match) → drop group summaries → drop ongoing → OTP gate; OTP filter
  is pure/JVM-testable: keyword regex
  (`code|код|kod|otp|2fa|verif|подтверж|pin|cvv|cvc|пароль|password|hasło`)
  AND isolated 4–8 digit run (`(?<![0-9])[0-9]{4,8}(?![0-9])`) → drop whole
  notification, even from allowlisted apps; caps (title ≤200 chars, body
  bytes-capped walking code points); stable FNV-1a content ids for dedup;
  grant flow = explainer screen first, then
  `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS` intent + enabled check via
  `NotificationManagerCompat.getEnabledListenerPackages`.

---

## F. Design-skill principles applied

The environment ships `impeccable`/`design-taste-frontend` skills (web-scoped);
their transferable principles adopted for Anima's design system: intentional
palette (not default Material seed), a type scale with a display face for the
creature's voice, motion as meaning (state changes read in the body, chrome
stays quiet), dark theme as the primary theme (a creature that lives in the
phone lives in the dark), generous negative space — the creature is the centre
of every screen.
