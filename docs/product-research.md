# Product & design research — v0.3, researched 2026-07-17

Method: two independent web-research passes (competitors/onboarding;
design/privacy/store), every claim cited. **UNVERIFIED** marks claims we could
not confirm against a primary source (review aggregators behind 403s,
single-vendor stats). Each finding ends in a concrete product decision; the
backlog at the bottom is the actionable summary with quick-win flags.

## 1. Competitors and adjacent apps

### Widgetable — the toxic benchmark
- Loved for: co-parenting pets with a partner/friend (social bond as core).
  https://marlvel.ai/apps/com-widgetable-theme
- Hated for: 30–90 s unskippable ads **to feed the pet**, ~$19 sub to remove
  ads, premium-only care items (UNVERIFIED at individual-review level —
  aggregator summaries only: https://justuseapp.com/en/app/1641107226/widgetable-pet-widget-theme/reviews,
  https://www.smileblogs.com/article/1400). Ad-gated care is the canonical
  toxic loop.
- → Decision: care actions in Anima are never gated by anything. Confirmed
  v0.2 anti-pattern list; no change needed.

### Finch — the ethical benchmark
- Loved for: "bird never dies, streaks never punish", no ads, functional free
  tier; flipped motivation (self-care feeds the bird). 4.8–4.9★.
  https://habitbox.app/blog/finch-app-review,
  https://www.makeuseof.com/finch-app-virtual-pet-motivation/
- Criticised for: gamification becoming a chore, dense early UI.
- → Decision: absence handling stays "it slept and dreamed" (v0.3 implements
  dreams literally); no streaks, no guilt. Keep early UI to ONE system
  (the creature) — resist adding meters.

### Replika — the trust cautionary tale
- Jan 2025 FTC complaint: premium upsells injected at emotionally charged
  moments (https://time.com/7209824/replika-ftc-complaint/); €5M Garante fine
  May 2025; 2023 ERP removal caused mass grief — a cloud-controlled companion
  can be changed under you. Sept 2025 FTC 6(b) inquiry into 7 companion firms.
  https://theconversation.com/i-tried-the-replika-ai-companion-and-can-see-why-users-are-falling-hard-the-app-raises-serious-ethical-questions-200257
- → Decision: permanence is a marketable guarantee unique to local-first:
  "no one can change or shut down your creature". Goes on the trust page.
  Cloud mind (v0.3, BYOK) must be opt-in, clearly labeled, and NEVER able to
  alter the creature's local soul without the same user confirmation.

### Talking Tom / Outfit7 — ads near companions end in regulator findings
- ASA/CARU findings on inappropriate ads to children; Common Sense privacy
  score 35% "Warning". https://privacy.commonsense.org/evaluation/Talking-Tom-Cat
- → Decision: no ads ever; already policy. Confirmed.

### Pixel Pals / pet-widget genre
- Complaints: shallow interaction ("pets mostly sit there"), 2-free-pets
  paywall, progress wiped on restart (UNVERIFIED aggregator summaries).
- → Decision: depth over variety — 8 concepts stay free; soul/journal are
  durable (SQLCipher + export). Nothing to change; validates v0.2 design.

### Tolan — anti-addictive companion design is commercially viable
- Non-human alien companion, "avoids dopamine-driven mechanics", nudges users
  to log off; $12M ARR by 07-2025.
  https://www.geekwire.com/2025/ai-companionship-app-tolan-raises-20m-to-help-more-people-grow-with-a-virtual-alien-friend/
- → Decision: keep creature stylized/non-human; creature occasionally
  suggests ending the session at night (dreams feature ties in: "I'll go
  dream now"). Quick-win: one gentle "go live your day" line in starter pool.

### Dot shutdown (10-2025) — "your friend can be deleted remotely"
- https://techcrunch.com/2025/09/05/personalized-ai-companion-app-dot-is-shutting-down/
- → Decision: soul export stays first-class; trust page states "delete Anima
  and everything goes with it; keep the soul file and nothing is lost".

### On-device-pet niche
- No polished consumer on-device-LLM pet app found (UNVERIFIED negative);
  nearest neighbours are model-runner utilities (PocketPal AI) and an
  open-source AI Tamago. Anima's niche appears open.

## 2. Onboarding practices 2025–2026

- TTFV ≤60 s, 3–5 screens; every pre-value screen drops completion 10–15%.
  https://uxcam.com/blog/10-apps-with-great-user-onboarding/ ,
  https://vmobify.com/blog/app-onboarding-best-practices
- Value-before-signup (Duolingo) is the highest-impact retention pattern;
  Anima has no signup — the wow IS "creature alive in 60 s".
- Permissions: Google DevRel — launch-time prompts increase denial; ask in
  context after the user reaches the relevant feature, educate first,
  handle denial gracefully.
  https://medium.com/androiddevelopers/top-tips-for-adopting-androids-notification-permission-bf69afd677b8 ,
  https://developer.android.com/training/permissions/usage-notes
- No published grant-rate data for Notification *Listener* access (Settings-
  level) — treat as maximally sensitive; UNVERIFIED "opt-in doubles when
  delayed" stat (aggregator-repeated).
- Push spam kills grants: 46% opt out at 2–5 pushes/week.
  https://www.mobiloud.com/blog/push-notification-statistics
- → Decisions (quick-wins, Phase 3):
  1. Onboarding ≤3 screens before first conversation: hello/hatch →
     concept+name → talk. Everything else moves to contextual moments.
  2. Notification Access is requested ONLY from the Notifications screen,
     narrated by the creature ("I could hear the phone's world…"), never in
     onboarding. Full value path if denied.
  3. Anima sends NO push notifications at all (status quo) — stays policy.

## 3. Flagship design trends 2026 (Material 3 Expressive)

M3 Expressive (I/O 2025; Android 16 QPR1 rollout 09-2025): physics-based
MotionScheme springs, shape morphing (35-shape library), expressive color,
oversized display type, haptics as a design layer, ~15 new components.
Third-party adoption still thin mid-2026 — adopting reads flagship-fresh.
https://m3.material.io/blog/building-with-m3-expressive ,
https://developer.android.com/jetpack/androidx/releases/compose-material3
(stable 1.4.0 2026-07-15; some expressive APIs still @Experimental — verify
against our Compose BOM before use).

Dynamic color guidance for brand-forward apps: creature palette is the brand —
dynamic color only for chrome, if ever; full static scheme stays.
https://m3.material.io/styles/color/dynamic/choosing-a-source

Haptics: semantic constants via performHapticFeedback (free system-setting
compliance); rich moments via VibrationEffect.startComposition gated by
Vibrator.arePrimitivesSupported(), fallback chain primitives → predefined →
one-shot → nothing. https://developer.android.com/develop/ui/views/haptics/haptics-apis

### Screen audit — «экран → что дотянуть»

| Screen | Gap vs 2026 flagship | Action (Phase 3) |
|---|---|---|
| Home (creature+chat) | haptics only on purr; no morph moment | haptic CONFIRM on fact-confirm, TICK on send; charge-celebration composed primitive (gated); keep hand-rolled springs (already physics-based) |
| Onboarding | 4+ steps incl. permission talk | cut to 3 screens; move everything contextual |
| Body diary | text list only; no data-viz | 24h/7d charge Canvas chart + storm↔drain correlation line; empty state with creature |
| Soul | plain list; empty state bare | creature-present empty state; haptic on remember/forget |
| Story (timeline) | plain; fine | empty state with creature |
| Notifications | cold permission ask | contextual creature-narrated ask; empty state |
| Mind (settings) | utilitarian | keep honest-tech tone; add cloud section (Phase 1C) with plain-language warning |
| Settings | fine | add "Разум: локальный/облачный" always-visible row; FLAG_SECURE toggle; trust page link |
| Widget | static snapshot ok | (v2 optional — config concept choice) |

## 4. Privacy as a product

Copy patterns of successful privacy-first apps: ownership ("Your thoughts are
yours" — Obsidian), incapability over promise ("we can't read it"), blunt
negation lists ("No ads. No trackers. No kidding." — Signal), verifiability
(open formats, audits). Architecture-as-guarantee is the master pattern.
https://obsidian.md/ , https://signal.org/ ,
https://www.inkandswitch.com/essay/local-first/ ("the network is optional",
"The Long Now").

Play Data safety: "No data collected" label is itself a conversion asset;
on-device processing does not count as "collected".
https://support.google.com/googleplay/android-developer/answer/10787469

→ Decision: in-app trust page "Why Anima doesn't need the internet"
(Phase 3, feature/settings): hero "Your creature lives here. Only here.";
three negations; incapability statement; how-it-works (on-device model +
encrypted soul); "try airplane mode" proof; honesty section (what DOES use
network: model delivery, optional cloud mind — named provider, opt-in);
delete story. With cloud mind enabled the page shows exactly what leaves the
device.

## 5. Icon and store presence

Specs: Play icon 512×512 PNG ≤1024 KB, no pre-rounded corners; adaptive icon
66 dp safe zone + monochrome layer for themed icons (expected, not mandated —
UNVERIFIED any 2026 mandate); feature graphic 1024×500, one message, 5–7 word
headline, no award/price claims.
https://developer.android.com/distribute/google-play/resources/icon-design-specifications

Pet-app icon pattern: character face close-up, oversized eyes, one silhouette
readable at 48 px (Finch, Talking Tom).

Three concepts (vectors in `docs/store/`, Phase 2 deliverable):
1. **The Peek** — creature face rising from bottom edge, amber eyes, indigo
   field. Category-native, eye contact.
2. **The Ember** — glowing scalloped orb with eye-dots on near-black
   blue-violet; best monochrome behavior; the "alive glow in your pocket"
   thesis. RECOMMENDED primary.
3. **The Den** — dark tile with a glowing squircle aperture, creature curled
   inside; encodes "lives in the device"; riskiest at 48 px.

Feature graphic: creature left third, eye contact, headline right-of-center,
"No internet needed" pill. Headline candidates (<30 chars):
"A friend that lives offline" / "No cloud. Just company." /
"A little life in your phone".

## Backlog (decision → phase)

| # | Decision | Source | Phase |
|---|---|---|---|
| 1 | Onboarding ≤3 screens to first talk | §2 | 3 (quick-win) |
| 2 | Contextual creature-narrated Notification Access ask | §2 | 3 (quick-win) |
| 3 | Empty states with creature on all screens | §3 audit | 3 (quick-win) |
| 4 | Haptics per event map, arePrimitivesSupported-gated | §3 | 3 (quick-win) |
| 5 | Trust page "Why Anima doesn't need the internet" | §4 | 3 |
| 6 | Dreams = absence handling ("I slept and dreamed") | §1 Finch | 3 |
| 7 | Charge chart + storm↔drain correlation in diary | §3 audit | 3 |
| 8 | Cloud mind opt-in honesty screen naming the provider | §1 Replika | 1C |
| 9 | Icon concepts as vectors + feature graphic layout | §5 | 2 |
| 10 | "Go live your day" line in starter pool; night = creature sleeps | §1 Tolan | 3 |
| — | No ads / no care-gating / no death / no push spam / no variety paywalls | §1 | policy, confirmed |
