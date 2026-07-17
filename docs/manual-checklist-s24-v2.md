# Manual checklist — Galaxy S24, v0.2

The v0.1 checklist (manual-checklist-s24.md) still applies for the basics
(encrypted open path, listener opt-in, motion feel). v0.2 adds:

## 1. Mind delivery + FIRST CONVERSATION (the key item)

- [ ] Settings → Mind shows "The mind sleeps" honestly (no Prompt API on any
      S24 — expected forever, see research-v2 §A.1).
- [ ] In the phone browser: huggingface.co/litert-community/Gemma3-1B-IT →
      accept the Gemma license → download `gemma3-1b-it-int4.litertlm`
      (~584 MB) or the 529 MB `.task`.
- [ ] Mind screen → "Choose mind file…" → pick the download. Progress runs,
      screen stays open; final state shows file name + size.
- [ ] (Alt path) "Download by direct link" with a self-hosted URL: Wi-Fi-only
      default blocks on cellular; toggling "Allow mobile data" unblocks;
      airplane-mode mid-download → INTERRUPTED; retry resumes (watch the
      progress not restart from 0).
- [ ] Optional: paste the file's SHA-256 (from `git lfs ls-files -l` on the
      HF repo or `certutil -hashfile <file> SHA256`) and re-import — verify
      a corrupted/truncated file is rejected with the checksum message.
- [ ] Home: banner is gone, input appears. **Say hello. The creature answers
      on-device.** Note tokens/s feel, first-reply latency (engine load,
      expect seconds), RAM (adb shell dumpsys meminfo app.anima), warmth.
- [ ] Fact extraction: tell it something personal → candidate chip appears →
      confirm → visible in Soul. Garbage answers must produce no chip.
- [ ] Background the app mid-chat, return: engine reloads lazily (first
      reply after return is slow again — expected, ADR-005).
- [ ] Mind screen → Delete the mind file → home banner returns honestly.

## 2. Prompt budgets (ADR-005 measurement item)

- [ ] With ~24 facts and long history, send a message on the GEMMA tier and
      confirm no truncation crash; if `sizeInTokens` is available in
      tasks-genai 0.10.35, log actual input tokens vs the 1 700 budget and
      record the number in ADR-005.

## 3. Widget (ADR-007)

- [ ] Add the widget; a static creature appears (correct concept + genome).
- [ ] Plug in the charger → widget repaints to EATING within seconds
      (charge-edge one-shot).
- [ ] Unplug → repaint happens on the next 30-min heartbeat (accepted gap).
- [ ] fps/нагрев: with the widget on the home screen, creature app closed,
      confirm zero measurable battery drain over a day (Settings → Battery)
      and no warm device — the widget must be inert between updates.
- [ ] TalkBack on the widget reads "«name» is …, battery N%".

## 4. Evolution (date-shift test)

- [ ] Note current stage (Soul → Our story; body size on home).
- [ ] Settings → Date & time → manual, +40 days. Reopen the app: stage is
      YOUNG→ADULT (or further per score), body visibly larger, "30 days
      together" milestone on the story timeline.
- [ ] Set the date back: stage recomputes DOWN (score is pure); confirm no
      crash and no negative days (daysTogether floors at 1). Anti-tamagotchi:
      nothing "died" during the jump — only quiet growth.

## 5. Soul migration (two devices, or wipe-and-restore)

- [ ] Soul → Sealed backup: passphrase ≥8 chars → "Seal & save" to Downloads.
- [ ] Wrong passphrase on import → "Wrong passphrase…" and NOTHING changes.
- [ ] On the second device (or after reinstall): import → creature name,
      concept, seed (same body!), facts, story timeline, and the ORIGINAL
      hatch date all arrive. Re-import the same file → "already here" skips.
- [ ] Verify the backup file is unreadable garbage in a text editor.

## 6. TalkBack pass (v0.2 scope)

- [ ] Home: creature announces name + state + battery; chips and buttons all
      focusable with sensible labels; input field reachable in order.
- [ ] Mind / Soul / Story / Diary screens: linear swipe order sane, progress
      announced, no unlabeled tap targets.
- [ ] Dynamic type at max: no clipped text on Mind/Soul/Diary cards.

## 7. Quiet hours

- [ ] Enable quiet hours; set device clock to 23:30; post a test notification
      from an allowlisted app → nothing captured (counters unchanged).

## 8. Baseline profile (ADR-006)

- [ ] `gradlew :app:generateBaselineProfile` with the S24 connected; commit
      the generated `baseline-prof.txt` + `startup-prof.txt`.
