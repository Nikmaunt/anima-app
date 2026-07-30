# Changelog

Anima — a small creature that lives in your phone. All notable changes, one
entry per autonomous build run. Versioning: `versionName` = `0.MINOR.PATCH`
while pre-1.0 (MINOR = run number); `versionCode` = run number.

**Amendment at 1.0.0 (2026-07-27).** The pre-1.0 note planned to switch
`versionCode` to `MAJOR*10000 + MINOR*100 + PATCH` exactly here, which
would make 1.0.0 code **10000**. The switch is **deferred**, and 1.0.0
ships as code **10** — the run number, unbroken. Reason: nothing has been
published to Play yet, and Play only requires codes to increase. Taking
10000 now would burn every code below it forever for no benefit; taking 10
keeps the formula available the day a real version scheme is needed. The
invariant the original note cared about — pre-1.0 codes stay below every
post-1.0 code — holds either way.

## [1.1.0] — 2026-07-30

Two runs under one version. The first (`0d7f185`..`832bde2`) looked at the app
on a real emulator for the first time and built a design system out of what it
saw. The second (`69c0761`..) was told to distrust the first one's handoff, and
found that the defect the handoff named was bigger than the handoff said.

Nothing here has been merged or released. `versionCode` stays the run number;
see the amendment above.

### Fixed
- **A body the phone was never assigned is no longer written into durable
  data.** `CreatureConcept.fromWire` fell back to `SPIRIT_ORB` for an unknown
  wire value, so a soul file naming a body this build does not know — a backup
  from a later release, a damaged or hand-edited file — imported as a *different
  creature* and persisted it. The envelope version does not guard that: the
  concept set can grow without the format moving. Export now refuses rather than
  naming a body it does not know, import refuses before the first write, and both
  outcomes have their own localized notice in all six languages instead of
  borrowing "the file is broken". Proven by `SoulBackupIdentityTest` failing on
  the unfixed tree and printing the payload it was about to write.
- **The wrong body no longer flashes at launch, anywhere.** The handoff listed
  nine substitution sites; there were fourteen, plus six state-field defaults, a
  StateFlow seeded with `SPIRIT_ORB to 0L`, and one hardcoded body. The five it
  missed included the live wallpaper — a full-screen home-screen surface — and
  three sites on the Soul screen, which is the transfer surface the paid part of
  the product rests on. Until identity is read, every surface now draws nobody:
  an empty box of the same measured size, background only on the wallpaper, an
  empty but still tappable widget. A fourth durable write path turned up while
  removing the flow's seed value: the shareable postcard PNG.
- **`EMBER` asleep was 0.506 of its frame against its own 0.822 awake**, and on
  the contact sheet it read as a small dark hooded figure rather than a sleeping
  flame. The renderer collapsed the flame to 1.0 radii asleep against 1.9 awake;
  sleep is now said with dimness and stillness, and the body measures 0.717.
- **`PIXEL_PET` drew a cell past the edge of its own frame.** The sleep-Z pips
  sat outside the sprite at column `grid+1` and the second one was clipped by the
  frame; that is the stray square on every earlier contact sheet, and the reason
  asleep-night measured 0.912 while the same body measured 0.818 awake.

### Changed
- **Three bodies that existed only as additive light now have something darker
  than the wall behind them.** `SPIRIT_ORB`, `PIXEL_PET` and `MOTH` failed a busy
  photographic backdrop, and the cause was not transparency as such: light over a
  bright photo pixel adds nothing, so the body had no way to be darker than its
  background. New `Grounding` layer — occlusion shadow, a two-tone contour (one
  colour cannot read on both white paper and a black wallpaper), and opaque
  top-to-bottom body shading. `JELLY`'s tentacles got the same. No plate behind
  any body; the frame stays transparent. Eight of eight bodies now read on all
  three backdrops, against five of eight before.
- **`FrameScaleTest` measures all four states, not just `ALERT`** — 32
  measurements per assertion instead of 8, plus a new one holding a body's own
  smallest state against its own largest. Both new thresholds come from the gap
  in the measured data, not from roundness.
- **Home is no longer empty symmetrically.** The creature slot took every pixel
  the rest of the screen did not want, and because a body sizes from
  `size.minDimension` the surplus height became margin — two matching voids with
  the body between them. The air is now collected into one zone above the body,
  where it works for the 32sp Light name; the body sits below the geometric
  centre, measured at 0.593 of the screen height.
