# Play screenshot scenario — 6 frames (2026-07-18)

Rules verified (research-v4 §8): min 2 / max 8 per device type; featuring
needs ≥4 at ≥1080px; text ≤20% of the image; no device frames or outside
graphics; no "best/#1"; only real in-app experience. First ~3 portrait
frames are visible without interaction (a video would displace frame 1 —
skipped at launch deliberately). Captions ≤4 words, set inside the safe
text budget.

| # | Screen (real app surface) | Caption (EN) | Why this slot |
|---|---|---|---|
| 1 | Hero: creature alive on the home screen, warm idle pose, no UI chrome | **A friend that lives offline** | Value promise in the always-visible slot; pet-first (Finch pattern), not a feature list |
| 2 | Chat mid-conversation, the "on-device" mind indicator visible | **Chats right on your phone** | The "how" in the second guaranteed-visible slot; on-device AI is the category-unique mechanic |
| 3 | Creature content and active with the status bar showing airplane mode | **No internet? Doesn't care.** | Proof of the promise; targets the high-volume offline/no-wifi intent; last widely-seen frame |
| 4 | Anima's own trust screen: "No accounts · No ads · No tracking" rows | **It can't track you** | Trust slot; converts the Data safety label into a visual — no competitor does this in a frame |
| 5 | Soul/diary view with the lock glyph, "encrypted here" note | **Memories stay encrypted here** | Differentiation depth; "encrypted" is scannable and consistent with listing keywords |
| 6 | Return-after-a-week scene: warm greeting, no sad/sick state | **Never dies. Never guilts you.** | Emotional closer; answers the documented Pou/Finch complaint pattern (sick pets, streak guilt) |

Production notes:
- Localize captions per store language (store-listing-v4.md has the six
  locales; JA needs native review).
- Capture at 1080×2400 on the S24 (matches the manual checklist device),
  light theme for frames 1-3, dark for 5 (shows both).
- Two spare slots reserved for seasonal/localized frames later.
- Frame 3's airplane-mode status bar is real UI state, not an overlay —
  compliant with "real in-app experience".
