# Signing — upload key (v0.6)

Anima uses **Play App Signing**: Google holds the app signing key; this repo
only ever sees the **upload key**. Losing the upload key is recoverable
(Play Console → request upload key reset); leaking it is an
inconvenience, not a catastrophe — the app signing key never leaves Google.

## Where the key lives

NOT in the repo. The build looks for
`../anima-keys/keystore.properties` (a sibling directory of the checkout),
overridable with `-PanimaKeystoreProps=<path>`:

```
D:/Hermes/
├── anima-app/          ← this repo
└── anima-keys/         ← never under version control
    ├── anima-upload.keystore
    └── keystore.properties
```

`keystore.properties` format:

```properties
storeFile=D\:/Hermes/anima-keys/anima-upload.keystore
storePassword=…
keyAlias=anima-upload
keyPassword=…
```

When the file exists, `assembleRelease` / `bundleRelease` come out signed
with the upload key. When it doesn't (CI, fresh clones), release artifacts
build **unsigned** — deliberate: CI has no business holding keys, and an
unsigned AAB still proves the build path.

## Owner checklist (one-time)

1. The keystore was generated 2026-07-19 on this machine
   (RSA-4096, validity 25 y, alias `anima-upload`, dname CN=Anima Upload).
   If you want your own passphrase, regenerate:
   ```
   keytool -genkeypair -v -keystore anima-upload.keystore \
     -alias anima-upload -keyalg RSA -keysize 4096 -validity 9125
   ```
   and update `keystore.properties` accordingly.
2. **Back up** `anima-keys/` somewhere that is not this disk (password
   manager attachment / encrypted archive). Passwords live only in
   `keystore.properties`.
3. On first Play Console upload: enroll in Play App Signing (default for
   new apps) — the first signed AAB you upload registers this key as the
   upload key. No extra ceremony.
4. Verify a local release signature at any time:
   ```
   apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
   ```
