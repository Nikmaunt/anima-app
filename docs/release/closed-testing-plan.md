# Closed testing plan (v0.6 → Play)

The path from this repo to a production listing on a PERSONAL developer
account (created after 2023-11-13). Facts verified 2026-07-19; sources in
docs/research-v6.md §B.

## 1. The rule that shapes everything

**12 testers, opted in continuously for the last 14 days**, at the moment
of applying for production access (research-v6 §B.2). Opt-out+in resets the
clock for that tester.

- What counts: testers who joined via the closed-track opt-in link and
  stayed opted in. Continuous means *consecutive days*.
- Plan for attrition: recruit **16–18 people** so 12 survive.
- What "activity" looks like to reviewers (Google evaluates engagement
  when granting production): install, open across several days, at least
  some of the flow (hatch → chat → rest). Ask testers honestly for a few
  minutes across the window, not a single install-and-forget.
- Aim the 14-day window to START only after the build is stable — a
  mid-window rollout of a broken build wastes the clock.

## 2. Sequence (owner checklist)

1. Play Console: create app → complete **identity verification** and
   **DSA trader declaration** (non-trader: no monetization) — these gate
   everything for new accounts (§B.11).
2. Store listing: name, short/full description (docs/store/
   store-listing-v5.md + v6 delta), screenshots (see §5), icon 512 px
   (export from the adaptive icon foreground on the night background),
   feature graphic 1024×500.
3. **Data safety form**: finalized draft in docs/store/data-safety-v04.md —
   verify two honesty points: (a) BYOK cloud mind = user-initiated data
   sharing to the user's own chosen provider — declare chat text as
   "shared, optional, user-initiated" for that feature; (b) everything
   else: no collection. The form must match the privacy policy.
4. **Privacy policy URL**: publish docs/privacy-policy.md as rendered HTML
   on GitHub Pages (public repo or a dedicated pages repo). Requirements:
   public, stable, not a PDF/editable doc (§B.3).
5. **Content rating (IARC)**: questionnaire — virtual pet/companion; no
   violence/sex; no user-to-user chat (the chat partner is local AI); no
   unrestricted web. Expect Everyone/PEGI 3.
6. **GenAI policy**: the AI-content report affordance ships in-app (v0.6,
   long-press a creature reply → "report"); answer the app-content
   GenAI declaration accordingly (§B.5).
7. Upload the signed AAB (upload key: docs/release/signing.md) to a
   **closed testing** track; add testers by email list or Google Group.
8. Run the 14-day window; watch pre-launch report + ANRs.
9. Apply for production; answer the "production readiness" questions with
   the closed-test evidence.

## 3. Build artifacts for the track

- `./gradlew :app:bundleRelease` — signed AAB (with ../anima-keys present).
- Fast-follow pack: ADR-018 decides what rides the `mind-pack` slot; the
  AAB is valid with or without the model file present in the pack dir
  (registry falls through to download/SAF paths).
- versionCode/Name: 6 / 0.6.0 (scheme in CHANGELOG.md).

## 4. Release notes (Play "What's new", 6 locales, <500 chars each)

- EN: "First closed test: your creature hatches, remembers what you allow,
  rests with you, speaks your language, and never touches the internet.
  New: letters to your future self, an evening farewell, open-source
  licenses screen."
- RU: "Первый закрытый тест: существо вылупляется, помнит только то, что
  ты разрешишь, отдыхает вместе с тобой, говорит на твоём языке и не
  выходит в интернет. Новое: письма себе в будущее, вечернее прощание,
  экран лицензий."
- PL: "Pierwszy test zamknięty: stworzenie wykluwa się, pamięta tylko to,
  na co pozwolisz, odpoczywa z tobą, mówi po polsku i nie łączy się z
  internetem. Nowość: listy do przyszłego siebie, wieczorne pożegnanie,
  ekran licencji."
- DE: "Erster geschlossener Test: dein Wesen schlüpft, merkt sich nur, was
  du erlaubst, ruht mit dir, spricht deine Sprache und geht nie ins
  Internet. Neu: Briefe an dein zukünftiges Ich, ein Abendabschied,
  Lizenzbildschirm."
- ES: "Primera prueba cerrada: tu criatura nace, recuerda solo lo que
  permites, descansa contigo, habla tu idioma y nunca toca internet.
  Nuevo: cartas a tu yo futuro, despedida nocturna, pantalla de licencias."
- JA: "初のクローズドテスト：あなたのいきものが生まれ、許可したことだけを
  おぼえ、いっしょに休み、日本語で話し、インターネットには決して
  つながりません。新機能：未来の自分への手紙、おやすみの挨拶、
  ライセンス画面。"

## 5. Screenshots

Scenario from v0.4 store package, now via the screenshot rig: run the
Roborazzi rig in the 6 locales for framed captures of (1) hatch,
(2) home+chat, (3) rest together, (4) diary, (5) soul with confirmation
sheet, (6) trust screen. The rig produces raw PNGs under
`core/ui/build/…/roborazzi`; frame in the store template. NOTE: goldens
are device-independent but Play wants ≥1080 px on the long side — export
at 2× when framing. (Owner step — needs Play listing frames choice.)

## 6. During the window

- Watch: pre-launch report crashes, ANR rate, vitals.
- Collect tester notes weekly; only ship fixes that don't reset trust
  (schema migrations must keep passing the upgrade suite).

## 7. Blockers before submission (live list, v0.6 run end)

- [ ] Privacy policy published to a public URL (owner: enable GitHub Pages).
- [ ] Play Console account verification + DSA declaration (owner).
- [ ] 512 px icon + feature graphic exported (owner or next run).
- [ ] Store screenshots captured in 6 locales (§5, next run or owner).
- [ ] ADR-018 outcome applied to the shipped pack (int4 artifact or
      empty-slot fallback).
- [ ] S24 checklist v6 §0e passed (signed release APK behaves under R8).
