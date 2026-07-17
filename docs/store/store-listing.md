# Store presence — v0.3 concepts (research: docs/product-research.md §5)

## Icon

Three concepts as SVG in this directory; recommendation: **Concept 2
"The Ember"** (best silhouette at 48 px, best monochrome layer for themed
icons, encodes the product thesis — a warm living glow in a dark pocket).
Prototype 1 and 2 in a Play Store Listing Experiment when the listing
exists.

Production checklist (Play spec):
- Export 512×512 32-bit PNG ≤1024 KB, sRGB; **no** pre-rounded corners,
  no baked shadow (Play applies the mask).
- Adaptive icon: foreground = orb + eyes, background = #14142B; keep the
  orb inside the 66 dp safe zone.
- Monochrome layer: scalloped orb contour with eye-dot cutouts.

## Feature graphic (1024×500)

Layout in `feature-graphic.svg` (export to 24-bit PNG/JPEG, no alpha).
Composition: creature left third with eye contact toward the headline;
5-word headline; one factual pill ("No internet needed" — a feature
statement, not a banned award/price claim); no UI screenshots.

Headline candidates (<30 chars):
1. **"A friend that lives offline"** (27) — used in the layout.
2. "No cloud. Just company." (23)
3. "A little life in your phone" (27)

## Data safety (Play Console form)

- "No data collected / No data shared" — on-device processing does not
  count as collection. The optional BYOK cloud mind is user-configured
  traffic to the user's own provider; wording for the form to be
  finalized at listing time (release checklist), stated honestly in-app
  either way.

## Release checklist additions (cannot be done from the repo)

- Pass the Gemma Prohibited Use Policy restrictions through the store
  listing's terms of service (Gemma ToU §3.1(1)); the in-app notice file
  already ships (mind-pack/src/main/assets/GEMMA_NOTICE.txt).
- Store Listing Experiment: icon concept 2 vs 1.
