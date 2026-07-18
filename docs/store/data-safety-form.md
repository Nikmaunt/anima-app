# Google Play Data safety form — draft answers (2026-07-18)

Grounded in the live policy pages (research-v4 §8):
- "Collect" = "transmitting data from your app off a user's device".
- Exempt from disclosure: on-device-only processing; ephemeral in-memory
  processing; E2E-encrypted transfer unreadable by the developer.
- "Sharing" exemption: "a specific user-initiated action, where the user
  reasonably expects the data to be shared".
- A privacy policy link is mandatory even with zero collection.
Source: https://support.google.com/googleplay/android-developer/answer/10787469

**Chosen posture: "Reading A" (conservative).** BYOK cloud chat transmits
the user's typed message off-device to the provider THE USER configured with
THE USER's own key; Anima's developer runs no server and never sees it. The
policy's literal "collect" definition still covers off-device transmission,
so we declare it — as optional, ephemeral, not shared. The aggressive
"Reading B" (user's-own-account exemption → "No data collected") has no
published Google ruling → UNVERIFIED, not used. If a build ships without the
cloud-mind module, question 1 flips to **No** and the label is "No data
collected".

## Overview questions

| Console question | Answer | Rationale |
|---|---|---|
| Does your app collect or share any of the required user data types? | **Yes** | Solely the optional BYOK cloud chat (Reading A). |
| Is all of the user data collected by your app encrypted in transit? | **Yes** | BYOK calls are HTTPS-only (enforced in code — `CloudMindEngine` rejects non-https). Nothing else leaves the device. |
| Do you provide a way for users to request that their data be deleted? | **Yes** | In-app: wipe creature memory / delete conversations; nothing exists off-device on our side to delete. Data at the user's own AI provider is governed by that provider's account tools (stated in the privacy policy). |

## Data types — exactly one declared

**Messages → Other in-app messages** (the user's chat messages to the
creature, only while BYOK cloud chat is enabled):

| Field | Answer | Rationale |
|---|---|---|
| Collected? | Yes | Transmitted off-device to the user-chosen provider. |
| Shared? | No | Transfer to the user's own configured provider is "a specific user-initiated action, where the user reasonably expects the data to be shared" (policy exemption, quoted above). |
| Processed ephemerally? | Yes | In-memory request/response; the app persists chat history only locally, encrypted. |
| Required or optional? | Optional | Off by default; requires the user's own API key and an explicit toggle. |
| Purpose | App functionality | The only purpose. |

**Every other category** — Location, Personal info, Financial info, Health &
fitness, Photos/videos, Audio, Files & docs, Calendar, Contacts, App
activity, Web browsing, App info & performance, Device or other IDs:
**Not collected, not shared.** No analytics/telemetry SDKs, no ads SDKs, no
off-device crash reporting, no AAID/Android ID use (April 2025 rule), no
UsageStats.

Note on the model download: :core:model-delivery *receives* model bytes
(Play Asset Delivery or a user-initiated direct download); it transmits no
user data, so it triggers no declaration.

## Listing-visible security rows

- "Data is encrypted in transit" ✓
- "You can request that data be deleted" ✓
- MASA independent review: not pursued at launch (roadmap note).

## Consistency requirement

The label, the privacy policy (docs/privacy-policy.md), and the AAB behavior
must agree — Play runs automated pre-review checks against the bundle
(network endpoints in the BYOK module will be visible). Re-verify this form
against the shipping feature set at submission time.
