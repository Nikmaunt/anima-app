# ADR-001: Character technology — hybrid Canvas + springs + AGSL, no Rive/Lottie

Status: accepted, 2026-07-17.

## Decision

The creature engine is 100% code-authored Kotlin:

1. **One `withFrameNanos` frame loop** per creature surface, gated by
   `repeatOnLifecycle(RESUMED)`; time lives in a `mutableLongStateOf` read only
   inside draw lambdas (draw-phase-only invalidation).
2. **Bodies are procedural Compose Canvas drawing** — paths with harmonic
   radial offsets, layered gradients — plus **spring physics** (`Animatable`,
   velocity-preserving `spring()`) for interaction and chained springs for
   secondary motion (ears, tentacles).
3. **AGSL `RuntimeShader` as the skin layer** (glow, metaball goo, noise
   shimmer) via `ShaderBrush` inside the same draw scope, runtime-gated to
   API 33+ with a gradient fallback below; shaders pre-warmed once.
4. All "life" comes from deterministic oscillators + seeded value noise driven
   by the frame clock. No timers outside the frame loop, no LLM in the motion
   path.

## Why

- Rive: strongest runtime, but rigs are authored in the Rive editor — fails
  the hard requirement "авторится кодом" in this session. Lottie: AE-exported
  JSON, no physics, no gesture interruption. Both rejected on authoring, not
  quality (research.md §A4).
- Canvas+springs+AGSL has verified precedents for every piece (jellyfish
  DevRel article, 2025 metaballs series, RevenueCat frame-loop case study) and
  fits the 60fps/battery budget by construction: creature-bounded shader,
  zero per-frame allocation, full stop when not RESUMED.

## Consequences

- minSdk stays 31 (donor discipline); the AGSL layer is a runtime capability,
  not a floor. Every concept must look production-grade with the fallback too.
- Engine core (oscillators, rig math, state machine) is a pure-JVM library —
  unit-testable without a device, mirroring hermes-lens's pure-engine rule.
