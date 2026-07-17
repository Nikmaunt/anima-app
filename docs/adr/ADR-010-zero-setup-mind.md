# ADR-010: Zero-setup mind — Play Asset Delivery pack, Nano auto-detect, optional BYOK cloud

Status: accepted, 2026-07-17. Extends ADR-005 (tier architecture unchanged);
amends ADR-004 network policy (second and last sanctioned network module).

## Problem

The v0.2 GEMMA tier requires the user to accept a license on Hugging Face,
download a 657 MB file in a browser and import it via SAF. That is a
power-user quest, not a product. North star for v0.3: **install from the
store → open → the creature is alive and talking.** Zero manual model
downloads, zero SAF for the default path.

## Decision

Model resolution chain for the GEMMA tier (first hit wins):

1. **Play Asset Delivery pack** `:mind-pack` (fast-follow) carrying
   `gemma3-1b-it-int4.task` — the store path. Play fetches it automatically
   right after (or during) install; no user action, no login.
2. **User-downloaded model** (existing https downloader) — power users.
3. **SAF-imported model** (existing importer) — power users, air-gapped path.

NANO (AICore) stays the auto-detected first tier where the device supports
it (ADR-005 order unchanged: Nano → Gemma → sleep). A **cloud mind** becomes
a user-opt-in override (never a fallback the app chooses silently) — see
ADR-011.

### Why fast-follow, not install-time

Install-time packs are delivered as split APKs readable only through
`AssetManager`; MediaPipe's `LlmInference` API accepts a **file path** only.
An install-time pack would force a 657 MB copy out of the APK on first open
and the split stays installed → **~1.3 GB permanent footprint** (plus a 2×
free-space requirement during every install/update). Fast-follow packs land
as real files in Play-managed storage (`AssetPackManager.getPackLocation()`)
that MediaPipe can open directly — **single copy**, and Play still fetches
it automatically post-install. Cost: the model may still be downloading for
the first minutes after install; the app shows honest "mind is being born"
progress (AssetPackStateUpdateListener) and the creature already lives —
body senses, deterministic rituals and dreams don't need inference.
(Sizes/limits: research-v3 §A.1 — 1.5 GB per pack, 657 MB artifact fits.)

### Sideload / debug behavior

Plain `assembleDebug` APKs carry no packs (bundle-pipeline only). The chain
just falls through to downloaded/SAF — which is exactly the dev workflow.
`bundletool --local-testing` exercises the pack path when needed
(research-v3 §A.2). The pack directory is probed at runtime; no Play Core
API failure is fatal.

### License compliance (research-v3 §A.3)

Bundling Gemma weights is permitted Distribution under the Gemma Terms of
Use. Obligations implemented in-repo:
- `mind-pack/src/main/assets/GEMMA_NOTICE.txt` with the exact required
  notice text + pointer to the Terms;
- the Mind screen's model row links the notice;
- the Prohibited Use Policy pass-through lands in the store listing terms
  (release checklist item — cannot be done from the repo).
The actual `.task` file is NOT in git (gitignored slot + README): weights
enter the AAB at bundle-build time from the owner's licensed download.

### Why not "Play for On-device AI" (AI packs)

Same limits + RAM/SoC device targeting would be ideal, but the surface is
beta (research-v3 §A.4) and adds AGP plugin surface we cannot exercise
without a Play Console; recorded as the designated migration once GA. The
RAM gate therefore lives in-app (below).

### RAM gate

Gemma 3 1B int4 peaks ~1.0–1.2 GB (research-v3 §A.6; official 4 GB-RAM
minimum recommendation). In-app gate: `ActivityManager.memoryClass`/
`getMemoryInfo().totalMem` — devices with **< 6 GB total RAM** do not load
the pack model spontaneously; they get Nano if present, else the honest
sleep + a Mind-screen explanation (the user may still force-enable Gemma or
choose cloud). 6 GB keeps a comfortable LMK margin on real devices.

## Device matrix (what a user actually gets)

| Device | Store install | Sideload (debug APK) |
|---|---|---|
| Galaxy S24 (8 GB, no Nano — research-v3 §A.5) | GEMMA from pack, alive after pack fetch (~minutes on Wi-Fi) | asleep until adb/SAF/download model; then GEMMA |
| Pixel 9/10, S26 (Nano list) | NANO instantly; pack model unused (Gemma stays available as fallback file) | NANO instantly |
| Budget 4 GB RAM | RAM-gated: no spontaneous Gemma. Nano absent → creature lives without inference; user choices: cloud (BYOK) or force-Gemma (warned) | same |
| Any + BYOK cloud opt-in | CLOUD when enabled & online; degrade to local tier offline | same |

## Consequences

- `:mind-pack` asset-pack module joins the build; `:app` lists it in
  `assetPacks`. AAB grows to ~700 MB (store-side; base APK stays small).
- `:core:model-delivery` gains the Play `asset-delivery-ktx` dependency and
  a `PackModelSource`; `MindModelLocator` resolution becomes
  installed-file-first, pack second (a user-chosen file beats the bundled
  default).
- NetworkIsolationTest v3: INTERNET manifests = exactly
  {model-delivery, cloud-mind}; source-level network APIs allowed in the
  same two; version catalog still bans third-party HTTP stacks entirely.
- Play returns `NETWORK_ERROR`/`INSUFFICIENT_STORAGE` etc. per pack state;
  all surfaced as creature-voiced progress/failure in the Mind screen, and
  the app remains fully functional without the model — the honesty rule
  (never fake a mind) is unchanged.
