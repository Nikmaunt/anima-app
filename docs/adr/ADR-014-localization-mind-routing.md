# ADR-014: Localization set + language-aware mind routing

Status: accepted, 2026-07-18.

## Findings (research-v4 §7)

- Locale evidence: DE (61% Android, strongest documented privacy culture),
  ES (one locale, two continents), RU (66% Android; Play billing paused —
  reach, not revenue), PL (69% Android; absent from Forest AND Widgetable —
  a real gap), JA (Tamagotchi homeland, highest affinity — but iOS-majority
  and highest QA bar). Finch, the category leader, is EN-only.
- **Gemma 3 1B is an English-primary model.** The HF launch table says it
  plainly (1B: English; +140 languages: 4B/12B/27B) and the tech report's
  numbers confirm collapse (MGSM 2.04, Global-MMLU-Lite ≈ chance).
- Gemini Nano signals: EN certain; JA/ES/DE plausible (Chrome Prompt API
  list, ML Kit Summarization EN/JA/KO). RU/PL in no Nano list.
- Pseudolocales (en-XA expansion, ar-XB RTL) are the cheap pre-translation
  gate.

## Decision

- **v0.4 ships six locales: EN, DE, ES, RU, PL, JA** — full UI string
  localization through resources, no locale left partially translated
  (a partial locale is worse than none). JA copy flagged for native review
  before store release; FR noted as next candidate.
- **The mind routes by language honestly** (`MindLanguage` policy, single
  source of truth):
  - GEMMA (1B) tier: **always converses in English**, whatever the UI
    locale. Non-EN locales see a one-line honest note in chat settings
    ("my thoughts are in English for now — my small mind only speaks EN").
  - NANO tier: attempts the UI language only for locales with Nano evidence
    (EN, DE, ES, JA); otherwise EN.
  - CLOUD (BYOK) tier: user's model, UI language directly.
  - The persona/prompt layer states the target language explicitly; replies
    in the wrong language are NOT retried (small models loop badly) — the
    honest note covers the contract.
- **No machine-translated creature lines**: the fixed creature strings
  (greetings, diary phrases, care advice) are authored per locale in
  resources — they are product copy, not UI chrome.
- **TTS follows locale** (ADR-013): voice availability is checked per
  creature language; missing offline voice mutes rather than switching to a
  network voice.
- Pseudolocale test (`en-XA` overflow + `ar-XB` RTL smoke) joins the DoD;
  debug builds enable pseudolocales.

## Amendment (v0.4 close, 2026-07-18): string migration deferred to v0.5

The run's Phase 0 uncovered a shipping-critical defect (the WAL pool-growth
crash, audit-v03 F11) whose diagnosis, root-cause fix and E2E verification
consumed the budget v0.4 had implicitly reserved for migrating the ~240
inline UI strings to resources. Attempting the migration in the run's tail —
across every legacy screen, with ViewModel-produced strings needing context
plumbing — would have put the untouchable DoD (GMD, release builds, golden
verification) at risk, and a HALF-migrated base violates this ADR's own
"a partial locale is worse than none" rule.

Decision: v0.4 ships the localization INFRASTRUCTURE only — pseudolocales
enabled in debug builds, new modules (rest, wallpaper) resource-based from
birth, the six-locale store texts in docs/store/, and this ADR's routing
policy. The full string migration + RU (then DE/ES/PL/JA per ranking) is
the FIRST item of the v0.5 backlog, invoked per the v0.4 cut order
("локализация сверх EN+RU" was the sanctioned cut; RU itself moves with it
because the extraction cost, not translation, dominates). Recorded as a
deviation from the v0.4 brief in the final report.

## Amendment (v0.5, 2026-07-18): routing implemented; GEMMA-tier clause superseded

Audit-v04 recorded the honest gap: v0.4 shipped this ADR's routing as TEXT
only (no `MindLanguage` type existed). v0.5 Phase 1D implements it — with
one deliberate change against the original decision: the GEMMA tier is no
longer "always English", because the tier is no longer hard-wired to Gemma 3
1B. Routing now asks the ACTIVE model's registry spec
(`MindLanguageRouting.decide(uiLanguage, tier, spec)`, ADR-017): a
multilingual local model (Qwen2.5-1.5B default) answers natively; an
English-only model (legacy Gemma) falls back to EN behind the badge this
ADR promised. NANO stays EN-only-until-verified (stricter than the "Nano
evidence" list here — the S24 has no Nano to verify against, and honesty
beats optimism). The rest of this ADR (locale set, no-machine-translation,
TTS, pseudolocales) stands and is executed by v0.5 Phase 2.
