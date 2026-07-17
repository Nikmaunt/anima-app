# Anima Motion Bible

The contract every concept implements. All numbers trace to research.md §B2.
All "life" derives from deterministic oscillators + seeded value noise driven
by ONE frame clock (`withFrameNanos`). No timers outside the frame loop, no
randomness at render time (noise fields are functions of time + seed), no LLM
anywhere near motion.

## The clock

- One `withFrameNanos` loop per creature surface, started only in RESUMED,
  cancelled on leave. Delta-time capped at 100 ms (background→foreground jump
  safety). Time state is read exclusively in draw lambdas.
- Reduced motion → the loop runs a **calm-statics schedule**: a static settled
  pose re-drawn only on blink ticks (every few seconds) and on state changes.
  No continuous motion at all.
- Sleep/bored states self-throttle: the engine skips internal ticks to ~30 Hz
  (frame-skip), imperceptible on slow breathing but halves work.

## The rig (shared by every concept)

Every concept renders from the same `CreaturePose`:

| Channel | Source oscillator |
|---|---|
| breath 0..1 | sine, inhale 60% / exhale 40% of the cycle (asymmetric ease) |
| blink 0..1 per eye | scheduled impulses; closure ~120 ms in, ~180 ms out |
| gazeX/gazeY -1..1 | spring-tracked target: touch point → drift target from noise |
| bodyOffset (x,y) | 2-octave value noise, amplitude per mood |
| squash 0..1 | spring on interactions (volume-preserving scaleX=1/scaleY) |
| tilt deg | slow noise + follow-through after gaze/pounce |
| energy 0..1 | mood-driven master gain on every amplitude |
| secondary[] | chained springs (ears/tentacles/leaves/wings), each link targets its predecessor |

## The schedule of life

**Every second (continuous):**
- Breathing. Calm 12/min (5 s cycle) · asleep 8/min · anxious/hot 22–26/min
  shallow · eating 14/min with a swallow accent every 4th cycle.
- Idle drift: body noise wander, ±1.5% of body size (alert), ±0.4% (asleep).

**Every few seconds (scheduled impulses, randomized by seed+time hash, never
on loop boundaries):**
- Blink: mean interval 3.5 s × genome.blinkRateScale, uniform 2–6 s; 10%
  double-blink; blink 100–300 ms. Focused states (chat "listening") drop to
  ~1/4 rate; asleep = eyes closed (no blink schedule).
- Eye dart: every 2–5 s while ALERT; dart travel in ~100 ms, 80% in the first
  step, micro-settle after. Frequency doubles when ANXIOUS, halves when BORED.

**Every ~10 seconds:**
- Weight shift / posture change: 8–12 s period, one slow ease (tilt + offset
  re-center). While BORED: an audible-looking sigh (one deep breath at 1.6×
  amplitude). While EATING: a contented wiggle after each "swallow" run.

**Every ~minute:**
- A rare "big" idle: look around (two darts + head turn), stretch (single
  squash-stretch arc), or concept-specific flourish (fox ear-flick, jelly
  tentacle ripple, robot eye-reboot, sprout leaf unfurl, ember spark, moth
  wing shimmer, orb satellite orbit swap, pixel-pet frame dance). Chosen by
  seeded hash of the minute index — deterministic, no RNG at runtime.

**On events (spring impulses, interruptible):**
- Tap: anticipation dip (~80 ms) → squash → spring back; haptic CONFIRM-class
  tick. A tap while ASLEEP: one eye opens halfway, then re-sleep.
- Pet (drag): lean INTO the finger (Nintendogs lesson), eyes soften/close,
  purr haptic = PRIMITIVE_LOW_TICK train at 40–60 ms spacing, sinusoidal
  scale 0.2–0.4 (only if `arePrimitivesSupported`); fallback: nothing rather
  than a wrong-feeling buzz.
- Typing (chat): creature perks up, gaze to the input line, small lean.
- Thinking (generation): rhythmic sway + slightly faster breath — the body
  thinks; no spinner chrome.
- Charge plug-in: happy bounce (2 springs), then EATING posture.
- Notification storm: startle (one sharp contraction), then ANXIOUS.

## Mood → parameter table (engine-level, concepts inherit)

| Mood | energy | breath | gaze | notes |
|---|---|---|---|---|
| ALERT | 1.0 | 12/min | active darts | default |
| BORED | 0.55 | 10/min | slow, droopy lids 30% | sighs every ~10 s |
| SLEEPY | 0.35 | 9/min | lids 60%, gaze low | dimmed palette |
| EATING | 0.9 | 14/min + swallow | content, half-lids | charge shimmer |
| ANXIOUS | 1.2 | 24/min shallow | rapid darts | jitter noise +60% |
| ASLEEP | 0.2 | 8/min deep | closed | curl pose; tap rouses 4 s |
| HOT | 0.7 | 26/min pant | squint | flush tint, droop, "leave me be" |

## Discipline (hermes-lens rules, carried over)

- Finite episodes: every reaction runs to equilibrium and stops feeding the
  spring system; the ambient loop is the only continuous consumer, and it
  stops with the lifecycle.
- Wall-clock fixed substeps (16.7 ms, max 4) — a throttled frame rate plays
  the same trajectory, just chunkier.
- `lastT = null` on resume — no dt spikes.
- Pure engine / impure driver: the oscillator+spring core imports no Android
  classes; the Compose layer owns scheduling and drawing only.
- Zero allocations per frame: poses and paths are reused holders.
