# ADR-007: Home-screen widget — static snapshot, honest triggers

Status: accepted, 2026-07-17.

## Decision

`:feature:widget` (Glance 1.2.0-rc01) shows ONE static frame of the real
rig: `WidgetSnapshot` builds a throwaway `CreatureEngine`, settles it in
reduced-motion statics, advances one 16 ms tick, renders the concept's
renderer into a 512 px `ImageBitmap` via `CanvasDrawScope`, and hands the
bitmap to Glance `ImageProvider`. No frame loop, no inference, no network in
any widget path. Tap = app launch intent.

Bitmap budget: RemoteViews caps total bitmap memory at ~1.5 × screen
(w×h×4×1.5); one 512² ARGB bitmap ≈ 1 MB — far inside (research-v2 §B.1).

## The trigger reality (original plan was impossible)

The brief asked for manifest receivers on ACTION_BATTERY_LOW / OKAY /
POWER_CONNECTED / DISCONNECTED. **None of these are on the implicit-broadcast
exemption list** — manifest receivers get none of them on API 26+
(research-v2 §B.1; the old training-page XML sample is legacy). Verdict:
that mechanism is dead; replaced with what a dead process actually gets:

| Trigger | Mechanism | Latency |
|---|---|---|
| Heartbeat | `updatePeriodMillis = 1 800 000` (30 min, system floor) | ≤ 30 min |
| Charging started | WorkManager one-shot, `requiresCharging`, re-arms after firing | seconds |
| Battery recovered | WorkManager one-shot, `requiresBatteryNotLow`, re-arms | seconds–minutes |
| Battery went LOW | **no negative constraint exists** — waits for heartbeat | ≤ 30 min |
| App-side events | app process already alive; next heartbeat repaints | ≤ 30 min |

The battery-LOW gap is accepted and recorded: the alternative (a periodic
15-min WorkManager job) doubles scheduled wakeups for one cosmetic edge.

## Background-entity budget (DoD)

Added entities: the Glance `AnimaWidgetReceiver` + WorkManager's
infrastructure driving two parked constraint one-shots. Both are "widget
triggers" under the DoD's "listener + widget triggers only" budget. Zero
services of our own; zero exact alarms; zero foreground services.

## Quality verdict

Glance-over-RemoteViews cannot animate the rig and cannot ship AGSL — the
static-snapshot design is not a cut-down compromise but the honest fit:
the creature on the home screen is a photo, not a cage. fps/thermals on S24
with the widget installed is a manual-checklist item.
