# Anima

A creature lives in your phone. It *is* your phone.

Anima is a standalone Android app: an on-screen companion whose body reflects the
real state of the device (battery, charging, storage, connectivity, thermal),
who talks via **on-device-only** LLM inference (Gemini Nano / ML Kit GenAI),
remembers everything **strictly locally** in an encrypted database, and whose
"soul" exports as a single file.

**No server. No telemetry. No accounts. No network calls at all.**

## Hard guarantees

| Guarantee | Mechanism |
|---|---|
| Zero network | No `INTERNET` permission in the merged manifest; grep-test in CI |
| On-device inference only | ML Kit GenAI (AICore / Gemini Nano); honest offline mode when unavailable |
| Local encrypted storage | SQLCipher-encrypted SQLite, key held in Android Keystore |
| Minimal permissions | Only Notification Access, strictly opt-in |
| One background entity | `NotificationListenerService` that only writes events to the local DB |
| Animation discipline | Runs only while visible & screen on; reduced-motion → calm statics |
| Untrusted text is never a command | Facts enter the soul only after explicit user confirmation in UI |

## Structure

See [docs/](docs/) — research, ADRs, motion bible, handoff notes.

## Build

```
./gradlew assembleDebug
./gradlew test
```
