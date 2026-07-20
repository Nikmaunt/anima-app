# Research v6 — int4 artifact pipeline + release engineering (2026-07-19)

Verification discipline: every claim below carries its source; anything not
confirmed against a primary source is marked **UNVERIFIED**. Gathered by two
web-research passes run 2026-07-19.

## A. Qwen2.5-1.5B int4 conversion pipeline (Phase 1)

### A.1 The toolchain moved

- `ai-edge-torch` was renamed → **`litert-torch`** (pip `litert-torch`; the
  old PyPI name is frozen at 0.7.2). Latest release **0.9.1, 2026-05-19**.
  https://github.com/google-ai-edge/litert-torch,
  https://pypi.org/pypi/litert-torch/
- Requirements per README: **Python ≥3.10 <3.14, Linux only** (WSL2 works),
  PyTorch ≥2.4, tf-nightly. Conversion is **CPU-only** per Google's guide;
  official wall-time for a 1B model: 10–30 min on 8 cores.
  https://ai.google.dev/gemma/docs/conversions/hf-to-mediapipe-task
- RAM: not documented; **UNVERIFIED estimate 16 GB min** (1.5B fp32 weights
  ≈ 6.2 GB plus conversion overhead).

### A.2 Qwen2.5 is a first-class example

- `litert_torch/generative/examples/qwen/convert_to_tflite.py` supports
  `--model_size` ∈ {0.5b, **1.5b**, 3b}.
  https://github.com/google-ai-edge/litert-torch/tree/main/litert_torch/generative/examples/qwen
- Quantization recipes (`generative/utilities/converter.py`): `none`,
  `dynamic_int8` (default), `weight_only_int8`, `fp16`,
  **`dynamic_int4_block32`**, **`dynamic_int4_block128`**.

### A.3 Packaging: .task vs .litertlm

- `.task` bundling (`mediapipe.tasks.python.genai.bundler`) requires a
  **SentencePiece tokenizer.model — Qwen2.5 has none** (HF BPE
  tokenizer.json only). Open issue since Oct 2024:
  https://github.com/google-ai-edge/litert-torch/issues/275,
  https://github.com/google-ai-edge/mediapipe-samples/issues/468
- **`.litertlm` is the way**: `litert_torch/generative/utilities/
  litertlm_builder.py` (pip `litert-lm-builder` 0.14.0) accepts **HF
  tokenizers** (`add_hf_tokenizer`) and knows Qwen presets.
- MediaPipe LLM Inference docs state litert-community models (incl.
  `.litertlm`) need no further conversion; the minimum tasks-genai version
  for `.litertlm` is **UNVERIFIED** — must be smoke-tested on 0.10.35.
  https://developers.google.com/edge/mediapipe/solutions/genai/llm_inference

### A.4 No ready int4 artifact exists (checked 2026-07-19)

`litert-community/Qwen2.5-1.5B-Instruct` (Google-run org; ungated,
Apache-2.0) publishes **only f32 and q8**:

| file | bytes | sha256 (lfs oid, prefix) |
|---|---|---|
| multi-prefill-seq_q8_ekv1280.task | 1 597 913 616 | 8d867a7c… |
| multi-prefill-seq_q8_ekv4096.litertlm | 1 597 931 520 | faa60663… |
| seq128_q8_ekv1280.task | 1 567 364 648 | 8771564e… |

All q8 exceed a decimal-1.5-GB pack budget; whether Play's "1.5 GB" is GB
or GiB is **UNVERIFIED** — treat q8 as not safely fitting (handoff v0.5
said the same). int4 estimate from the Gemma3-1B q4/q8 ratio (~0.65):
**≈1.0–1.1 GB** — fits. https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct

### A.5 Supply chain

- Base checkpoint: `Qwen/Qwen2.5-1.5B-Instruct`, **Apache-2.0, ungated**
  (3B is NOT — research-licensed; 1.5B confirmed).
  https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct
- Pinned revision for our conversion:
  `989aa7980e4cf806f80c7fef2b1adb7bc71aa306` (main @ 2026-07-19, HF API).
- HF file integrity: LFS oid = sha256, retrievable via
  `GET /api/models/{repo}/tree/main`; verify post-download.
  https://huggingface.co/docs/hub/en/security
