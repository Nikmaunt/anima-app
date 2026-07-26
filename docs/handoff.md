# Handoff — v0.9 autonomous run, 2026-07-26

## ПРАВИЛО ФОНОВЫХ ПРОЦЕССОВ (обязательно с v0.9)

Введено потому, что в прогоне v0.8 фоновая задача с эмулятором прожила
**девять часов** после окончания прогона, продолжая писать verbose-лог на
переполненный диск. Это не небрежность одного раза, это отсутствовавшее
правило. Теперь оно есть.

1. **Таймаут обязателен.** Любой запуск эмулятора идёт с явным таймаутом.
   Ни одна фоновая задача не переживает свою фазу.
2. **Уборка в конце КАЖДОЙ фазы, где поднимался эмулятор** — и
   подтверждение фактом, а не намерением:
   ```powershell
   Get-Process -Name "qemu*","emulator*" -EA SilentlyContinue | Stop-Process -Force
   adb kill-server
   Get-Process -Name "qemu*","emulator*" -EA SilentlyContinue   # обязан вернуть пусто
   ```
3. **Никакого foreground-verbose**, если результат нужен как файл: писать
   лог в файл, читать `tail`/`grep`. Многомегабайтные буферы вывода в
   контекст не вычитываются никогда.
4. **Финальный отчёт обязан содержать строку** «активных фоновых задач не
   осталось» — с выводом инструмента под ней.
5. **Проверять только через `Get-Process`, никогда через `adb`.** Найдено
   DoD-сабагентами v0.9 дважды подряд: любая команда `adb` (включая
   безобидное `adb devices`) **поднимает fork-server заново**, поэтому
   проверяющий своей же проверкой создаёт то, о чём потом отчитывается
   как о находке. Убивать — `Get-Process -Name "adb*" | Stop-Process
   -Force` (а не `adb kill-server`, который тоже стартует сервер, если
   его нет), проверять — `Get-Process` и ничем иным.

Сопутствующий гвоздь среды (v0.8, всё ещё актуален): после `adb emu kill`
остаются `hardware-qemu.ini.lock` и `multiinstance.lock` в `<avd>.avd/`, и
следующий старт падает с «Running multiple emulators with the same AVD».
Рецепт: `Stop-Process` на `qemu-system-x86_64` и `emulator`, удалить оба
`*.lock`, `adb kill-server && adb start-server`, стартовать заново.
Первый холодный бут после этого занимает >2 минут — это не зависание.

## ГЛАВНАЯ НАХОДКА v0.9 — блокером был КОНТЕЙНЕР, а не токенизатор

Полтора прогона проект считал, что дефолтный движок не может работать с
Qwen из-за токенизатора. Матрица Фазы E показала, что причина другая и
куда безобиднее: tasks-genai 0.10.35 не разбирает **контейнер
`.litertlm`** — любой. Тот же Qwen в контейнере **`.task`** он читает и
отвечает: Qwen2.5-0.5B за 2.9 с, продуктовый Qwen2.5-1.5B q8 за 7.0 с.

Что из этого следует для следующего прогона:

- **Целевой артефакт пака назван:**
  `Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.task`
  (litert-community, `gated: False`, Apache-2.0, 1 598 556 720 Б).
  Сжатый в паке — **1 377 210 476 Б**, запас 8.2% к лимиту Play, то есть
  на 819 КБ компактнее, чем `.litertlm`.
- **Движок менять не надо.** Флаг LiteRT-LM остаётся OFF и debug-only.
- **int4 закрыт измерением**, а не гипотезой: mixed-int4 `.litertlm`
  отказал ровно как q8 (ADR-022, ячейка 1).
- **Осталось ровно одно** — скорость и качество на Exynos 2400 (§0
  чеклиста v9). Эмулятор об этом не говорит ничего.

Урок методологии, который стоит держать: **вывод из одной ячейки — это не
вывод.** v0.8 не выдумал факт; он сверхобобщил одно измерение до вердикта
о движке, и вердикт разошёлся по документам. Различать причины умеет
только матрица, где меняется одна переменная за раз.

## ГВОЗДЬ СРЕДЫ v0.9 — эмулятор умирает насовсем после жёсткого убийства

Новый и дорогой: он стоил этому прогону двух пунктов (ячейка
`litertlm-android` × 1.5 ГБ `.litertlm` и вся Фаза C).

