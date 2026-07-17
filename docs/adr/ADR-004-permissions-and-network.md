# ADR-004: Permission & network posture

Status: accepted, 2026-07-17.

## Decision

Merged-manifest permission budget, in full:

| Entry | Kind | Why |
|---|---|---|
| `ACCESS_NETWORK_STATE` | normal, no dialog | ConnectivityManager transport kind = the creature's "hearing" (online/wifi/cellular). Never SSID (location-gated — untouched). |
| `VIBRATE` | normal, no dialog | `VibrationEffect` compositions for petting/purr haptics. |
| `POST_NOTIFICATIONS` | **absent** | Anima posts zero notifications, ever. |
| `INTERNET` | **absent** | The guarantee. Its absence in the merged manifest is asserted by an automated test; no network stack exists in the dependency graph. |
| Notification Access (`BIND_NOTIFICATION_LISTENER_SERVICE` on the service) | special access, strict opt-in | The only sensitive capability; granted by the user on the system screen after an in-app explainer. Not a `uses-permission`. |

- The one background entity is the `NotificationListenerService`; it filters
  (allowlist → group-summary → ongoing → OTP) and writes to the local encrypted
  DB. No inference, no network, no UI, no wake locks.
- "Разрешения: только Notification Access" is interpreted as: **no
  runtime-prompt (dangerous) permissions and no special-access permissions
  except Notification Access.** Normal install-time permissions that never
  show a user dialog and cannot exfiltrate data (`ACCESS_NETWORK_STATE`,
  `VIBRATE`) are in scope of the creature's senses. Without INTERNET,
  ACCESS_NETWORK_STATE is provably read-only sensing.

## Incident captured during build (kept for the audit trail)

The ML Kit GenAI client library transitively pulls Google **DataTransport
telemetry**, which injected `android.permission.INTERNET` plus two background
scheduler components (`JobInfoSchedulerService`,
`AlarmManagerSchedulerBroadcastReceiver`) into the merged manifest. Both are
now stripped at merge time with `tools:node="remove"` in the app manifest;
`NetworkIsolationTest` asserts against the packaged manifest so a library
update that re-introduces either fails the build. Inference is unaffected —
it runs in the separate AICore system process.

## Enforcement

- `NetworkIsolationTest` (JVM): greps the merged manifest report for
  `android.permission.INTERNET` and fails if present; greps all module sources
  for network classes (`java.net.Http`, `okhttp`, `retrofit`, `Socket(`,
  `URL.openConnection`).
- The version catalog contains no network coordinates; `:feature:notifications`
  has no dependency capable of network I/O (verified by its dependency list).
- DoD audit re-checks all of the above read-only.

## Amendment (v0.2, 2026-07-17): the single network exception + test v2

ADR-005 introduces `:core:model-delivery` — the one module allowed to touch
the network, for the one flow of fetching the mind's model file on explicit
user action. Changes against the original text of this ADR:

- The app manifest no longer strips INTERNET at merge; the permission now
  merges in from `core/model-delivery/src/main/AndroidManifest.xml` and from
  nowhere else. DataTransport telemetry components stay stripped.
- **Honest limitation:** Android permissions are app-wide; once merged, the
  OS lets the whole process open sockets. Per-module scoping is a build-time
  discipline, not an OS guarantee. NetworkIsolationTest v2 enforces it:
  INTERNET originates only from model-delivery's manifest; no source outside
  that module references network APIs; no build file in the repo carries a
  network-stack coordinate; the version catalog stays clean.
- The audit's two efficacy gaps are closed: the merged-manifest assertion
  now FAILS when no build output exists (and `:app` unit tests depend on the
  manifest-packaging task so the input always exists), and the dependency
  grep covers every `build.gradle.kts`, not just the catalog.
