# ADR-006: Baseline profile — generator wired, generation is device-bound

Status: accepted, 2026-07-17.

## Decision

- `:baselineprofile` module (androidx.baselineprofile 1.4.1 + macrobenchmark)
  is wired to `:app` with a startup-path generator
  (`StartupProfileGenerator`: cold start → home → 3 s of creature frames,
  `includeInStartupProfile = true` for DEX-layout optimization; R8 is already
  on, AGP 8.13 qualifies).
- The app consumes `androidx.profileinstaller`.
- **No profile file is committed in this change.** Research (research-v2
  §B.5) confirms there is no device-free generation path: an emulator or an
  API 33+ device must run the macrobenchmark. This build machine has no
  guaranteed emulator; the owner's S24 (API 34+, non-rooted) is a valid
  generator device.

## Why not commit a hand-written or empty profile

A baseline profile is a *measurement*. Committing a hand-authored rule list
would claim a verified startup win that nobody measured; an empty committed
profile silently disables the mechanism while looking configured. Both are
the kind of overclaim the v0.1 audit exists to catch.

## The honest path (S24 checklist item)

```
gradlew :app:generateBaselineProfile   # S24 connected, USB debugging on
# output lands in app/src/release/generated/baselineProfiles/ — commit it
```

Until that run happens, release builds ship without a profile — measurably
slower first-day JIT, zero correctness impact.