Симптом: после `Stop-Process` по qemu следующий старт эмулятора **не
загружается вообще** — процесс живёт, но ест 2 с CPU за минуту, держит
0.2 ГБ, консольный порт 5554/5555 никто не слушает, `adb devices` пуст,
verbose-лог обрывается на второй строке. **Пересоздание AVD с нуля не
помогает** (проверено: удалён `.avd` и `.ini`, создан заново, тот же
результат). Пробовано и отвергнуто: `-wipe-data`, уменьшение
`disk.dataPartition.size` с 12288 до 6144, удаление `*.lock`,
`adb kill-server`.

Вывод для следующего прогона: **эмулятор — расходник на один запуск.**
Планировать порядок ячеек так, чтобы всё нужное снималось за одну
загрузку; жёсткого убийства избегать до последнего (сначала `adb emu
kill`, ждать); если убили — считать, что до перезагрузки хоста эмулятора
нет. Перезагрузка хоста за пределами мандата автономной сессии.

## ОБЯЗАТЕЛЬНАЯ ПРЕДПОЛЁТНАЯ ПРОЦЕДУРА (выполнять ПЕРВОЙ, до любых фаз)

> **АДДЕНДУМ v0.9 (2026-07-26): дисковый блокер владельца СНЯТ владельцем.**
> Измерено в начале этой сессии: C: — **73.5 ГБ** свободно (было 2.2 ГБ на
> старте v0.8), D: — 178.6 ГБ. Порог 40 ГБ выполняется с большим запасом.
> Владелец также перенёс `GRADLE_USER_HOME` → `D:\gradle` (проверено:
> переменная видна сессии, каталог на старте отсутствовал, то есть кэш
> действительно пуст) и `HF_HOME` → `D:\hf-cache`. WSL и Docker удалены —
> любой скрипт, зависящий от WSL, работать не будет; конверсия int4
> отменена решением ADR-021 и не воскрешается. Порог этой сессии — не
> опускаться ниже 25 ГБ.

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

## ГЛАВНАЯ НАХОДКА v0.8 — блокер владельца №1 был не тем блокером

Полгода «блокером №1» считалась int4-конверсия: q8 (1 597 931 520 B)
не влезал в лимит Play 1.5 GB. Прогон v0.8 проверил обе посылки.

1. **Размер: посылка ОШИБОЧНА.** Сравнивали размер файла на диске, а
   Play меряет compressed download size. Замер с реальной моделью в
   паке: `Defl:N` → 1 378 024 613 B; `bundletool get-size total
   --modules=mind_pack` → 1 393 939 118 B MAX (с base), пак сам по себе
   ≈1 378 035 869 B. Запас **8.1%** к строгому десятичному прочтению
   лимита. Сжатие не отключено (`noCompress` в проекте отсутствует).
2. **Рантайм: обнаружен НАСТОЯЩИЙ блокер.** tasks-genai 0.10.35 — наш
   дефолтный движок — не инициализируется на этом файле за 0.14 с:
   `INVALID_ARGUMENT: SentencePiece tokenizer is not found in the model`
   (`tokenizer_utils.cc:136`). Требование SentencePiece сидит в
   рантайме, а не только в бандлере `.task`, как полагал ADR-018.
3. **int4 не помогла бы** (гипотеза, UNVERIFIED): ошибка про формат
   токенизатора, ортогональна квантизации.
4. **LiteRT-LM — INCONCLUSIVE:** 76 с работы (много дальше tasks-genai),
   затем `lowmemorykiller` (2 GB эмулятор против 1.6 GB модели). Предел
   среды, не вердикт о движке.

Что это даёт следующему прогону: Фазу D надо переиграть на машине с
достаточной RAM (AVD ≥ 6 GB — попытка поднять его в v0.8 упёрлась в
зомби-эмулятор, см. гвоздь ниже) или на S24. Именно LiteRT-LM на
`.litertlm` — единственная живая ветка к «говорит на шести языках
из коробки».

**Гвоздь среды (новый, v0.8):** после `adb emu kill` эмулятор оставляет
`hardware-qemu.ini.lock` и `multiinstance.lock` в `<avd>.avd/`, и
следующий старт падает с «Running multiple emulators with the same AVD».
Рецепт: убить `qemu-system-x86_64` и `emulator` через `Stop-Process`,
удалить оба `*.lock`, `adb kill-server && adb start-server`, стартовать
заново; первый холодный бут после этого занимает >2 минут — не считать
это зависанием.

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
  заново в **ADR-021**: официальный q8 влезает в лимит Play с запасом
  8.1%, НО дефолтный движок tasks-genai его не читает — **пак остаётся
  ПУСТЫМ**, int4 уходит с критического пути как не решающая проблему.
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
