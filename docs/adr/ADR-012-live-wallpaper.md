# ADR-012: Creature as live wallpaper — approved, with a hard budget

Status: accepted, 2026-07-18.

## Findings (research-v4 §3)

- The platform contract is explicit: "a wallpaper must only use CPU while it
  is visible"; well-behaved engines halt in `onVisibilityChanged(false)`,
  `onSurfaceDestroyed` and `onDestroy`.
- Muzei (the reference battery-friendly wallpaper) renders WHEN_DIRTY with a
  visibility-gated `requestRender()`: steady state is **0 fps**, frames only
  on state change. A 0 fps steady-state wallpaper does no GPU work between
  changes; panel cost equals a static wallpaper.
- AOD is closed to us (`onAmbientModeChanged` is @SystemApi) — the engine is
  invisible during doze, full stop applies.
- Known OEM pitfall: Samsung AOD paths have historically missed
  `onVisibilityChanged(false)` (libgdx #4985) — frames must be gated on
  `isVisible()` AND an episode deadline, never "loop until told to stop".
- Published battery percentages are blog-grade (UNVERIFIED); the physics and
  the Muzei precedent are the evidence.

## Decision

Live wallpaper ships in v0.4 as the **single sanctioned exception** to the
"animation only on the visible app screen" invariant, under this budget,
enforced in code (`WallpaperBudget`):

- **Steady state: 0 fps.** The creature is a static pose derived from the
  current body/mood state. No timer-driven animation, ever.
- **Redraw triggers: state changes only** (mood/stage/charging transitions,
  config/surface changes) — deterministic, debounced to ≥1 per 30 s except
  surface/config events.
- **Transition episodes: ≤ 2.5 s at ≤ 30 fps** (every-other-vsync via
  Choreographer), each episode carries a hard deadline; the loop exits on
  deadline even if `onVisibilityChanged(false)` never arrives.
- **Invisible = dead stop**: visibility false, surface destroyed, or engine
  destroyed cancels the episode scope immediately.
- `Surface.setFrameRate(LOW)` hinted on API 30+ so ARR displays can downclock.
- No wake locks, no WorkManager, no sensors in the wallpaper process path;
  it reads the same persisted body state the widget reads.

S24 checklist §0b measures a real night of battery with the wallpaper set
(the honest test no desk measurement replaces).
