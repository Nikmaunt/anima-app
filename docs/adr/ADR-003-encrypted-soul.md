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

## Addendum (v0.2, 2026-07-17): passphrase zeroing — what is actually true

The original text claimed the passphrase is "zeroed after opening the DB".
The v0.1 audit (docs/audit-v01.md) found no zeroing anywhere in app code.
Investigating the library itself (bytecode of `net.zetetic:sqlcipher-android`
4.6.1, since secondary sources contradicted each other):

- `SupportOpenHelperFactory` **retains the exact `byte[]` reference** and
  never scrubs it. There is **no `clearPassphrase` parameter** in this
  library — the boolean ctor argument is `enableWriteAheadLogging` (the
  deprecated `android-database-sqlcipher` had clearing; its successor
  dropped it).
- Therefore in v0.1 the passphrase lived in the Java heap, unscrubbed, for
  the whole process lifetime. The ADR overclaimed.

v0.2 mechanism (explicit, ours):

- `SoulKeyHolder` (core:data) is the single custodian of the plaintext array;
  the same reference goes into the factory, so scrubbing the holder's array
  also scrubs the factory's retained copy.
- `AnimaApp.onCreate` eagerly opens the DB on an IO coroutine and then calls
  `SoulKeyHolder.zero()` — plaintext window is startup-seconds, not
  process-lifetime. A hypothetical re-open after zeroing fails loudly (wrong
  key) instead of silently working with a scrubbed array; accepted, because
  the Room singleton holds its connection for the process lifetime.

**Accepted limitation, on the record:** SQLCipher's native side derives and
holds key material for open connections; there is no API to scrub those
native copies short of closing the database. The threat model is at-rest
disk compromise, not a same-process memory reader — a process that can read
our heap can read the open DB anyway.
