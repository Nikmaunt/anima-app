# Research v2 — verified 2026-07-17

Every claim carries its source; anything not confirmed against a primary
source is marked **UNVERIFIED**. Two independent web-research passes,
cross-checked; where secondary sources contradicted each other we went to
ground truth (bytecode / official indexes).

## A. Inference on the base Galaxy S24 (the owner's device)

### A.1 ML Kit GenAI Prompt API — S24 is out, permanently

- Latest artifact: `com.google.mlkit:genai-prompt:1.0.0-beta3` (Google Maven,
  updated 2026-07-13). https://dl.google.com/android/maven2/com/google/mlkit/genai-prompt/maven-metadata.xml
- The official device list names **no Galaxy S24 variant at all** (not even
  Ultra): Samsung entries are Z Fold7 / Z TriFold (nano-v2) and Galaxy S26
  family (nano-v3). S24 was skipped while S25/S26 were added through 2026 —
  on S24 `FeatureStatus.UNAVAILABLE` is terminal, by design.
  https://developers.google.com/ml-kit/genai
- v0.1's code comment said "incl. Galaxy S24" for UNAVAILABLE — correct in
  effect, but the v0.1 assumption "S24 Ultra would work" is **wrong**: no S24
  works. (S24 does run Gemini Nano for *Samsung's own* Galaxy AI features —
  that stack is not exposed to third-party apps.
  https://www.sammobile.com/news/all-samsung-devices-that-support-gemini-nano/)
- Prompt API input cap: ~4 000 tokens (~3 000 English words).
  https://developers.google.com/ml-kit/genai/prompt/android/get-started

### A.2 MediaPipe LLM Inference API — chosen Tier-2 runtime

- `com.google.mediapipe:tasks-genai:0.10.35` is the latest (Google Maven,
  2026-04-27; docs still show 0.10.27 — docs lag).
  https://dl.google.com/android/maven2/com/google/mediapipe/tasks-genai/maven-metadata.xml
- **Maintenance-only mode**; Google recommends LiteRT-LM
  (`com.google.ai.edge.litertlm:litertlm-android:0.14.0`, 2026-07-08) for new
  work. We stay on tasks-genai for v0.2 (documented, stable API; LiteRT-LM
  Kotlin surface less verified) and record the migration path in ADR-005.
  https://developers.google.com/edge/mediapipe/solutions/genai/llm_inference/android
  https://github.com/google-ai-edge/LiteRT-LM
- API surface (docs, same page): `LlmInference.LlmInferenceOptions`
  (`setModelPath`, `setMaxTokens` — default 512, **input+output combined**),
  `LlmInferenceSession.LlmInferenceSessionOptions` (`setTopK` default 40,
  `setTemperature` default 0.8), session `addQueryChunk` +
  `generateResponseAsync` = multi-turn. Model formats: `.task` and
  `.litertlm`. CPU and GPU backends selectable.
- No constrained decoding / JSON schema in tasks-genai — prompt-only JSON
  with app-side tolerant parsing. (LiteRT-LM advertises constrained decoding;
  whether the stable Kotlin 0.14.0 exposes it: **UNVERIFIED**.)
  https://developers.googleblog.com/blazing-fast-on-device-genai-with-litert-lm/

### A.3 Gemma model for Tier 2

- Chosen class: **Gemma 3 1B IT int4** — canonical mobile artifact 529 MB
  (.task, context 2048) / 584 MB (`gemma3-1b-it-int4.litertlm`); quantized
  peak RAM 657–1 504 MB (+ up to ~1.2 GB GPU memory when on GPU); blog
  recommends ≥4 GB device RAM → 8 GB S24 passes with margin.
  https://developers.googleblog.com/gemma-3-on-mobile-and-web-with-google-ai-edge/
  https://huggingface.co/litert-community/Gemma3-1B-IT
- Perf (official, S24 Ultra / SD 8 Gen 3 — same SoC as US S24): CPU int4
  prefill 138 tok/s, decode 50 tok/s; GPU prefill 2 585 tok/s, decode
  56 tok/s. GPU wins prefill, barely wins decode. Base-S24/Exynos-2400
  numbers: **UNVERIFIED — no published measurements found**; expect below
  Ultra (smaller vapor chamber; Xclipse 940 vs Adreno 750).
  https://huggingface.co/litert-community/Gemma3-1B-IT
- Thermals: sustained load on S24 Ultra throttled GPU 680→231 MHz at 78 °C
  (third-party citing arXiv 2603.23640 — **UNVERIFIED**, paper not fetched).
  Design consequence: bursty short inference only, which matches the product
  (1–3 sentence replies).
- Smaller fallback: Gemma 3 270M IT (`gemma3-270m-it-q8.litertlm`, 304 MB) —
  exists but GPU support "WIP" per model card; not chosen.
  https://huggingface.co/litert-community/gemma-3-270m-it
- **No 500M-class Gemma exists**: ladder is 270M → 1B → 3n E2B (3.14 GB int4,
  too big for 8 GB) → Gemma 4.

### A.4 Model delivery reality — license gate changes the design

- All official Gemma artifacts on Hugging Face (`litert-community/*`) are
  **license-gated**: anonymous fetch returns HTTP 401 (verified directly by
  the research pass). Kaggle likewise requires account+consent
  (**UNVERIFIED** for exact Kaggle flow today).
  https://huggingface.co/litert-community/Gemma3-1B-IT
- Consequence: an in-app "download the mind" button **cannot fetch the
  official artifact anonymously**. Design (ADR-005): SAF import is the
  primary, always-works path ("bring the file yourself" — user downloads via
  browser after accepting the license); the in-app downloader exists for a
  user-supplied direct URL (self-hosted mirror or HF `?download=true` link
  with the user's token embedded — their choice, their license acceptance),
  Wi-Fi-only by default, resumable, with SHA-256 verification when the user
  provides an expected hash.
- No official SHA-256 manifest is published for Gemma artifacts; HF Git-LFS
  OIDs *are* SHA-256 per file but sit behind the same license gate. Pinned
  hashes therefore ship as **UNVERIFIED** placeholders the owner fills in
  after their own authenticated download (documented in the S24 checklist).
  https://github.com/huggingface/huggingface_hub/issues/2364

## B. Platform APIs for Phase 2

### B.1 Glance widget

- Use `androidx.glance:glance-appwidget:1.2.0-rc01` (stable 1.1.1 is a 2024
  build; 1.2.0-rc01 2025-12-03).
  https://developer.android.com/jetpack/androidx/releases/glance
- Glance renders to **RemoteViews** (all RemoteViews limits apply).
  `ImageProvider(bitmap)` exists — static snapshot rendering is viable.
  Bitmap budget: total bitmaps in one update ≤ ~screenW×screenH×4×1.5 bytes;
  pre-scale to widget dp size.
  https://developer.android.com/reference/kotlin/androidx/glance/ImageProvider
  https://developer.android.com/develop/ui/compose/glance
- **Battery broadcast trap (kills the original trigger plan):** none of
  ACTION_BATTERY_LOW / OKAY / POWER_CONNECTED / POWER_DISCONNECTED are on the
  implicit-broadcast exemption list — manifest receivers get **none** of them
  on API 26+. The old training-page XML sample is legacy.
  https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions
  → Design: WorkManager periodic refresh (floor 15 min; we use 30) +
  update-on-app-events (charge listener already runs while app alive) +
  `setRequiresCharging` one-shot works for charge transitions. ADR-007.
- WorkManager periodic floor: 15 min (`PeriodicWorkRequest`).
  https://developer.android.com/reference/androidx/work/PeriodicWorkRequest

### B.2 SQLCipher: FTS5 + passphrase handling

- FTS5: compiled into sqlcipher-android (`-DSQLITE_ENABLE_FTS5` in the repo's
  Android.mk). Prebuilt-AAR == repo flags: high confidence, formally
  **UNVERIFIED** → soul search does a `fts5` pragma smoke-check at first use
  and falls back to LIKE (recorded in ADR-008).
  https://github.com/sqlcipher/sqlcipher-android/blob/master/sqlcipher/src/main/jni/sqlcipher/Android.mk
- Passphrase: **ground truth from 4.6.1 bytecode (local AAR, javap):**
  `SupportOpenHelperFactory` retains the byte[] as-is; no clearPassphrase
  parameter exists (the boolean is `enableWriteAheadLogging`); nothing in the
  Support layer ever zeroes it. Secondary sources disagreed (one claimed a
  clearing default, CommonsWare said none) — bytecode settles it: **no
  library zeroing**. App-side eager-open-then-zero implemented; ADR-003
  addendum.

### B.3 Encrypted soul backup (KDF)

- PBKDF2WithHmacSHA256 via `javax.crypto` — available since API 26, zero
  extra deps. OWASP 2026 recommendation: 600 000 iterations (Argon2id
  preferred in absolute terms but needs a native dep).
  https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
- argon2kt last release 1.6.0 (2024-09-06) — stale-ish; adds native dep;
  rejected for v0.2.  https://github.com/lambdapioneer/argon2kt
- Envelope: custom minimal format — magic+version ‖ salt(16) ‖ iter(int32) ‖
  GCM IV(12) ‖ AES-256-GCM ciphertext. Params in header so counts can rise.

### B.4 Voice input

- `EXTRA_PREFER_OFFLINE` is a **hint**; docs: "may have no effect" — no
  offline guarantee on any OEM.
  https://developer.android.com/reference/android/speech/RecognizerIntent#EXTRA_PREFER_OFFLINE
- The verifiable path (API 31+): `SpeechRecognizer.isOnDeviceRecognitionAvailable`
  + `createOnDeviceSpeechRecognizer` — binds the on-device service by
  construction. S24-specific behavior: **UNVERIFIED**, needs the manual
  checklist. → Feature ships behind a toggle, on-device recognizer only,
  honest "unavailable" state otherwise (ADR-009).
  https://developer.android.com/reference/android/speech/SpeechRecognizer#createOnDeviceSpeechRecognizer(android.content.Context)

### B.5 Baseline profile

- Plugin `androidx.baselineprofile` 1.4.1 (benchmark 1.4.1, 2025-09-10).
  Generation **requires a device/emulator** (non-rooted OK on API 33+; GMD
  optional). No device-free path exists; the committed
  `app/src/main/baseline-prof.txt` is consumed directly by AGP.
  https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile
  → v0.2: generator module wired; running it is an S24-checklist item
  (ADR-006). No profile is committed until generated on hardware — an empty
  committed profile would be a lie.

### B.6 Static analysis versions

- detekt stable = 1.23.8 (2.0 in alpha, needs Kotlin ≥2.2.20 toolchain);
  works on Kotlin 2.2 source for rule analysis without type resolution.
  https://github.com/detekt/detekt/releases
- ktlint engine 1.7.1 (Kotlin 2.2-aware) via `org.jlleitschuh.gradle.ktlint`
  13.1.0.  https://github.com/ktlint/ktlint/releases
  https://github.com/JLLeitschuh/ktlint-gradle/releases
