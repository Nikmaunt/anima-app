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