- litert-community is Google-affiliated (org card → google-ai-edge;
  maintainers incl. MediaPipe leads) but has **no SLSA/sigstore
  provenance** — trust = org + pinned commit + sha256. ADR-018 records the
  decision to convert from the official Qwen checkpoint ourselves rather
  than trust third-party conversions.

### A.6 Runtime successor

- tasks-genai stays 0.10.35 (Google Maven metadata 2026-04-27; no 0.10.36).
- LiteRT-LM now has a stable Maven artifact:
  **`com.google.ai.edge.litertlm:litertlm-android:0.14.0`** (2026-07-08) —
  the designated migration target, out of scope this run (ADR-016 note).
  https://github.com/google-ai-edge/LiteRT-LM

## B. Release engineering facts (Phase 2)

1. **Target API**: deadline for API 36 is **2026-08-31** (new apps and
   updates; extensions to Nov 1). We already target 36 — compliant.
   https://support.google.com/googleplay/android-developer/answer/11926878
2. **Closed testing (personal account, created after 2023-11-13)**:
   **12 testers opted in continuously for the last 14 days** at the moment
   of applying for production. Opt-out+opt-in resets the clock.
   https://support.google.com/googleplay/android-developer/answer/14151465
3. **Privacy policy**: mandatory for ALL apps regardless of data
   collection; public URL, not a PDF, not an editable doc; GitHub Pages
   (rendered HTML) is acceptable.
   https://support.google.com/googleplay/android-developer/answer/10787469
4. **Content rating**: standard IARC questionnaire; claims of 2025/2026
   AI-specific IARC questions are **UNVERIFIED** (not found in official
   sources; Apple's 2025 change is often conflated).
   https://support.google.com/googleplay/android-developer/answer/9898843
5. **GenAI policy applies to on-device AI too**: "AI-Generated Content"
   policy covers text-to-text chatbot apps with no on-device/cloud
   distinction → Anima needs an **in-app report/flag mechanism for AI
   replies**, usable without leaving the app.
   https://support.google.com/googleplay/android-developer/answer/13985936
   → implemented this run (chat message long-press report; see Phase 3).
6. **CI**: GitHub-hosted Linux runners (incl. free ubuntu-24.04) have
   **KVM since 2024-04**; arm runners do NOT. udev rule step required.
   GMD+aosp-atd is the recommended CI path; known flakiness reports exist
   (issuetracker 193118030, 287312019) → workflow ships with a retry and
   the GMD job non-blocking on first offense (ADR in workflow comments).
   https://github.blog/changelog/2024-04-02-github-actions-hardware-accelerated-android-virtualization-now-available/
7. **actionlint v1.7.12** (2026-03-30) is the standard workflow linter.
   https://github.com/rhysd/actionlint
8. **Play App Signing**: Google-generated app signing key + local upload
   key (resettable if lost). Implemented: docs/release/signing.md.
   https://support.google.com/googleplay/android-developer/answer/9842756
9. **OSS licenses**: not a Play policy item per se but a license duty
   (Apache-2.0/MIT notice reproduction). Google's oss-licenses-plugin is
   maintenance-mode and broke on Gradle 8.8+ (issues #299, #390). Chosen:
   **AboutLibraries gradle plugin 12.2.4, build-time export only** — the
   15.x line requires Kotlin 2.4/Compose 1.11 (out of our pins); the
   screen itself is hand-rolled (no runtime dep, network ban intact).
   https://github.com/mikepenz/AboutLibraries
10. **PAD limits**: **1.5 GB per asset pack** (any type); base+install-time
    ≤4 GB; fast-follow/on-demand cumulative 30 GB.
    https://support.google.com/googleplay/android-developer/answer/9859372
11. **Play Console admin gates for new accounts**: identity verification +
    EU DSA trader declaration (non-trader OK for a free app).

## C. Decisions taken on this evidence

- **ADR-018**: convert Qwen int4 ourselves (supply chain §A.5), package
  `.litertlm`, smoke-gate on device; pack decision matrix there.
- CI: full pipeline on push; GMD job isolated with KVM prerequisites (§B.6).
- Licenses: AboutLibraries 12.2.4 build-time + hand-rolled screen (§B.9).
- GenAI policy: report-a-reply affordance added to chat (§B.5).
- Privacy policy hosting: GitHub Pages, docs/privacy-policy.md rendered
  (owner TODO in closed-testing plan §4).