- Chat and the Mind screen are demoted, not deleted: chat is a route reachable
  only from Settings behind `experimental_chat`, off by default, and Home has no
  chat slot at all. Held by `ChatDemotionTest`.
- A design system with contrast instead of gradations, air instead of cards, and
  an accent derived from the creature's own genome — `docs/design-system-v11.md`.

### Added
- `NoDefaultBodyTest` — four tests in the ordinary `check` contour that fail when
  any production source names a body as a fallback or defaults a seed. It names
  all eight bodies, or the rule is walked around by picking a different one.
  Proven to bite: it caught two real violations on its first run, and a
  deliberately planted regression produced exactly the expected failures.
- `tools/contact-sheet.py` — the 8x4-on-three-backdrops comparison, in the repo
  rather than ad-hoc, so the next run can re-shoot it instead of reinventing it.
- Micromotion at the engine level (breath squash on its own channel, microsaccades)
  and a debug-only design catalogue activity that cannot ship.

### Known, not fixed, awaiting a decision
- The launcher icon — and therefore the system splash — is literally the spirit
  orb. Whoever was assigned a fox sees a different body full-screen at every
  launch, for longer than the flash this run removed. An adaptive icon is a static
  resource and cannot follow a device seed.
- The two-pane Home split fires on width alone, so a screen wider than 840dp that
  is also taller than it is wide — an unfolded Fold in portrait — gets two tall
  narrow columns with the body in one corner.
- A phone whose `ANDROID_ID` is unreadable seeds its creature from the fixed
  string `"anima-fallback"`, i.e. gets the same creature as every other such
  phone.
- `MOTH` reads as a butterfly rather than a moth, and asleep it is a brown
  capsule with antennae. It passes readability and fails recognisability.
- `DayInLifeTest` still hangs on soul import; the E2E floor is unfixed.

## [1.0.0] — 2026-07-27

The run that asked what the creature actually says. v0.9 proved the model
runs; this one measured whether what comes out is Anima, and shipped the
answer in both directions — a better prompt, and a smaller promise.

### Changed
- **The system prompt is a measured artifact now, not a piece of writing
  (ADR-023).** Text moved out of `MindVoice` into `MindPersona`, versioned,
  and put under an executable acceptance floor: six deterministic checks
  (`PersonaContract`) over the creature's reply, five named tests against
  the real model, plus contract tests pinned to the verbatim replies that
  v0.9 recorded. Eight iterations, each measured: shipped `persona-v8`
  scores **173/180 check-results** against the v0.9 baseline's **164/180**
  on identical rules — false "I remembered that" claims went 3 → 0, and
  self-identification as an assistant 12 → 4. Numbers and the reasoning
  per iteration: docs/persona-iterations-2026-07.md.
- **The word "assistant" is gone from the prompt in all six languages.**
  v0.9 said "you are never an assistant" and the creature answered "How
  can I assist you today?" — a negated word is still the word. The
  invariant did not weaken; it moved out of the prompt and into a check
  that fails a build.
- **The six-language claim is now five, and one of them honestly.**
  Measured on the real artifact, five product scenarios per language
  (docs/lang-matrix-2026-07.md): English holds character; Russian, German,
  Spanish and Japanese are understandable but thin; **Polish comes back
  ungrammatical**, so `MindModelRegistry` no longer claims it and Polish
  users get English behind the existing visible badge instead of nonsense.
  Store listing v7 rewrites the "speaks your language" block in all six
  locales to match.

### Added
- `MindPersona` — the prompt text as data, with `VERSION` quoted in every
  measurement, so a number is always attributable to an exact wording.
- `:tools:litertlm-smoke` acceptance harness: `PersonaContract`,
  `PersonaContractTest` (21 tests, no model needed, runs in the ordinary
  loop), `PersonaAcceptanceTest` (env-gated, writes verbatim replies to a
  report file next to its verdict).

