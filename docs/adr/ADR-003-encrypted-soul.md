# ADR-003: Soul storage — SQLCipher Room + Keystore-wrapped passphrase

Status: accepted, 2026-07-17.

## Decision

- Single Room database ("soul.db") encrypted with **SQLCipher for Android**
  (`net.zetetic:sqlcipher-android`), wired via `SupportOpenHelperFactory`.
- Key material: a 32-byte random passphrase generated once with
  `SecureRandom`, encrypted (AES-GCM) by a non-exportable key in
  **Android Keystore** (`KeyGenParameterSpec`, purpose ENCRYPT/DECRYPT,
  no user-auth requirement — the creature must live without biometrics),
  stored wrapped in app-private storage. Decrypted only in memory, zeroed
  after opening the DB.
- Schema follows hermes-app conventions: string PKs (`prefix-hex`),
  epoch-millis UTC, enums as wire strings with safe read fallback,
  **soul_facts append-only + supersede/forget** (no `@Delete`, no full-row
  `@Update` in the DAO type — structurally enforced), exportSchema=true with
  committed schema JSON.
- Backup posture: `allowBackup=false` + `dataExtractionRules` excluding all
  domains for both cloud-backup and device-transfer. The soul leaves the device
  only through the explicit export feature.

## Why

hermes-app was named as the encrypted-SQLite donor, but read-only inspection
proved it has **no encryption implemented** (plain Room + backup exclusion;
Keystore work deferred to its M1). Anima's constraint #3 is non-negotiable, so
we implement the standard SQLCipher+Keystore pattern fresh. SQLCipher is chosen
over androidx.security EncryptedFile-style approaches because Room needs a
`SupportSQLiteOpenHelper` factory, and over passphrase-derived-from-Keystore-id
hacks because a wrapped random passphrase survives Keystore key rotation
design changes and is the documented industry pattern.

## Consequences

- DAO tests run under Robolectric against **plain Room in-memory** (the DAO
  layer is factory-agnostic). Robolectric can load neither the SQLCipher
  native library nor AndroidKeyStore, so the encrypted open path
  (SupportOpenHelperFactory + Keystore unwrap) is verified on-device — it is
  an explicit item in the manual S24 checklist.
- Losing the Keystore key (factory reset without export) loses the soul —
  by design; the product's answer is the one-file soul export.
