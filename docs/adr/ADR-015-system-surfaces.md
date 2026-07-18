# ADR-015: System surfaces — QS tile as action, shortcuts yes, lock screen free-ride, Wear rejected

Status: accepted, 2026-07-18.

## Findings (research-v4 §4-§5)

- QS tile guidance explicitly names our naive plan as an anti-pattern:
  "Avoid using tiles that display information, but aren't interactive…
  Avoid using tiles to launch an app. Use an app shortcut."
- Lock screen widgets on phones are ordinary AppWidgets surfaced by Android
  16 QPR2 (Pixel stable Dec 2025); no dedicated API, opt-out via
  `not_keyguard`; One UI third-party support UNVERIFIED.
- App Shortcuts: up to 4 shown by launchers; dynamic shortcuts may carry a
  mood glyph in the icon; cheapest surface in the set.
- Wear OS: a watch surface requires a Wear app module (complications/tiles
  run on the watch), Watch Face Format forecloses a custom face
  (legacy faces uninstallable from Play since 2026-01-14), and phone↔watch
  sync drags `play-services-wearable` — a third privileged Google
  dependency in a deliberately offline app. Honest bill: ≥2-4 dev-weeks +
  a second release track for a cosmetic glance.

## Decision

- **QS tile ships as an ACTION, not a mood display**: "Rest together" —
  tap starts/stops a rest session (opens the rest screen with one tap-in;
  active session shows remaining time in the tile subtitle). Active-mode
  TileService; process runs only for tile events. The mood-display tile
  from the original brief is rejected per platform guidance.
- **App Shortcuts ship**: static shortcuts Talk / Diary / Rest (3 of 4
  slots, room for one dynamic later). No dynamic mood icon in v0.4 —
  deterministic, testable statics first.
- **Lock screen widget: free-ride, no dedicated project.** The existing
  Glance widget stays keyguard-eligible (no `not_keyguard` opt-out), gets
  verified at hub sizes; nothing else is built for it in v0.4.
- **Wear OS: rejected** for v0.4+ on the numbers above; revisit only on
  real user demand. The watch-local-creature cheap path breaks the
  single-creature premise and is also rejected.
