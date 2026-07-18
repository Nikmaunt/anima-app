# Research v4 — inputs for the v0.4 iteration (2026-07-18)

Method: four parallel research tracks executed 2026-07-18 with live web
search/fetch. Every external claim carries a source URL; anything that could
not be confirmed against a live page is listed under **UNVERIFIED** at the end
of its section. Decisions extracted from findings are marked
"→ decision".

Sections: §1 behavioral anchors · §2 wellbeing positioning · §3 live
wallpaper · §4 lock screen / QS / shortcuts · §5 Wear OS · §6 system TTS ·
§7 localization · §8 marketing/ASO (artifacts in docs/store/).

---

## §1 Behavioral anchors 2026

### 1a. Mellow (disambiguated)

"Mellow" is ambiguous in 2026; three live candidates, all documented:

1. **Mellow inside "Friends – Pengu, Bao & Mellow" (SLAY GmbH)** — the focus
   companion ("Your calm AI study and focus companion… gentle structure,
   reminders, and encouragement"), one of three personas (Pengu = shared
   companion for two people, Bao = self-care). Feeding, outfits, mini-games,
   home-screen widgets. 4.9★ / 237K+ ratings; subscription $9.99/mo; recent
   reviews cite ad and notification fatigue.
   https://apps.apple.com/us/app/friends-pengu-bao-mellow/id6462927800
2. **"Mellow: Anxiety Relief & Calm" (Kaviam Wellness)** — renameable cloud
   companion, "grey and rainy on the heavy days, bright and sunny on the good
   ones", explicit "No guilt for missing a day", no streaks. v1.0 April 2026.
   https://apps.apple.com/us/app/mellow-anxiety-relief-calm/id6759960297
3. **Mellow Bot** — pomodoro+breathing+journal; headline: "Your thoughts and
   data stay on your device, and Mellow Bot works offline."
   https://play.google.com/store/apps/details?id=com.raslan.mellow

Decisions:
- Persona-split by function (focus pet vs care pet) → **ADAPT**: one creature,
  distinct demeanors per activity (rest-time vs chat-time), never multiple
  characters — protects single-creature attachment.
- "Gentle structure, never enforcement" → **ADOPT** (matches covenant).
- Cloud-AI dependency + ad economy + notification volume → **REJECT**.
- Weather/lighting reflecting mood → **ADOPT**: PAD state already drives the
  ambient rig; keep it the primary non-verbal state channel.
- Offline-first as store headline (Mellow Bot) → **ADOPT**: proof the claim
  sells in this exact niche.

### 1b. Forest

- Current positioning: "Spend your time well", tree per session, forest
  forever; Deep Focus Mode, allow-lists, group planting, real-tree program;
  60M+ downloads. https://forestapp.cc/
