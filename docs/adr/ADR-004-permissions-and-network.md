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
