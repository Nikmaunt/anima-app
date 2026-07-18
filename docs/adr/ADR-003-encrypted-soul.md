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

## Addendum (v0.4, 2026-07-18): the zeroing ritual was unsound — removed

The v0.2 mechanism rested on the assumption that "the Room singleton holds
its connection for the process lifetime" — i.e. exactly one physical
connection. That assumption is false under WAL (Room's default journal
mode): `SQLiteConnectionPool` opens **non-primary connections on demand**
under concurrent load, and 4.17.0 bytecode (audit-v03 §1) shows every
physical open re-reads `configuration.password` — the very array we had
zeroed. Result observed live in the v0.4 emulator pass, minutes after a
fresh onboarding: `SQLiteNotADatabaseException: file is not a database` in
`tryAcquireNonPrimaryConnectionLocked` → process death. The v0.2/v0.3
"plaintext window is startup-seconds" claim bought a crash, not a security
property.

v0.4 posture:

- `SoulKeyHolder` keeps the passphrase for the process lifetime; nothing
  scrubs it while the pool lives. `SoulVaultWarmer.warmUp()` remains as the
  fail-fast for corrupt `soul.key`.
- Pinned by `WalPoolKeyDeviceTest` (core:data androidTest): a held write
  transaction plus a concurrent read forces a non-primary connection; the
  read must succeed.
- The honest security statement: at rest the passphrase exists only
  Keystore-wrapped; in memory it lives as long as the process, same as the
  native key material it feeds. The threat model (at-rest compromise, not
  same-process memory readers) is unchanged — see the accepted limitation
  above, which now covers the Java copy too.

## Note (v0.4): the one bulk write path into the soul

Sealed-backup restore (`SoulBackup.importPayload`) inserts every fact from
the envelope after a single file+passphrase act — the sanctioned exception
to per-fact confirmation (audit-v03 F9): restoring one's own soul IS the
consent. A crafted backup a user chooses to import can inject arbitrary
memories; accepted, since the same user can type arbitrary memories.
