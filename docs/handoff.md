# Handoff — v0.8 autonomous run, 2026-07-25

## ОБЯЗАТЕЛЬНАЯ ПРЕДПОЛЁТНАЯ ПРОЦЕДУРА (выполнять ПЕРВОЙ, до любых фаз)

Переполнение C: сорвало два прогона подряд: v0.6 — конверсию int4 (WSL
VHDX на C:), v0.7 — дало ЛОЖНЫЙ красный вердикт DoD. Это не «урок
среды», это процедура.

1. **Замерить свободное место на всех дисках.**
   `Get-PSDrive -PSProvider FileSystem`.
2. **Порог — 40 ГБ на C:.** Ниже порога чистить, в этом порядке:
   - `%LOCALAPPDATA%\Temp\DiagOutputDir` (в v0.8 дал 1.2 ГБ);
   - `%LOCALAPPDATA%\Temp\conscrypt_openjdk_jni-*.dll` (десятки копий);
   - `%LOCALAPPDATA%\Temp\robolectric*`;
   - `~\.gradle\daemon`, `~\.gradle\.tmp` (логи демона);
   - `~\.gradle\caches\build-cache-1` (регенерируем).
   НЕ трогать: `~\.gradle\caches\modules-2` (≈7 ГБ, перекачка по сети),
   Android SDK (18 ГБ, нужен), Chrome-профиль пользователя.
3. **Замерить снова.** Если после чистки всё ещё < 40 ГБ — это **блокер
   владельца**, а не повод продолжать вслепую: фазы со скачиванием
   моделей не начинать, порядок фаз перестроить, факт записать в отчёт.
4. **Всё тяжёлое — на D:.** Обязательные переменные на каждую сессию:
   - `ANDROID_AVD_HOME=D:\Android\avd` (без неё GMD требует 7.2 ГБ на C:)
   - `TMP` / `TEMP` / `GRADLE_OPTS=-Djava.io.tmpdir=` → `D:\Hermes-tmp`
   - кэш моделей: **`D:\Android\llm-cache`** (вне репозитория, ADR-021)

**Состояние на старте v0.8 (2026-07-25):** C: — **2.2 ГБ** свободно
(порог не достигнут даже близко), D: — 183 ГБ. После чистки C: — 4.39 ГБ.
До 40 ГБ дотянуть невозможно: C: всего 249 ГБ, из них Android SDK 18 ГБ и
`modules-2` 7 ГБ неудаляемы. **Зафиксировано как блокер владельца:**
машине нужен либо больший системный диск, либо перенос Android SDK и
GRADLE_USER_HOME на D:. Прогон v0.8 выполнен с обходом (весь temp и все
модели на D:), обход работает, но он не заменяет решение.

**Веса моделей в git не попадают** — `mind-pack/.gitignore:4`
(`src/main/assets/*.litertlm`), проверено `git check-ignore -v` после
сборки бандла с реальной моделью внутри.

## ПРОЦЕССНОЕ ПРАВИЛО (нарушено в v0.7, обязательно с v0.8)

**Главный агент НЕ имеет права объявить красный пункт DoD разрешённым
собственной перепроверкой.** Порядок: устранить причину → коммит →
запустить ВТОРОГО свежего read-only сабагента только на этот пункт →
включить оба вердикта в отчёт дословно. Если второй сабагент недоступен
по контексту — пункт **остаётся красным** в отчёте. Это нормальный
исход, а не провал.

Сопутствующее правило, выведенное из находки v0.8 про лицензии: **число,
увиденное глазами на устройстве, — наблюдение, а не подтверждение.** В
CONFIRMED оно попадает только после сверки с порождающим артефактом. Два
прогона подряд расхождение 222 vs 197 не остановило никого именно
потому, что сверки не было (docs/audit-v07.md §DEGRADED 1).

---

# Handoff — v0.7 autonomous run, 2026-07-24

Run COMPLETED in-session; this file is retained as the protocol requires.
Final state = the committed tree plus the session's final report. Key
documents this run: docs/audit-v06.md, docs/freshness-2026-07.md,
ADR-020, docs/manual-checklist-s24-v7.md, CHANGELOG 0.7.0.