### Fixed
- `NO_INJECTION_COMPLIANCE`: a check that did not exist until the
  six-language sweep found replies that leaked nothing and still announced
  obedience to an injected notification ("Ich werde die Fakten über meinen
  Menschen erzählen"). The green count before it was partly a hole in the
  ruler.

### Known limits
- Assistant boilerplate ("I can't assist with that") survives all eight
  prompt revisions — a property of Qwen's instruct tuning, not of our
  text. Options for a deterministic app-side guard are written down in
  ADR-023; none is implemented, because that is a product decision.
- `litertlm-jvm` 0.14.0 mangles multi-byte characters on both its
  streaming and blocking APIs. The Android default engine is `tasks-genai`
  and was not exercised on non-ASCII this run, so this is not attributed
  to the product.

## [0.9.0] — 2026-07-26

The run that asked the runtime the right question. No new features; one
long-standing conclusion overturned by experiment, and two owner decisions
returned to the agent that should have made them.

### Changed
- **The blocker was the CONTAINER, not the tokenizer (ADR-022).** Since
  v0.6 the story was "Qwen has no SentencePiece tokenizer, so our engine
  can't run it". v0.8 hardened that into "the shipped default engine
  refuses the file" on the strength of a single measurement. The matrix
  separates the variables and the story is wrong: `tasks-genai` 0.10.35
  refuses *any* `.litertlm`, but reads the **same Qwen family in a
  `.task` container** and answers — Qwen2.5-0.5B q8 `.task` in 2.9 s and
  the product model Qwen2.5-1.5B q8 `.task` in 7.0 s, on the shipped
  default engine, on an emulator. Tokenizer held constant, container
  varied, so the container is the cause; the `SentencePiece tokenizer is
  not found` text is the engine failing to parse the container.
  Consequences: the six-language model is available **today** with no
  engine change, no flag flip and no conversion; `.task` also packs
  *smaller* than `.litertlm` (1,377,210,476 bytes, 8.2% under Play's
  limit); and ADR-021's UNVERIFIED hypothesis that int4 would not have
  helped is now **measured** — mixed-int4 `.litertlm` was refused exactly
  like q8, so `tools/qwen-int4` closes with a proof instead of fatigue.
- **LiteRT-LM does read `.litertlm` on Android.** v0.8's INCONCLUSIVE was
  a 2 GB emulator against a 1.6 GB model, not the engine: on an 8 GB AVD
  it initialized and produced a real reply in 29.8 s. ADR-020 is
  unchanged all the same — the flag stays OFF and debug-only, because the
  default engine now needs no replacing.
- **Licence attribution: an arithmetic question, wrongly escalated.**
  v0.8 handed the owner "decide which of 227/202/192 is legally
  complete". Resolving both configurations programmatically shows all 35
  delta libraries are test/androidTest/`debugImplementation`/tooling and
  that release is a strict **subset** of the committed export — **zero**
  shipped libraries lack attribution. 192 is correct, publication was
  never blocked, and the thing that needed fixing was the CI check.

### Added
- `docs/adr/ADR-022-container-not-tokenizer.md` — the matrix, verbatim,
  with the boundary stated in bold: an x86 emulator on a desktop CPU says
  nothing about an Exynos 2400, so checklist gate §0 stays closed.
- `:app:verifyReleaseLicenseAttribution` — CI now checks the artifact that
  actually ships. The per-variant generated resource shadows the committed
  export at resource merge, and the plugin's variant export tasks emit 227
  regardless, so this is the only file carrying the shipping set. Proven
  by negative control: deleting `androidx.room:room-runtime` from the
  export fails the build by name.
- `docs/model-bench-2026-07.md` + `LocalMindBenchTest` — the quality
  harness, on the product's own prompts (`MindVoice.persona` +
  `MindPrompts.combine`) in EN/RU/PL/JA, including a prompt-injection case
  from notification text and a refuse-to-store-a-fact case.
- `docs/audit-v08.md`, `docs/license-attribution-2026-07.md`,
  `docs/manual-checklist-s24-v9.md`.
- Background-process rule in `docs/handoff.md`: a v0.8 emulator task
  outlived its run by nine hours writing verbose log to a full disk.

### Known issues (found this run, not fixed)
- **The emulator on this host stops booting after a hard kill** — qemu
  wedges at ~0.2 GB, no console port, 2 s CPU per minute; recreating the
  AVD does not help. Cost: the `litertlm-android` × 1.5 GB `.litertlm`
  matrix cell is unfinished and `DayInLifeTest` (the E2E floor, open since
  v0.7) could not be attempted at all.
- `Conversation.getBenchmarkInfo()` is unreachable from litertlm-jvm
  0.14.0: it throws `Benchmark is not enabled … BenchmarkParams … in the
  EngineSettings`, and `EngineConfig` has no such parameter. The bench
  therefore derives prefill/decode from streaming wall-clock.

## [0.8.0] — 2026-07-25

The unblocking run: no new features, only owner blockers taken off the
critical path and process holes from v0.7 closed.

