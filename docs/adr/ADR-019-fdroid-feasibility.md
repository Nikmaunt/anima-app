# ADR-019: F-Droid flavor feasibility (analysis only, no implementation)

Status: accepted (as analysis; implementation deliberately out of v0.6 scope)
Date: 2026-07-19

## Question

Can Anima ship on F-Droid, whose inclusion policy requires every dependency
to be FOSS (no proprietary blobs, no Google Play services)?

## Dependency-graph verdict (from the v0.6 aboutlibraries export — actual
resolved graph, not guesses)

Proprietary (blockers for F-Droid, license names verbatim):

| artifact | license | role |
|---|---|---|
| com.google.mlkit:genai-prompt/common + mlkit:common | ML Kit ToS | NANO tier (ADR-005): Gemini Nano via AICore |
| com.google.android.gms:play-services-base/basement/tasks | Android SDK License | transitive under ML Kit |
| com.google.android.play:asset-delivery(+ktx), core-common | Play Core ToS | Play pack delivery (ADR-010) |

FOSS (fine as-is):

- com.google.mediapipe:tasks-genai 0.10.35 — **Apache-2.0** (the GEMMA/QWEN
  tier engine; contains prebuilt .so, but of an Apache-licensed project
  from a trusted repo — acceptable to F-Droid via the google() repo rule,
  though they *prefer* source builds; anti-feature tag `NonFreeAssets` is
  NOT needed since models are user-supplied in this flavor).
- net.zetetic:sqlcipher-android 4.17.0 — BSD-style Zetetic license (the
  export shows an empty license id — a metadata gap in the POM, license is
  https://www.zetetic.net/sqlcipher/license/; noted for the licenses
  screen too).
- Everything else: AndroidX/Compose/Hilt/Room/Kotlinx — Apache-2.0.

## Feasibility

**Feasible with a product-honest degradation.** An `fdroid` flavor would:

1. Exclude `:core:model-delivery`'s Play path — the module keeps only the
   user-initiated download + SAF import chains (both already exist and are
   the fallback on plain-APK installs today; ADR-010 anticipated this).
2. Exclude the NANO tier: `:core:mind` gets a flavor source set where the
   AICore engine is replaced by a no-op locator (the tier ladder already
   treats NANO as optional — S24 has no Prompt API and lives on GEMMA).
3. Keep MediaPipe tier + BYOK cloud mind (cloud-mind is plain HTTPS via
   user's own key — FOSS client code; no SDK).

Product cost: no zero-setup mind out of the box on F-Droid (no Play pack,
no Nano) — first-run must offer download/SAF immediately. That is exactly
today's plain-APK behavior, so the UX path is already real and tested.

Engineering cost estimate: **M** — flavor plumbing in :app,
:core:model-delivery, :core:mind (2 source-set seams), CI matrix +1 build,
and an F-Droid metadata dir. No architecture changes: the seams (tier
ladder, delivery chain) were designed for absence.

Reproducible-builds note: F-Droid's builder would compile from a tag;
signing stays theirs (or reproducible-signing with our key). No blocker
identified beyond the flavor work.

## Decision

Record feasibility = YES (medium cost, honest degradation), implement
nothing in v0.6. Revisit after closed testing proves the Play track;
tracking item stays in the backlog with this ADR as the spec.
