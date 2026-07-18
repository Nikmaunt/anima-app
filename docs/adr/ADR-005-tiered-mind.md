# ADR-005: Tiered mind — Nano where possible, Gemma via MediaPipe on S24, honest sleep otherwise

Status: accepted, 2026-07-17. Supersedes the single-backend part of ADR-002
(the MindEngine interface and its honesty rules stand unchanged).

## Problem

The owner's device is a base Galaxy S24. Research (docs/research-v2.md §A.1)
established that **no S24 variant is, or ever will be, on the ML Kit GenAI
Prompt API device list** — Google skipped the S24 generation while adding
S25/S26. v0.1's only real backend was Nano, so the product's core — talking
to the creature — was dead on the target device.

## Decision: three tiers behind the same interface

| Tier | Backend | When |
|------|---------|------|
| 1 NANO | ML Kit GenAI Prompt API → AICore (system-managed Gemini Nano) | `checkStatus()` ∈ {AVAILABLE, DOWNLOADABLE, DOWNLOADING} |
| 2 GEMMA | MediaPipe `tasks-genai:0.10.35` + **Gemma 3 1B IT int4** (~529–584 MB artifact), loaded from an app-private file | Nano unavailable AND a validated model file is present |
| 3 ASLEEP | none | neither — the creature lives, converses not; never faked |

`TieredMindEngine` (core:mind) probes in that order once per process start
(re-probed on model install/delete). The `MindEngine` contract from ADR-002 is
unchanged: honest availability, no fabrication, extraction candidates are
suggestions requiring explicit user confirmation.

Runtime choice: tasks-genai is in **maintenance-only mode**; its successor
LiteRT-LM (0.14.0) is the designated migration target (it would also bring
constrained decoding for JSON). We ship v0.2 on tasks-genai because its Kotlin
API surface is documented and stable while LiteRT-LM's is young; migration is
mechanical (same .litertlm artifacts) and recorded as v0.3 candidate work.

## Context budgets (measured constraints, enforced by PromptBuilder)

- **Nano**: input cap ~4 000 tokens (official docs). Budget in chars at a
  conservative 3 chars/token: **9 000 chars** (unchanged from v0.1).
- **Gemma 3 1B (ekv2048 class artifact)**: `maxTokens` is **input + output
  combined**, default 512, model KV-cache ceiling 2048. We set maxTokens =
  2048, reserve 320 tokens for the reply → ~1 700 input tokens →
  **5 100 chars** budget (3 chars/token floor).
- PromptBuilder takes the budget as a parameter now; trimming order is
  unchanged (dialogue → facts → bare persona+body). On-device token-count
  measurement on S24: manual-checklist item (sizeInTokens exists in the API
  historically but is UNVERIFIED in 0.10.35 — if present, we log actuals).

## Sampling

- Chat session: topK 40, temperature 0.8 (API defaults, match the persona's
  gentle variability).
- Fact extraction: separate one-shot session, temperature 0.2 — JSON needs
  determinism more than sparkle. Prompt-only JSON (no constrained decoding in
  tasks-genai); `FactJson` tolerant parser drops garbage silently; schema
  validation + user confirmation remain mandatory.

## Model delivery (see also ADR: core:model-delivery in research-v2 §A.4)

Gemma artifacts are license-gated on every official host — anonymous in-app
download of the official file is impossible. Therefore:

1. **SAF import is the first-class path**: "bring the file yourself". The
   user accepts Google's license in a browser, downloads the artifact, picks
   it in the app; we stream-copy it into app-private storage and validate.
2. **In-app downloader** exists for a user-supplied HTTPS URL (their mirror,
   or a tokenized HF URL — their license acceptance, their choice): explicit
   user action on a dedicated screen showing size, Wi-Fi-only by default,
   resumable (HTTP Range), SHA-256 verified when an expected hash is given.
3. Validation without an official hash: size window + load smoke test at
   selection time; the engine refuses files that fail to load rather than
   pretending. Official SHA-256s are license-gated (UNVERIFIED placeholders
   in code; the owner can pin them after their own authenticated download).
4. Download runs **only while the app is foreground** (no service — the
   background-entity budget stays "listener + widget triggers only").
   Interrupted downloads resume.

## Network isolation consequence (amends ADR-004)

`:core:model-delivery` is the **single sanctioned network exception**:
- Only its module manifest declares `android.permission.INTERNET`.
- It uses platform `java.net.HttpsURLConnection` — the "no network stack in
  the version catalog" invariant survives verbatim.
- **Honest limitation:** Android permissions are app-wide. Once merged, the
  whole process technically may open sockets; OS-level scoping to one module
  does not exist. Enforcement is therefore build-time: NetworkIsolationTest
  v2 asserts (a) INTERNET originates only from model-delivery's manifest,
  (b) no source outside model-delivery touches network APIs, (c) no module
  build file adds a network dependency, (d) DataTransport telemetry
  components stay stripped, (e) the merged-manifest check can no longer pass
  vacuously (the test fails if no merged manifest exists).

## Consequences

- The S24 gets a real conversing creature for the price of one 530 MB
  user-initiated import; devices with AICore use Nano at zero storage cost.
- Two model-quality profiles exist (Nano vs 1B int4); the persona prompt is
  shared, and the 1B's occasional lower coherence is accepted as the honest
  price of local-only inference on this hardware.
- RAM: 1B int4 peaks ~0.7–1.5 GB — acceptable on 8 GB; the engine is
  released when the chat closes (LRU: model unloads on background).
- Thermals favor bursty short replies — which is the product's shape anyway.

## Errata (v0.4, 2026-07-18)

"Probes in that order once per process start" was never how the code worked:
tier probing is per call (a cheap binder/StateFlow read), deliberately
stateless — installing or deleting a model or going offline needs no
invalidation choreography. The per-call design is the one on record.