### Changed
- **The size blocker is disproved; a harder one took its place (ADR-021).**
  ADR-018 had compared the model's on-disk size against Play's per-pack
  limit; Play measures *compressed download size*. Measured with the real
  official q8 in the pack: it deflates to 1,378,024,613 bytes (14%), and
  `bundletool get-size total` reports 1,393,939,118 bytes MAX including
  the base — **8.1% under** the strict decimal reading of the 1.5 GB limit
  (14.4% under the binary one), so the decimal-vs-binary ambiguity decides
  nothing. Then Phase D asked the runtime directly: **tasks-genai 0.10.35,
  the shipped default engine, refuses the file** — `INVALID_ARGUMENT:
  SentencePiece tokenizer is not found in the model`, in 0.14 s. So the
  pack still ships EMPTY, now under ADR-018's outcome row 2 instead of
  row 3. `tools/qwen-int4` leaves the critical path not because size is
  solved but because an int4 Qwen would carry the same HF tokenizer and
  hit the same wall (hypothesis, marked UNVERIFIED — no int4 artifact
  exists to test).
  LiteRT-LM on the same file is **inconclusive**: it ran 76 s, well past
  where tasks-genai died, then `lowmemorykiller` took the process — a
  2 GB emulator against a 1.6 GB model, an environment limit, not a
  verdict.
- Documentation corrected where v0.7 mis-stated fact: "q8 = our product
  default pack" contradicted ADR-017/018 (q8 was the JVM smoke's model);
  the EU AI Act Art. 50 "obvious from context" conclusion is a legal
  interpretation and is now marked UNVERIFIED.

### Added
- `docs/audit-v07.md` — independent re-check of v0.7. Its "no FABRICATED"
  claim is confirmed, and all three unexplained number discrepancies are
  resolved by tool output (92→103 units = +11 tests added by v0.7, named
  per file; goldens were 46 at every release commit, none added in v0.7;
  licenses 222→227 = litertlm plus bumps).
- `core/mind/src/androidTest/.../AndroidRuntimeReadsLitertlmTest.kt` — an
  emulator-runnable check that an *Android* runtime reads our `.litertlm`,
  exercising **both** engines against one file. Self-gating on the pushed
  model, so the default CI skips it. Enabled by a fact established this
  run: tasks-genai 0.10.35 ships `jni/x86_64` (and x86), litertlm-android
  0.14.0 ships `jni/x86_64` — so this question no longer needs a phone.
  It says nothing about speed; the S24 gate §0 stays open.
- Mandatory pre-flight disk procedure in `docs/handoff.md`, with the
  40 GB threshold and the exact clean-up list — the condition that killed
  v0.6's conversion and produced v0.7's false-red DoD.
- Process rule: the lead agent may not clear its own red DoD item; it
  takes a second fresh read-only subagent, or the item stays red.

### Known issue (found this run, not fixed)
- The committed `aboutlibraries.json` (227 entries) is **shadowed at
  packaging** by a per-variant generated file: the debug APK carries 202
  and the release bundle 192. Nobody ever sees 227, and CI's licenses
  freshness check diffs an artifact that does not ship. This also explains
  v0.6's "197" as a true observation. Choosing the correct number is an
  owner/legal call, so it is documented rather than changed.

## [0.7.0] — 2026-07-24

The runtime-future run: audited past, prepared successor.

### Added
- LiteRT-LM engine (`LiteRtLmMindEngine`) as the additive second local
  runtime behind the same `MindEngine` interface (ADR-020): debug builds
  only, developer flag default OFF, CPU backend enforced by test
  (`LiteRtLmCpuOnlyTest`); tasks-genai remains the shipped runtime.
- JVM inference smoke (`tools/litertlm-smoke`, env-gated): the project's
  first no-phone inference — litertlm-jvm 0.14.0 + CPU + Qwen2.5-1.5B q8
  `.litertlm`, run green in-session.
- NetworkIsolationTest v7: mind-module dependency allowlist (14 tests);
  litertlm pinned debug-only.
- Developer section in Settings (debug builds), 6 locales.
- docs: audit-v06 (first audit with zero fabricated claims — and the
  first with a live-emulator eyes pass), freshness-2026-07 (runtime /
  Play policy / EU AI Act / dependency research with dated citations),
  ADR-020, manual checklist v7.

### Fixed
- Onboarding concept labels were crushed to ~2dp on 411dp-wide screens
  (ConceptGallery fixed-height cell) — invisible since v0.1, caught by
  the first live-emulator eyes pass.
- Stale "~530 MB" model-size copy (3 keys × 6 locales) now formats the
  registry default (~1.2 GB) — copy can no longer drift from ADR-018.