- The guilt arc: tree-death-on-leaving was made **opt-in** in April 2020
  ("please turn on 'Deep Focus Mode'" —
  https://x.com/forestapp_cc/status/1255331515623489537); allow-lists Nov 2022
  (https://x.com/forestapp_cc/status/1595711319931494417). 2025-26 reviews
  openly discuss "the guilt of killing a tree" and harsh copy ("Go back to
  forest immediately!") — https://calmevo.com/forest-app-review/,
  https://goalsandprogress.com/boost-your-focus-with-the-forest-app/
- Monetization drift (one-time → subscription) burned goodwill —
  https://productivewithchris.com/tools/forest/,
  https://justuseapp.com/en/app/866450515/forest-stay-focused/reviews

Decisions:
- Even Forest retreated from death-as-default → **ADOPT**: no negative outcome
  in rest sessions, period (our bar is stricter than their opt-in).
- Dead trees as "honest record" → **REJECT**: permanent failure artifacts are
  a soft shame mechanic; Anima keeps no record of failure at all.
- Durable positive artifact per session ("joins your forest forever") →
  **ADOPT**: completed rest sessions accumulate visibly (diary + evolution).
- Real-tree planting → **REJECT** (network/payments).
- Never claw back free features → **ADOPT** as standing policy.

### 1c. Finch (the ethical-streak reference)

- Store: "You don't get penalized/made to feel bad if you don't complete some
  goals." 4.9★ / 728K ratings, Editors' Choice.
  https://apps.apple.com/us/app/finch-self-care-pet/id1528595748
- Streak mechanics (official help center "Understanding Streaks",
  https://help.finchcare.com/hc/en-us/articles/37780736136205 — direct fetch
  403'd, wording via search index → UNVERIFIED details): streak counts **app
  open**, not task completion; missed day = bird "waits patiently"; Rainbow
  Stones can repair; free Streak Repair Saver every 3 adventures (max 2
  stored); **Pause Mode** freezes the streak officially.
- Energy loop: small self-care acts energize the bird → adventure → returns
  with stories + souvenirs; currency buys cosmetics; growth stages from
  cumulative care. https://habitbox.app/blog/finch-app-review,
  https://calmevo.com/finch-app-review/
- Failure mode: "gamification becomes a chore in month three"; all-or-nothing
  goal feel. https://www.autonomous.ai/ourblog/finch-self-care-app-review-full-breakdown

Decisions:
- Showing-up streaks + repair + pause → **ADOPT, then exceed**: Anima counts
  only what grows ("days together", sessions completed); nothing resets, so
  nothing needs repairing. Celebrate quietly, never dramatize a gap.
- Never-dies at 728K ratings → **ADOPT**: market proof that non-punitive
  retains better than death mechanics.
- Adventure-returns-with-stories → **ADAPT**: already have DreamWeaver; tie
  dream fragments to completed rest sessions.
- Cosmetic-only earned rewards → **ADAPT** into Wardrobe/Forms unlocks; keep
  drop cadence slow to dodge the month-three chore cliff.
- Core loop must stay ambient (creature IS the loop) → **ADOPT**.

### 1d. Widgetable

- "Adopt adorable virtual pets and co-parent them with your friends!" 4.9★ /
  376K ratings. https://apps.apple.com/us/app/widgetable-pet-widget-theme/id1641107226
- Aliveness without animation: discrete state changes (egg/hunger/bath/sleep)
  make the widget picture *different than last look*; fresh information, not
  motion. Neglect consequence: pet "leaves you" (coaxable back) — via review
  aggregation (fetch blocked → UNVERIFIED details).
- Ad-for-food economy ("watch 3 ads to get 2 more foods") → widely disliked.

Decisions:
- Widget aliveness = state-machine snapshots → **ADOPT** (Glance widget
  re-renders on state change; no animation runtime — matches our invariant).
- "Pet leaves" → **REJECT**: withdrawal of affection is abandonment-guilt.
  Anima's creature may nap/dream when idle, never withdraws.
- Social co-presence engine → **REJECT** (out of scope), but **ADAPT** the
  trick: the creature's own dreams/discoveries are the "other being" whose
  state you check.
- Ad economy → **REJECT**.

### Newcomers 2025-2026

- **Focus Friend (Hank Green / Honey B Games, 2025)** — bean knits socks
  while you focus; interruption = sadness, never destruction; artifacts
  become room décor; #1 App Store, **Google Play Best App of 2025**, Apple
  Cultural Impact Award, 1M+ Android installs.
  https://techcrunch.com/2025/11/18/hank-greens-focus-friend-is-google-plays-app-of-the-year/,
  https://focusfriend.me/, https://play.google.com/store/apps/details?id=com.underthing.focus.friend
  Weaknesses: bug complaints (~12.5% of reviews → UNVERIFIED figure), content
  depth runs out ("decorating goes by way too fast").
  → **ADOPT** disappointment-not-punishment (creature is *doing something*
  during rest; interruption pauses, does not destroy); **ADAPT**
  artifact-production loop; plan unlock depth from day one.
- **tama96** (desktop/terminal tamagotchi, PH April 2026) — validates
  persistent-visible-creature; our equivalent is widget/wallpaper, not a port.
  https://www.producthunt.com/products/tama96-desktop-terminal-ai-pet
- **Urso: Self Care Virtual Pet** — care upgrades a cozy home; fits den
  concept. https://apps.apple.com/us/app/urso-self-care-virtual-pet/id6450211570
- **Focus Dog** — "the emotional response is to a relationship; relationships
  don't habituate the same way rewards do."
  https://focusdog.app/magazine/tamagotchi-effect-virtual-pets-and-focus/
  → **ADOPT** framing: Anima sells a relationship, not a reward schedule.
- Cute-pomodoro cluster (My Little Pomodoro, Pomocat, ChickFocus) →
  **REJECT** competing as "a cute timer"; saturated.
- Macro-trend: hardware Tamagotchi passed 100M lifetime sales Aug 2025;
  buyers skew adult 25-45.
  https://sherwood.news/culture/tamagotchis-are-making-a-comeback/
  → timing evidence: the grown-up-virtual-pet demographic exists and overlaps
  with privacy-conscious users.

## §2 Digital wellbeing positioning

Verbatim incumbent positioning (live pages, 2026-07-18):

1. Forest — "Spend your time well with Forest"; "Time well spent, not just
   saved". https://forestapp.cc/
2. one sec — "Cut your screen time in half"; "Rewire your brain to hate
   scrolling"; "Everything You Need to Break Up With Your Phone"; privacy
   footnote "Your usage data is yours, either offline or in a private cloud".
   https://one-sec.app/
3. Opal — "Attention on autopilot."; "5 to 6 hours. That's the average time
   you'll spend on your phone today"; **no privacy claims on the landing page
   at all**. https://opalapp.com/
4. Jomo — "The app that ends bad screen time"; "Your phone, your rules";
   "Your data stays private & safe". https://jomo.so/
5. ScreenZen — "Less screen time. No subscription."; "the only
   donation-supported screen time app". https://screenzen.co/
6. Focus Friend — "#1 Focus App & Google Play App of the Year".
   https://focusfriend.me/

Pattern: the category frames itself as **war against the phone** (break up,
rewire, block, reclaim, end) and functionally requires surveillance-adjacent
permissions (usage access / Screen Time API / accessibility); most quantify
the user's failure back at them. Privacy is a footnote or absent. None can
say "we physically cannot see what you do" — their core feature depends on
seeing it.

Anima's honest edge: no UsageStats permission, no network in the core app,
no telemetry, no account, no adversarial frame.

Three candidate positioning angles (for §8 store texts):
1. **"Your phone finally contains something that isn't watching you."** —
   anti-surveillance flip; proof point "check the permissions screen — it's
   empty."
2. **"Not another app that guilts you off your phone. A reason to smile when
   you pick it up."** — against the war-frame; rides the Finch/Focus Friend
   gentleness wave.
3. **"A creature that lives entirely in your phone. Nothing leaves it — no
   account, no cloud, no ads, ever."** — Tamagotchi-for-adults nostalgia ×
   structurally-credible offline claim.

## §3 Live wallpaper — battery discipline (feeds ADR-012)

Platform contract:
- `Engine.onVisibilityChanged(boolean)`: "It is very important that a
  wallpaper only use CPU while it is visible" (AOSP WallpaperService source:
  https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/main/core/java/android/service/wallpaper/WallpaperService.java;
  https://developer.android.com/reference/android/service/wallpaper/WallpaperService.Engine).
  Canonical guidance: "When invisible… a wallpaper must stop all activity" —
  https://android-developers.googleblog.com/2010/02/live-wallpapers.html
- `onSurfaceDestroyed` / `onDestroy`: stop rendering unconditionally; pattern
  confirmed in libgdx backend and vogella tutorial
  (https://github.com/libgdx/libgdx/blob/master/backends/gdx-backend-android/src/com/badlogic/gdx/backends/android/AndroidLiveWallpaperService.java,
  https://www.vogella.com/tutorials/AndroidLiveWallpaper/article.html).
- AOD/ambient: `onAmbientModeChanged` is **@SystemApi** — third-party
  wallpapers cannot draw on AOD; the engine is reported invisible in doze.
  Pixel "Ambient AOD" (16 QPR1+) is a system-generated blurred still, not our
  rendering (https://www.androidauthority.com/wallpaper-on-aod-android-16-qpr1-3563717/).
- OEM pitfall: some Samsung AOD paths historically failed to deliver
  `onVisibilityChanged(false)` → naive loops never sleep
  (https://github.com/libgdx/libgdx/issues/4985). Defense: gate every frame on
  `isVisible()` AND an episode deadline; never "loop until told to stop".
- Reference implementation: **Muzei** renders with RENDERMODE_WHEN_DIRTY and a
  visibility-gated `requestRender()` — steady state **0 fps**, frames only on
  state change
  (https://raw.githubusercontent.com/muzei/muzei/main/main/src/main/java/com/google/android/apps/muzei/MuzeiWallpaperService.kt,
  https://raw.githubusercontent.com/muzei/muzei/main/main/src/main/java/com/google/android/apps/muzei/render/RenderController.kt).
- `Surface.setFrameRate()` (API 30+) is a display-rate hint, not a limiter —
  cadence stays our responsibility
  (https://developer.android.com/media/optimize/performance/frame-rate).
  Android 15 ARR makes low-rate content cheaper for free on supporting
  hardware (https://source.android.com/docs/core/graphics/arr).
- Battery numbers: no rigorous published measurements; blog-grade figures
  (~1-3% light / 5-10% heavy) are **UNVERIFIED**. The verifiable physics: a
  0 fps steady-state wallpaper does no GPU work between changes; panel cost
  equals a static wallpaper; overhead is the resident service process (RAM).
- Android 16: `WallpaperDescription`/`WallpaperInstance` allow multiple picker
  instances from one service (future: per-concept entries)
  (https://developer.android.com/about/versions/16/features).

## §4 Lock screen widgets, QS tiles, App Shortcuts

- **Lock screen widgets**: tablets Android 15 QPR1; phones **Android 16
  QPR2** (Pixel stable Dec 2025). Developer surface = ordinary AppWidgets
  ("same requirements as any other widgets"); opt-out via `not_keyguard`
  category; tap-through requires auth or `showWhenLocked`.
  https://android-developers.googleblog.com/2025/03/widgets-on-lock-screen-faq.html,
  https://www.androidauthority.com/lock-screen-widgets-on-phones-android-16-qpr2-3589668/,
  https://9to5google.com/2025/12/02/android-16-qpr2-pixel/
  One UI: 7 is first-party-only; One UI 8 third-party support **UNVERIFIED**
  (https://www.sammobile.com/news/one-ui-8-bring-third-party-widgets-lock-screen/).
- **QS tiles**: TileService; active-mode recommended; guidance explicitly says
  "Avoid using tiles that display information, but aren't interactive… Avoid
  using tiles to launch an app. Use an app shortcut."
  https://developer.android.com/develop/ui/views/quicksettings-tiles
  → a "mood display" tile is the named anti-pattern. A tile is legitimate
  only as an *action* — e.g. start/stop a rest session.
  API 33+ `requestAddTileService()` prompt available.
- **App Shortcuts**: launchers show up to 4; per-device publish limit via
  `getMaxShortcutCountPerActivity()`; dynamic shortcuts may change icons at
  runtime (mood glyph legal). Cheapest surface (XML + optional runtime calls).
  https://developer.android.com/develop/ui/views/launch/shortcuts

## §5 Wear OS

- Complications/tiles are services that run **on the watch** — a Wear app
  module is mandatory
  (https://developer.android.com/training/wearables/complications/exposing-data,
  https://developer.android.com/training/wearables/tiles/get_started).
- Own watch faces are foreclosed: new faces must be declarative Watch Face
  Format; legacy AndroidX/WSL faces uninstallable from Play since
  **2026-01-14**
  (https://android-developers.googleblog.com/2025/06/upcoming-changes-to-wear-os-watch-faces.html).
- Phone↔watch sync = Wearable Data Layer = `play-services-wearable` on the
  phone — a third privileged Google dependency in a deliberately offline app
  (https://developer.android.com/training/wearables/data/data-layer).
- Honest bill: 1 new Wear module + phone-side data layer, 3-4 new API
  surfaces, separate Play track/QA, est. ≥2-4 dev-weeks to a static
  complication (estimate derived from the verified inventory, not external —
  UNVERIFIED as a figure). Cheap path (watch-local creature, no sync) breaks
  the single-creature premise.

## §6 System TTS (feeds ADR-013)

API contract for offline guarantee:
- `Voice.isNetworkConnectionRequired()` is the modern per-voice check;
  `KEY_FEATURE_NETWORK_SYNTHESIS` is deprecated in favor of Voice-based
  selection
  (https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/speech/tts/Voice.java,
  https://developer.android.com/reference/android/speech/tts/TextToSpeech.Engine#KEY_FEATURE_NETWORK_SYNTHESIS).
- The embedded-synthesis contract is the platform's clearest privacy
  statement: "the engine must synthesize text on-device (without making
  network requests)"
  (https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/speech/tts/TextToSpeech.java).
  Network voices are by definition network-based synthesis (latency constants
  documented as network round-trips) → utterance text leaves the device. An
  explicit official sentence "network voices send text to Google servers" was
  NOT found → UNVERIFIED as a quote; the engineering conclusion stands on the
  documented contract. **Rule: only voices with
  `isNetworkConnectionRequired() == false`, never the engine default voice
  blindly; no offline voice → creature stays mute + deep-link to TTS
  settings.**
- Google engine: offline voices are per-language downloadable packs, managed
  by the USER in system settings (we cannot download them)
  (https://support.google.com/accessibility/android/answer/6006983?hl=en);
  ~90 languages incl. RU/PL/DE/ES/JA (secondary source, Play listing
  JS-rendered). Samsung TTS: install flow documented, offline guarantee for
  its own engine UNVERIFIED — the Voice filter, not engine identity, is our
  guarantee (https://www.samsung.com/us/support/answer/ANS10003701/).
- Character control: `setPitch`/`setSpeechRate` (1.0 normal; 0.5/2.0
  documented examples; engine clamps UNVERIFIED), set before each `speak()`;
  per-utterance `KEY_PARAM_VOLUME`/`KEY_PARAM_PAN`. Multiple offline voice
  variants per locale + pitch/rate give 8 distinguishable characters in EN;
  single-voice locales rely on pitch/rate alone (perceptual distinctness =
  design judgment, UNVERIFIED as guarantee).
- Animation: `UtteranceProgressListener.onStart/onDone` as the baseline
  mouth/glow envelope; `onRangeStart` (API 26+) only if the engine supplies
  timing — progressive enhancement
  (https://developer.android.com/reference/android/speech/tts/UtteranceProgressListener).

## §7 Localization

Market + competitor evidence (StatCounter June 2026; App Store info pages):
- Android share: DE 60.9%, PL 69.1%, RU 66.0%, ES 68.5%, **JP 41.3%
  (iOS-majority)** (https://gs.statcounter.com/os-market-share/mobile/germany
  et al.).
- **Finch is English-only** — the category leader ships zero localization
  (https://apps.apple.com/us/app/finch-self-care-widget-pet/id1528595748);
  Forest 14 languages (no PL), Widgetable 15 (no PL) → PL is a real gap.
- RU: Play billing paused since 2022 — reach and goodwill, not revenue
  (https://support.google.com/googleplay/android-developer/answer/11950272).
- DE privacy culture: 78.4% take active data-protection measures; 49.7%
  restrict app permissions (eco survey, 5 years GDPR)
  (https://international.eco.de/presse/eco-survey-on-5-years-of-the-gdpr-large-majority-of-germans-protect-their-personal-data-online/).
- JA: Tamagotchi homeland (Bandai 1996, kawaii-centered design)
  (https://en.wikipedia.org/wiki/Tamagotchi) — highest concept affinity,
  highest polish bar, iOS-majority market.

Gemma 3 1B multilingual — honest verdict:
- Model card says "multilingual support in over 140 languages" family-wide
  (https://ai.google.dev/gemma/docs/core/model_card_3), but the HF launch
  table is explicit: **1B: English; +140 languages: 4B/12B/27B**
  (https://huggingface.co/blog/gemma3), and the tech report's numbers settle
  it: 1B MGSM 2.04 (vs 74.3 for 27B), Global-MMLU-Lite 24.9 ≈ chance
  (https://arxiv.org/html/2503.19786v1). **The 1B tier is an
  English-primary model.**
- Gemini Nano (ML Kit GenAI): beta; validated-language list not directly
  fetchable (UNVERIFIED exact list); adjacent signals: Summarization
  EN/JA/KO (https://developers.google.com/ml-kit/genai/summarization/android),
  Chrome's Nano Prompt API accepts en/ja/es/de/fr
  (https://developer.chrome.com/docs/ai/prompt-api). **RU/PL are in no Nano
  list.**
- Consequence (feeds ADR-014): ship **UI localization + language-aware mind
  routing** — 1B tier answers in English regardless of UI locale (disclosed
  in-app), Nano tier may attempt DE/ES/JA, BYOK handles everything.

Recommended locale ranking: **EN** (baseline, only language the 1B mind
speaks) → **DE** (privacy audience + Nano-validated) → **ES** (two
continents, Nano-validated) → **RU** (Android reach, UI-only) → **PL**
(genuine competitor gap, UI-only) → **JA** (highest affinity, iOS-majority +
highest QA cost — last, native review required). FR noted as a future
candidate (in every Nano list, both competitors ship it).

Pseudolocale gate: enable `isPseudoLocalesEnabled = true` in debug, test
en-XA (expansion ~20-40%, catches hardcoded strings + concatenation) and
ar-XB (RTL mirroring) before commissioning translations
(https://developer.android.com/guide/topics/resources/pseudolocales).

## §8 Marketing / ASO

Live Play listing data (fetched 2026-07-18, US-EN listings; short descriptions
via meta-description tag — mapping UNVERIFIED as documented behavior):

- **Finch** (4.9★, 600K reviews, 10M+): title `Finch: Self-Care Pet`; short
  desc is a bare keyword list (`Mood journal, habits, self-care tracker,
  self-love`). Makes mental-health claims we cannot copy. Data safety: "may
  share Personal info, Financial info and 3 others" — contrast fuel.
  https://play.google.com/store/apps/details?id=com.finch.finch
  Review demand gap: "The ad claimed it was like a tamagotchi, but it isn't
  really" — users want more creature, less checklist.
- **Forest** (4.5★, 810K, 10M+): title at exactly 30 chars; keyword-dense
  benefit sentence (pomodoro, ADHD, phone addiction). Collects Location + 5
  others. https://play.google.com/store/apps/details?id=cc.forestapp
- **Widgetable** (4.7★, 531K, 50M+): audience-keyword title ("Besties &
  Couples"), 80/80-char short desc; ad complaints in reviews.
  https://play.google.com/store/apps/details?id=com.widgetable.theme.android
- **Pou** (4.3★, 11.4M, 1B+): Play surfaces an **Offline tag** on pet games;
  Data safety literally says "Data isn't encrypted". Review pattern: pet
  sick/hungry overnight → validates "never dies, never guilts".
  https://play.google.com/store/apps/details?id=me.pou.app
- **My Talking Tom** (4.2★, 17.4M, 1B+): description dominated by mandatory
  ad/IAP disclosures. https://play.google.com/store/apps/details?id=com.outfit7.mytalkingtomfree
- **one sec** (4.3★, 44K, 1M+): the precedent — **"No data collected / No
  data shared"** label + "all data remains offline" in the description.
  https://play.google.com/store/apps/details?id=wtf.riedel.onesec
- **Opal**: claims "Your data never leaves your device" in copy while its
  Data safety shows Location sharing + "Data isn't encrypted" — the gap
  Anima's label can close honestly.
  https://play.google.com/store/apps/details?id=com.withopal.opal

Verified limits/rules: title 30 / short 80 / full 4000
(https://support.google.com/googleplay/android-developer/answer/9859152);
metadata policy bans emojis-in-title, ALL CAPS, "#1/best"
(https://support.google.com/googleplay/android-developer/answer/9898842);
no keyword field on Play — title > short desc > long desc indexing, ~3-5
natural repeats (https://www.apptweak.com/en/aso-blog/play-store-keyword-research).
"Offline/no-wifi" is a high-volume evergreen query family (JindoBlu offline
games 92M downloads 2025 — https://mobilegamer.biz/the-top-mobile-game-downloads-of-2025/).
Do NOT use: anxiety/depression/ADHD/mental-health benefit claims (Play
Health Content scrutiny + our own constraint); "Tamagotchi" (Bandai
trademark); "#1/best" (banned).

Data safety: "Collect" = "transmitting data from your app off a user's
device"; ephemeral in-memory processing and user-initiated expected sharing
are exempt from parts of disclosure
(https://support.google.com/googleplay/android-developer/answer/10787469).
BYOK verdict: **Reading A (conservative, recommended)** — declare Messages →
Other in-app messages, Collected=Yes, Shared=No (user-initiated to user's own
provider), Ephemeral=Yes, Optional=Yes, purpose App functionality; everything
else Not collected. Reading B ("no collection" via the user's-own-account
exemption) is plausible but has no published ruling → UNVERIFIED. Privacy
policy URL is mandatory even with zero collection
(https://support.google.com/googleplay/android-developer/answer/9859455).
Tone exemplars: Fossify per-app one-pagers
(https://www.fossify.org/policy/notes/), one sec
(https://one-sec.app/privacy/).

Screenshots: min 2, max 8/device type; featuring needs ≥4 at ≥1080px; text
≤20% of image; first ~3 portrait frames visible without scrolling
(https://support.google.com/googleplay/android-developer/answer/9866151,
https://asomobile.net/en/blog/screenshots-for-app-store-and-google-play-in-2025-a-complete-guide/
— scroll/conversion figures UNVERIFIED beyond the source). The 6-frame plan,
title/short-desc variants and the complete Data safety draft live in
docs/store/ (store-listing-v4.md, data-safety-form.md,
screenshot-scenario.md) and docs/privacy-policy.md.

Full UNVERIFIED list for §6-§8 mirrored from the research tracks: network-TTS
"text leaves device" as explicit official sentence; Samsung TTS offline
guarantee; pitch/rate engine clamps; 8-character distinctness in single-voice
locales; ML Kit Prompt API exact language list; Sensor Tower Finch estimates;
competitor screenshot visuals (not rendered); BYOK Reading B; ASO scroll
percentages; Play search volumes; meta-tag↔short-desc mapping; manual char
counts (re-verify in Console).

---

## UNVERIFIED (consolidated §1-§5)

- Finch streak-repair economy details (help-center page 403'd; wording from
  search index).
- Widgetable neglect mechanics + ad-food quotes (review aggregators blocked
  direct fetch).
- Forest feature-removal/subscription complaints (aggregator fetch blocked;
  corroborated by two independent snippets).
- Focus Friend "~12.5% of reviews mention bugs" (search snippet only).
- iOS-widget "static snapshot" doc page returned no body (standard platform
  knowledge, uncited).
- OFFTIME brand status in 2026 (no maintained page found).
- Which "Mellow" the roadmap meant (primary candidate documented with URL).
- Live-wallpaper battery percentages (blog-grade only).
- One UI 8 third-party lock screen widgets (only "expected" from spring-2025
  sources).
- Pixel "Ambient AOD" device gating (pre-release code analysis).
- Wear ~2-4 dev-weeks figure (own derivation).
- Exact shortcut publish limit "15" (docs now say "varies").
- WallpaperService.Engine javadoc quoted from AOSP source + docs mirror
  (live reference page renders client-side; wording matched across both).

---

## §9 Battery health facts (feeds Phase 2.2 "care as creature care")

Verified 2026-07-18 (dedicated research track). Key findings, each mapped to
what the creature may honestly say:

- **20-80% is a continuum, not a cliff.** BU-808 tables: 100% DoD ≈ 300
  cycles vs 20% DoD ≈ 2,000; charge-voltage table shows every 70 mV drop
  costs ~10% capacity but multiplies cycle life
  (https://www.batteryuniversity.com/article/bu-808-how-to-prolong-lithium-based-batteries/).
  Keil et al. 2016: degradation vs SoC is non-monotonic with plateaus; the
  dominant driver is TIME AT HIGH SoC
  (https://iopscience.iop.org/article/10.1149/2.0411609jes).
  → creature expresses comfort in the middle, never a hard threshold.
- **Overnight ≠ overcharge, but 100% dwell is real stress.** Samsung: "Keeping
  your battery at a full 100% charge for a long time can reduce its
  lifespan"; One UI Battery protection modes Basic/Adaptive/Maximum(80%)
  (https://www.samsung.com/sg/support/mobile-devices/galaxy-battery-protection-feature-in-one-ui-6-1/).
  Pixel: Adaptive Charging + "Limit to 80%" (full charge every ~10th cycle
  for gauge calibration)
  (https://support.google.com/pixelphone/answer/6090612?hl=en).
  → recommend the OS feature BY NAME; never reimplement it.
- **Deep discharge**: phone-0% occasionally = mild (BMS stops well above
  damage); habitual deep cycling burns cycle life; true over-discharge in
  storage causes copper dissolution/dendrites
  (https://calce.umd.edu/news/story/understanding-the-nature-of-copper-dissolution-in-overdischarged-lithiumion-batteries).
  Samsung: recharge at 15-20%, store at ≥50%
  (https://www.samsung.com/us/support/galaxy-battery/care-and-maintenance/).
- **Heat is enemy #1**, especially heat + full charge (BU-808 storage table:
  a year at 100%/40°C → ~65% retained). Samsung operating range 0-35°C.
  → the top-priority advice line.
- **Fast charging**: cell-level harm is heat-mediated; modern flagships
  engineer around it — do not demonize wattage, tie advice to observed
  temperature.
- **Top-ups beat full cycles**: no memory effect; micro-cycling roughly
  doubled equivalent-full-cycle lifetime in experiment
  (https://www.sciencedirect.com/science/article/pii/S2352152X2201338X);
  occasional full charge keeps the fuel gauge calibrated.
- **Myths banned from the app**: memory effect, "drain fully", "unplug at
  exactly 80% or damage", overnight-explosion fear, and ANY numeric health
  percentage (Android 14's true SoH API needs BATTERY_STATS — ungrantable;
  ΔCHARGE_COUNTER/Δ% estimation is ±5% at best and stays out of v0.4).
- UNVERIFIED: Pixel 80% "bypass charging" behavior; exact "doubles per
  +10°C" multiplier; S24 populating EXTRA_CYCLE_COUNT for third-party apps
  (gate on runtime value if ever used).

CareAnalyzer (core/model/Care.kt) encodes exactly these rules; advice
strings live in the diary and speak in preferences, never doom.