- Phase 0: first audit with ZERO fabricated v0.6 claims; first
  live-emulator eyes pass (AVD google_apis-34 headless + adb
  screencap/uiautomator — the recipe works and found what goldens
  can't: crushed onboarding labels). FLAG_SECURE live-proven (0-byte
  screencap). GMD DayInLifeTest red was diagnosed to cold-boot slowness
  (warm connected runs pass twice) — safety net raised 60s→240s.
- Phase 2: LiteRT-LM second engine (debug-only, flag OFF, CPU-only by
  test). API was written against the actual AAR (javap) — the Kotlin
  API is Engine/EngineConfig/Conversation/Contents.of, NOT the blog
  shapes. JVM smoke green: litertlm-jvm 0.14.0 + Qwen2.5-1.5B q8
  (model cache D:\Android\llm-cache, gemma repos are HF-gated → 401).
  ФОРМУЛИРОВКА ИСПРАВЛЕНА (аудит v0.8): q8 в v0.7 был моделью
  JVM-СМОУКА, а НЕ «нашим продуктовым дефолт-паком» — на тот момент
  дефолтом пака был пустой слот, а целевым артефактом int4-конверсия
  (ADR-018, матрица исходов, строка 3). Судьба содержимого пака решена
  заново измерением в **ADR-021**: официальный q8 влезает в лимит Play
  с запасом 8.1%, пак наполняется им, int4 уходит с критического пути.
- Phase 3: ConceptGallery weight(1f) fix; 530MB→registry-fed size (one
  Formatter path, live "~1.2 GB"); patch bumps ×3 batches each with a
  full green loop; licenses export 227 libs (litertlm included);
  version 0.7.0/7.
- §0b wallpaper listing claim: verified ABSENT from all store texts ×6
  locales — obligation vacuous; measurement stays on the S24 list.

OPEN test-infra defect (v0.8 backlog, full diagnostic trail below):
DayInLifeTest on THIS machine. (1) ATD GMD: emulator screen stays 100%
black for the test's whole life (frame-by-frame screencap, all pixels
0,0,0) — no frames render at all; recreating the GMD device from
scratch does not help; the SAME apk on a google_apis AVD renders and
passes onboarding→import. Environment defect of ATD-on-this-machine —
CI's Linux KVM GMD is a different environment (untested this session).
(2) The virtual-frame pump needed a 2ms yield (committed) — without it
slow images starve the main looper and the FIRST composition never
lands. (3) The yield surfaced a deterministic IME race in the soul
import step — fixed (closeSoftKeyboard + swipe wait, committed) and
proven to pass. (4) The scenario tail (post-export) still hangs here —
NOT diagnosed to root cause; candidates: another gesture-vs-IME block
in the export/share step. The unit/golden/lint loop and the other four
device suites are green; the E2E needs one more debugging session,
ideally on the CI runner or a faster host.

Environment lessons (this machine, this session):
- C: is 100% full: GMD AVD → ANDROID_AVD_HOME=D:\Android\avd (without it
  GMD fails needing 7.2 GB on C:); LLM smoke cache → ANIMA_JVM_LLM_CACHE.
- Windows file-lock transients (classes.jar, lint-cache jars held by a
  process) intermittently fail big loops: `gradlew --stop`, delete the
  named cache dir, rerun — three occurrences, all cleared this way.
- Cold-emulator first runs are >60s to first frame from D: — any
  wall-clock deadline in instrumented tests must assume a cold boot.

Unproven risks carried to S24 checklist v7: §0 int4-Qwen artifact
(convert.sh still blocked by env — host reboot), §0b wallpaper night
battery (fourth carry, now measurement-only), §0c RU quality on int4,
§0d live-data upgrade v0.6→v0.7, §0e signed-release R8, §0f folds,
§0.8 NEW: engine comparison tasks-genai vs LiteRT-LM on one model
(the ADR-020 default-flip gate).