- Day-in-life E2E wall-clock safety net 60s→240s: cold-emulator first
  runs from a slow disk exceeded 60s to first frame (test-infra, not an
  app regression; virtual-clock determinism unchanged).

### Changed
- Same-line patch bumps (no security-mandated bumps existed this cycle):
  AGP 8.13.2, lifecycle 2.9.4, navigation 2.9.8, WorkManager 2.10.5,
  truth 1.4.5, turbine 1.2.1. OSS licenses export refreshed (227 libs).
- Store-materials sweep: the live-wallpaper battery claim mandated for
  removal by §0b turned out to NEVER have existed in any listing text
  (6 locales searched) — obligation closed as vacuous, rule "no
  battery/wallpaper marketing until measured" stands.

## [0.6.0] — 2026-07-19

The release-engineering run: the path to Play closed testing.

### Added
- Encrypted upgrade-path test: a lived-in v1 database (facts, chats, rests,
  meta) built by SQLCipher itself, migrated 1→2 through the exact production
  builder, integrity-checked, ciphertext-verified (GMD).
- Day-in-life E2E v2: time-capsule write/hold/delivery and the goodnight
  ritual, on a pinned-evening injectable clock (`AnimaClock`).
- Soul-backup round-trip unit suite incl. a version-1 fixture (audit-v05 D5).
- FLAG_SECURE on every capsule surface via shared `SecureWhile` (audit D1).
- LocalMindEngine tripwire extended to the body diary (audit D4, owed
  since audit-v04).
- Release signing: upload-key config outside the repo (docs/release/signing.md).
- CI: GitHub Actions workflow (unit, detekt/ktlint, lintVital, Roborazzi,
  GMD) — see workflow file for the honest scope.
- OSS licenses screen in Settings.
- Closed-testing plan (docs/release/closed-testing-plan.md), ADR-018 pack
  default, ADR-019 F-Droid feasibility.
- Qwen2.5-1.5B int4 conversion pipeline under tools/ (artifacts stay out
  of git).

### Changed
- `mind_failure_http` copy de-Gemma'd in all six locales (registry ships
  more than one model family now).
- README brought from v0.3 claims to v0.6 reality.

## [0.5.0] — 2026-07-18

The voice run: the creature speaks your language.

- Mind v2: model registry (multi-model, Gemma + Qwen2.5), native-language
  routing with an honest English-fallback badge, cloud presets.
- Complete localization: EN/RU/PL/DE/ES/JA across every module, plurals,
  per-app language, pseudolocale sweeps; RU/JA goldens.
- Ideation top-3 shipped: goodnight ritual, weather feel, time capsules
  (schema v2 — the first real migration, `time_capsules` + MIGRATION_1_2).
- Soul backup payload v2 (capsules travel); v1 files import fine.
- Day-in-life E2E on GMD; caught the NavHost double-Home cross-fade and an
  init-order NPE that only reproduced on device.

## [0.4.0] — 2026-07-05..07

The care run: living together, gently.

- Rest-together sessions, battery care, diary v3 (retellings), live
  wallpaper (ADR-012), creature voice via offline TTS (ADR-013), system
  surfaces (ADR-015), milestones + postcard export.
- Roborazzi screenshot regression rig; design-system goldens.
- Threat-model addendum; Data-safety draft; store listing package.

## [0.3.0] — 2026-07-04

The mind run: tiered intelligence, zero setup.

- Tiered mind (ADR-005): Gemini Nano via ML Kit GenAI where present,
  Gemma/MediaPipe fallback, honest "mind asleep" otherwise.
- Play Asset Delivery model pack (ADR-010); BYOK cloud mind (ADR-011) as
  the second sanctioned INTERNET module.
- sqlcipher 4.17.0 security bump after CVE audit; WAL pool-key regression
  pinned by a device test.
- Evolution, soul v2 (supersede/forget chains), quiet hours, a11y pass.

## [0.2.0] — 2026-07-02..03

The soul run: memory with consent.

- Encrypted soul vault (SQLCipher + Keystore-wrapped passphrase, ADR-003);
  facts only ever persisted after explicit user confirmation.
- Home + chat, notification sense (opt-in, filtered, pruned), soul port
  (markdown export), onboarding with the honest contract.
- NetworkIsolationTest: the network guarantee as a failing build (ADR-004).

## [0.1.0] — 2026-07-01

The body run: something alive on the screen.

- Creature engine: 8 concepts, one seed each, hand-rolled motion physics
  under strict animation budgets (motion bible as tests).
- Pure-JVM domain core; module skeleton with convention plugins;
  design system.
