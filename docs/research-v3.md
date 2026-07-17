# Research v3 — verified 2026-07-17

Same contract as research-v2: every claim carries its source; anything not
confirmed against a primary source is marked **UNVERIFIED**. Feeds ADR-010
(zero-setup mind delivery) and the Phase-0 build-engineering fixes.
Product/design research lives separately in docs/product-research.md.

## A. Zero-setup model delivery (ADR-010 inputs)

### A.1 Play Asset Delivery size limits — a 700 MB model fits

- Per-pack limit **1.5 GB** (compressed download size); install-time packs
  cumulative with all modules **4 GB**; on-demand+fast-follow cumulative
  30 GB; app total 34 GB.
  https://support.google.com/googleplay/android-developer/answer/9859372
- >200 MB on mobile data → non-blocking "large app" user dialog.
- Install-time packs are delivered as split APKs: install requires ~2× the
  pack size in free space during install/update; fast-follow/on-demand need
  only "a few hundred extra MBs".
  https://developer.android.com/guide/playcore/asset-delivery
- Gemma3-1B-IT dynamic_int4 `.task` = **657 MB** (litert-community model
  card, Google's org). https://huggingface.co/litert-community/Gemma3-1B-IT

### A.2 PAD behavior on sideload / debug

- `bundletool build-apks --local-testing` + `install-apks`: install-time
  packs install normally; fast-follow degrades to on-demand (served from
  external storage, no code change); updates unsupported (uninstall first).
  https://developer.android.com/guide/playcore/asset-delivery/test
- Internal app sharing = production-identical behavior.
  https://support.google.com/googleplay/android-developer/answer/9303479
- Plain `assembleDebug` APK deploys carry **no packs at all** (packs exist
  only through the bundle pipeline; Android Studio needs Deploy = "APK from
  app bundle"). → The app MUST keep a no-pack fallback chain; dev workflow
  stays adb/SAF. (Explicit doc statement of the plain-APK case: UNVERIFIED —
  inferred from the bundle pipeline design.)

### A.3 Gemma Terms of Use — redistribution inside the app is allowed

- Bundling weights = "Distribution" and is permitted, commercial use
  included, under obligations (§3.1): pass the use restrictions through as an
  enforceable provision to recipients; provide recipients a copy of the
  Terms; prominent notices on modified files; ship a notice file with the
  exact text: "Gemma is provided under and subject to the Gemma Terms of Use
  found at ai.google.dev/gemma/terms". https://ai.google.dev/gemma/terms
- Prohibited Use Policy incorporated by reference.
  https://ai.google.dev/gemma/prohibited_use_policy
- Outputs: "Google claims no rights in Outputs" (§3.3).
- MediaPipe LLM docs still say "host the model on a server… too large to be
  bundled in an APK" — written about the base APK, pre-dates PAD sizes; no
  official MediaPipe↔PAD recipe exists (UNVERIFIED that Google recommends a
  specific Play channel for MediaPipe models).
  https://developers.google.com/edge/mediapipe/solutions/genai/llm_inference/android

### A.4 Play for On-device AI ("AI packs") — beta, mid-2026

- Google Play's dedicated model-delivery channel: AI packs (models only),
  install-time/fast-follow/on-demand, same 1.5 GB / 4 GB limits, plus
  **device targeting by SoC, device model, min/max RAM** — the official tool
  for RAM-gating a model at distribution level. Beta; AGP 8.8+ (8.10+ for
  device targeting). https://developer.android.com/google/play/on-device-ai
- Restriction: AI-pack models "should only be used by your apps".

### A.5 Nano / AICore status — S24 still excluded

- ML Kit GenAI device lists (updated 2026-07-15): Prompt API = nano-v2 OEM
  set + nano-v3 (Pixel 9/10, Galaxy S26, OnePlus 15…). **No Galaxy S24
  variant on any list.** research-v2 §A.1 conclusion stands.
  https://developers.google.com/ml-kit/genai
- Prompt API not supported on unlocked-bootloader devices.
  https://developers.google.com/ml-kit/genai/prompt/android/get-started

### A.6 RAM reality for Gemma 3 1B int4

- Official S24-Ultra benchmarks: CPU int4 ≈ 982 MB RAM @1280 ctx, 1145 MB
  @4096 ctx; GPU int4 ≈ 1205 MB CPU + 529 MB GPU.
  https://huggingface.co/litert-community/Gemma3-1B-IT
- Google blog: "at least 4GB of RAM" recommended for Gemma 3 1B on mobile.
  https://developers.googleblog.com/en/gemma-3-on-mobile-and-web-with-google-ai-edge/
- No official minimum-spec table for MediaPipe LLM inference (UNVERIFIED
  beyond the blog); 4 GB devices sit near LMK territory at ~1–1.2 GB peak →
  in-app RAM gate required (ADR-010 §device matrix).
- MediaPipe LLM Inference "does not reliably support device emulators"
  (same doc as A.3) → GMD instrumented tests must not exercise real
  inference; fakes stay.

### A.7 Precedents

- Google AI Edge Gallery: Play-distributed, models downloaded at runtime
  from HF (auth for gated Gemma).
  https://play.google.com/store/apps/details?id=com.google.ai.edge.gallery
- PocketPal AI, MLC Chat: runtime GGUF/weight downloads. No prominent app
  found that bundles ~700 MB weights in the AAB (UNVERIFIED negative) —
  Anima would be early, but inside documented Play limits.

## B. Cloud mind (BYOK) — API surface

- OpenAI-compatible `POST {base}/v1/chat/completions` with `stream: true`
  SSE lines (`data: {...}`, terminator `data: [DONE]`) remains the de-facto
  multi-vendor standard (OpenAI/compatible: Groq, Mistral, OpenRouter,
  Together, local LM Studio/Ollama endpoints). Implemented over
  HttpsURLConnection — no HTTP client library enters the app (the network
  stack ban in the version catalog stays intact).
  UNVERIFIED against a live endpoint from this machine (no key in this
  session; format asserted from API docs knowledge — the manual S24
  checklist v3 carries the live check).

## C. Build engineering (Phase 0)

### C.1 lintVitalAnalyzeRelease × MediaPipe AAR

- v0.2 observed 15–40 min hangs of `lintVitalAnalyzeRelease` "scanning the
  MediaPipe tasks-genai AAR" at `org.gradle.jvmargs=-Xmx3g` and disabled
  `checkReleaseBuilds` globally.
- v0.3 experiment on this machine: fresh daemon, `-Xmx6g`,
  `:app:lintVitalRelease` → **BUILD SUCCESSFUL in 43 s** (27 tasks executed
  incl. `:app:lintVitalAnalyzeRelease`, 0 issues). The hang does not
  reproduce with heap headroom; `checkReleaseBuilds = false` removed, no
  baseline needed. Root-cause attribution (GC thrash at 3g looking like a
  deadlock) is the best-fit explanation, not a proven upstream bug
  (UNVERIFIED as a known public issue).

### C.2 Gradle Managed Devices

- AGP 8.13 DSL: `testOptions.managedDevices.localDevices` (the old `devices`
  container is deprecated — nowinandroid PR #1861).
  https://developer.android.com/studio/test/managed-devices
- ATD images verified available via local `sdkmanager --list` (2026-07-17):
  `aosp_atd` x86_64 for API 30–35 (the doc's "only API 30" sentence is
  stale; **no API 36 ATD yet**). Chosen: `system-images;android-34;aosp_atd;x86_64`.
- Windows: WHPX is the supported hypervisor (AEHD sunsets 2026-12-31);
  GMD AVDs live in `~/.android/avd/gradle-managed`; wipe via
  `cleanManagedDevices`. Known Windows flakiness class: issuetracker
  249111286 / 216744701 / 287312019 — mitigations: swiftshader GPU,
  maxConcurrentDevices=1, setupTimeoutMinutes up, clean+retry.
- Headless flags (verified against AGP option sources):
  `android.testoptions.manageddevices.emulator.gpu=swiftshader_indirect`,
  `android.experimental.testOptions.managedDevices.maxConcurrentDevices=1`,
  `android.experimental.testOptions.managedDevices.setupTimeoutMinutes=30`,
  `android.builder.sdkDownload=true` (licenses must be pre-accepted once).
- ATD strips Google apps/SystemUI/launcher & disables hardware rendering:
  SQLCipher/Room/Compose-interaction tests fine (swiftshader); hardware-
  rendering screenshot tests unsupported; real widget *hosting* and
  NotificationListenerService instrumentation UNVERIFIED on ATD — those
  stay at the AppWidgetManager/filter level or on a full `aosp` image.
- MediaPipe LLM inference explicitly unreliable on emulators (§A.6) — the
  instrumented suite never loads a real model.

### C.3 Dependency CVE audit (2026-07-17)

Method: OSV.dev API per exact coordinate, GitHub Advisory API (validated
against a log4j positive control), NVD 2.0 keyword queries, vendor
changelogs. Full table in the session report; verdicts:

- **sqlcipher-android 4.6.1 — the one actionable item.** No direct CVE, but
  it bundles SQLite 3.46.1, inside the affected ranges of CVE-2025-29087
  (fixed SQLite 3.49.1) and CVE-2025-6965 (fixed 3.50.2); pre-4.15 also
  lacks `sqlcipher_export` source-name sanitization (Deutsche Telekom
  report, low severity). Both need attacker-influenced SQL — low practical
  risk here (all SQL is Room-generated) but real fix versions exist.
  → **Bumped to 4.17.0** (SQLite 3.53.3; Maven Central latest 2026-07-08);
  open/key path re-verified by the GMD suite in-session. Bundled-OpenSSL
  scanner hits (CVE-2024-13176/CVE-2025-0306) are declared non-impacting
  by Zetetic (no ECDSA use).
  https://nvd.nist.gov/vuln/detail/CVE-2025-29087
  https://nvd.nist.gov/vuln/detail/CVE-2025-6965
  https://www.zetetic.net/blog/2026/04/28/sqlcipher-4.15.0-release/
  https://discuss.zetetic.net/t/new-vulnerability-detected-in-openssl/6877
- mediapipe tasks-genai 0.10.35: no known CVE anywhere (NVD "mediapipe" =
  0 hits); 0.10.35 is latest.
- mlkit genai-prompt 1.0.0-beta3: no known CVE; latest. Closed source —
  absence of advisories is the strongest available check (UNVERIFIED
  beyond public databases).
- room 2.7.2, datastore 1.1.7, asset-delivery-ktx 2.3.0 (the old Play Core
  CVE-2020-8913 does not apply to the 2.x split artifacts), hilt 2.57,
  kotlinx-serialization 1.8.1, coroutines 1.10.2: all clean in OSV/GHSA/NVD.

### C.4 SQLCipher 4.6.1 → 4.17.0 migration note

4.7.0 changed keying behavior (no SELECT before keying; memory/init rework;
key-material obfuscation). Our path keys at open via
`SupportOpenHelperFactory(passphrase)` — GMD SoulVaultDeviceTest verifies
open / reopen-same-key / wrong-key-fails-loudly / file-not-plaintext green
on 4.17.0. The ADR-003 zero-share claim (factory retains OUR array, so
zeroing it scrubs the factory's copy) was verified against 4.6.1 bytecode
only — **re-verify against 4.17.0** (S24 checklist v3 §0); either outcome
is safe (worst case: one more residual copy, same class as the accepted
native residue).
