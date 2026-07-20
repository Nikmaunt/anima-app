# Anima — Privacy Policy

*Effective: 2026-07-19. This page is the policy of record for the Anima
Android app; it is linked from the Play listing and from Settings → Why no
internet, as Google Play requires even of apps that collect nothing.*

## The short version

Anima collects no data. The app has no accounts, no ads, no analytics, no
telemetry, and no crash reporting to us. Your creature's memory lives in an
encrypted database on your phone and nowhere else. We — the developers —
run no servers and cannot see anything you do.

## What stays on your phone (everything)

- Your conversations with the creature.
- The facts the creature remembers (each one saved only after you tap yes).
- The creature's body diary (battery and charging patterns, quiet hours,
  usage-free presence signals like "the phone is resting").
- Notification titles, only if you turn the notification sense on, only
  from apps you allowlist, pruned after 7 days, and never message bodies
  unless you separately opt in.
- Time-capsule letters you write to your future self (v0.5) — same
  encrypted database, delivered back to you by the creature, never sent
  anywhere.
- Reports you make about an AI reply (v0.6, long-press → report): the
  reply is deleted; only a local timestamped "a reply was flagged" note
  stays in the body diary. Nothing about it leaves the phone.
- A sparse log of barometer readings (v0.5, phones that have the sensor):
  timestamps and air-pressure values only, kept for 12 hours so the
  creature can "feel the weather in its bones". Air pressure is not
  location; the log never leaves the device.

All of it is stored in a database encrypted with a key that is generated on
your device and wrapped by your phone's hardware keystore. It is excluded
from Android backups. Uninstalling the app, or using "forget everything" in
Settings, destroys it.

## What can touch the network (two things, both yours to trigger)

1. **Bringing the mind file.** The optional on-device AI model (~530 MB)
   arrives either through Google Play's asset delivery or a download you
   start yourself. Bytes come IN; no data about you goes out. Google Play's
   own delivery is governed by Google's terms.
2. **The optional cloud mind (off by default).** If you enable it and paste
   your own API key for a provider you choose, then — and only then — your
   chat messages, the creature's relevant memory facts, and its body report
   are sent to THAT provider to generate replies. This traffic goes to your
   own account under your provider's privacy policy; we never receive,
   store, or see it. Turn the switch off and nothing leaves the phone again.
   Your API key is stored encrypted by the hardware keystore and never
   leaves the device (it is never part of any export).

There is no third thing. A build-time test fails our release if any other
part of the app gains network capability. (v0.5 adds one detail to the
second thing: a "check key" button that, when you press it, asks your
chosen provider whether your key works — it sends the key header and no
conversation content.)

## Permissions

Anima requests no dangerous permissions: no location, no contacts, no
camera, no microphone, no storage access, and no usage-stats access — the
app cannot see what you do in other apps, by construction. The notification
sense uses the system's notification-listener permission, which you grant
explicitly in system settings and can revoke there at any time.

## Deleting your data

Everything is on your device: use Settings → forget everything, or simply
uninstall the app. There is nothing on our side to delete. If you used the
cloud mind, whatever your chosen provider retained is governed by that
provider's tools and policy — we cannot delete it for you because we never
had it.

## Children

Anima is a general-audience app and knowingly collects no data from anyone,
children included.

## Changes

If a future version ever changes any of the above, this page changes first,
the in-app "Why no internet" page changes with it, and the Play Data safety
label is updated before release.

## Contact

Questions: open an issue on the project's repository, or use the contact
address on the Play listing.
