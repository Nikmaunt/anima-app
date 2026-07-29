# Фаза 0 прогона v1.1b — свежий аудит хендоффа против кода

Дата: 2026-07-30. Ветка `main`, дерево чистое на момент аудита.
Метод: собственный прогон команд и чтение исходников. Ни одна строка кода
не изменена. Проверялись `docs/handoff-v11.md`, `docs/audit-v10.md`,
`docs/design-system-v11.md` и утверждения задания.

Правило этого документа: если рядом с утверждением нет пути, лога или
вывода команды — утверждения нет.

---

## 1. Состояние репозитория — подтверждено

| Факт | Значение | Чем проверено |
|---|---|---|
| Ветка | `main`, отслеживает `origin/main`, расхождений нет | `git branch -vv` → `* main b3c81d6 [origin/main]` |
| HEAD | `b3c81d6` | `git log --oneline -1` |
| `git status` | пусто | `git status --porcelain` → пустой вывод |
| Remote | `https://github.com/Nikmaunt/anima-app.git` | `git remote -v` |
| Видимость | **PRIVATE** | `gh repo view Nikmaunt/anima-app --json visibility` → `{"visibility":"PRIVATE"}` |
| Коммитов в истории | **70** | `git rev-list --count HEAD` |
| versionName / versionCode | **`1.0.0` / `10`** — не поднимались | `app/build.gradle.kts:104-105` |
| Toolchain | AGP 8.13.2, Kotlin 2.2.0, KSP 2.2.0-2.0.2, composeBom 2025.06.01, navigation 2.9.8 | `gradle/libs.versions.toml:12,13,14,19,20` |
| SDK | compileSdk 36, minSdk 31 | `build-logic/src/main/kotlin/KotlinAndroid.kt:20,23` |
| `app-release.aab` | 53 390 289 Б, от 2026-07-28 02:25 | `ls -la app/build/outputs/bundle/release/` |
| `NetworkIsolationTest` | **14** `@Test` в исходнике | `grep -c "@Test" app/src/test/kotlin/app/anima/NetworkIsolationTest.kt` → `14` |
| Auto Backup | **выключен**, `android:allowBackup="false"` | `app/src/main/AndroidManifest.xml:30` |

---

## 2. Полный `check` — и находка о самом `check`

### 2.1 Зелёная сборка, в которой не выполнился ни один тест

Я выполнил ровно ту команду, которую предписывает хендофф:

```
./gradlew check -Proborazzi.test.verify=true
→ BUILD SUCCESSFUL in 1m 12s
→ 1745 actionable tasks: 25 executed, 1720 up-to-date
→ > Task :app:testDebugUnitTest UP-TO-DATE
```

**Ни один тестовый таск не выполнялся.** Второй прогон той же команды после
удаления `*/build/test-results` дал `36 from cache`:

```
> Task :app:testReleaseUnitTest FROM-CACHE
BUILD SUCCESSFUL in 6s
```

То есть XML с результатами был восстановлен из build-кэша, а не создан
прогоном. Обе сборки «зелёные», обе не доказывают ничего о текущем
состоянии тестов.

Тесты выполнились по-настоящему только на третьей попытке:

```
find . -path "*/build/test-results" -type d -exec rm -rf {} +
./gradlew check -Proborazzi.test.verify=true --no-build-cache
→ BUILD SUCCESSFUL in 57s
→ 1745 actionable tasks: 61 executed, 1684 up-to-date
```

Лог: `/tmp/check-verify2.log` (вне репозитория). В нём 38 тестовых тасков
без суффикса `UP-TO-DATE`/`FROM-CACHE`, включая `:app:testDebugUnitTest`,
`:app:testReleaseUnitTest`, `:core:model:test` и `:tools:litertlm-smoke:test`.

> **Новая ловушка среды, которой нет в хендоффе.**
> `-Proborazzi.test.verify=true` — необходимое условие, но не достаточное.
> Свойство Gradle не меняет входы тестового таска, поэтому таск остаётся
> `UP-TO-DATE`, а голдены не сравниваются, хотя флаг передан. Чтобы прогон
> что-то значил, нужно снести `*/build/test-results` **и** передать
> `--no-build-cache`. Хендофф выводит правило «проверь, что таск реально
> выполнился» из истории с lint — оно ровно так же применимо к самому
> предписанному им же тестовому прогону.

### 2.2 Числа — из вывода прогона

Посчитано по `*/build/test-results/**/TEST-*.xml`, созданным прогоном из
2.1 (скрипт `/tmp/cnt2.py`, вне репозитория):

| Таск | Тестов |
|---|---|
| `testDebugUnitTest` (все модули) | 138 |
| `testReleaseUnitTest` (все модули) | 138 |
| `core:model:test` (JVM) | 116 |
| `tools:litertlm-smoke:test` (env-gated) | 27, из них 7 SKIPPED |
| **Всего** | **419**, отказов 0, ошибок 0, пропусков 7 |

Отказов и ошибок **нет ни одного**. Все 7 пропусков — в
`tools/litertlm-smoke` (гейты `ANIMA_*`, `docs/audit-v10.md` §0.5).

**Уточнение к цифре из хендоффа.** Хендофф пишет «392 tests». 419 − 27 =
392, то есть цифра воспроизводится, но читать её надо как *выполнения*, а
не как тесты: у android-модулей `check` прогоняет и `debug`, и `release`,
поэтому каждый тест считается дважды. Различных тест-методов вне
`litertlm-smoke` — **254** (138 android + 116 в `core:model`).

`NetworkIsolationTest` — 14 тестов в каждом варианте, отказов 0
(`app.anima.NetworkIsolationTest [testDebugUnitTest] = 14`,
`[testReleaseUnitTest] = 14`). Инвариант зелёный.

По модулям (сумма двух вариантов): `app` 40, `core/creature` 66,
`core/data` 40, `core/mind` 30, `core/ui` 30, `core/cloud-mind` 16,
`core/voice` 12, `feature/rest` 24, `feature/home` 12,
`feature/wallpaper` 6, `core/model` 116, `tools/litertlm-smoke` 27.

**Модули с нулём юнит-тестов** (тестовый таск выполнился, XML не создан):
`core/body`, `core/model-delivery`, `feature/notifications`,
`feature/onboarding`, `feature/settings`, `feature/soul`,
`feature/widget`. Это существенно для Задачи 1: три из четырёх поверхностей,
которые рисуют существо вне Home — виджет, обои и Душа — лежат в модулях
без юнит-тестов (`feature/wallpaper` — исключение, 3 теста на бюджет обоев).

### 2.3 Голдены действительно сравнивались

Это единственная часть, где хендофф просил не верить прозе. Проверено
артефактом, а не пересказом:

`core/creature/build/test-results/roborazzi/debug/results-summary.json`

```json
{"summary":{"total":32,"recorded":0,"added":0,"changed":0,"unchanged":32}}
```

32 голдена сравнены, 32 совпали, 0 изменённых, 0 перезаписанных. Такие же
summary созданы для `core/ui`, `feature/home`, `feature/rest` в обоих
вариантах.

Обратная половина утверждения тоже воспроизвелась. Прогон **без** флага:

```
./gradlew :core:creature:testDebugUnitTest --rerun
→ > Task :core:creature:finalizeTestRoborazziDebug SKIPPED
→ BUILD SUCCESSFUL in 17s
```

Таск, который сравнивает и отчитывается, **пропускается**. При этом
голдены не перезаписываются — mtime всех 32 файлов остался
`Jul 28 16:38`, `git status` пуст. То есть без флага режим не «тихая
перезапись», а «сравнения не было вовсе», как и написано в хендоффе.

Отдельная поправка на будущее: в `*/build/outputs/roborazzi/` лежат файлы
`*_actual.png` и `*_compare.png` с временем моего прогона. **Это не признак
расхождения** — summary того же прогона говорит `changed: 0`. Судить о
результате по наличию `_compare.png` нельзя, только по `results-summary.json`.

---

## 3. Артефакты `docs/design/v11/` — открыты, а не перечислены

Все пути из хендоффа существуют: `phase0-emulator-eyes.md`,
`phase3-home.md`, `phase5-1-stillrender.md`, `phase5-2-scale.md`,
`before/` (27 PNG), `after/` (10 PNG), `catalog/` (6 PNG), `contact/`
(3 PNG), `rig/` (8 PNG).

Открыты и просмотрены глазами: три контактных листа и `after/10-home-dark-rigscale.png`.
Что видно (это же — вход в Задачи 2, 3 и 4):

**`contact/contact-busy-photo.png`** — обвинение хендоффа подтверждается и
оказывается тяжелее, чем описано.
- `SPIRIT_ORB`: тела нет. Читаются только глаза и блик; цветные пятна
  подложки проходят сквозь шар насквозь.
- `PIXEL_PET`: худший случай из восьми. Клетки полупрозрачны, подложка
  видна в каждой, силуэт читается как зелёная сетка поверх фото, а не как
  существо.
- `MOTH`: крылья прозрачны, тело — тонкая коричневая полоска; на пёстром
  почти пропадает целиком.
- `JELLY`: купол читается, щупальца — нет. На светлых пятнах подложки они
  исчезают. Это ровно тот пограничный случай, про который спрашивает задание.
- `FOX_KIT`, `ROBOT`, `SPROUT` читаются. **У `ROBOT` и `SPROUT` уже есть
  вычитающая составляющая** — серый эллипс тени и коричневый эллипс почвы
  соответственно. Это готовый образец того, что Задача 2 требует для
  остальных.

**`contact/contact-light-wallpaper.png`** и **`contact-dark-wallpaper.png`** —
на однородных подложках читается всё. То есть дефект не «облик плохой», а
«облик существует только как аддитивный свет», и провал воспроизводится
исключительно на ярком/пёстром фоне. Формулировка задания точна.

Два дефекта, увиденных на снимках и не связанных с прозрачностью:
- `EMBER` в `asleep-night` заметно мельче своих же трёх состояний **и**
  меняет силуэт (высокое пламя → маленький тёмный ромб). Видно на всех трёх
  подложках. Вход в Задачу 3.
- `MOTH` в `asleep-night` теряет крылья целиком — остаются усики и тело.
  То есть у `MOTH` разброс масштаба по состояниям тоже есть, и он не назван
  ни в хендоффе, ни в задании.
- В строке `PIXEL_PET`, в столбце `asleep-night`, у правого верхнего края
  кадра нарисованы 1–2 клетки **вне тела** (на тёмной подложке — две бледные,
  на светлой — одна). Похоже на тот самый «пиксель за кадром», о который
  хендофф упирается в объяснении, почему нельзя поднять калибровку.

**`after/10-home-dark-rigscale.png`** — посылка Задачи 4 подтверждается
глазами. Имя «Nika» 32 Light + «вместе 1 день» вверху, ряд навигации
внизу, между ними существо, и **два больших пустых поля почти одинакового
размера** сверху и снизу от тела. Никакого содержимого между ними нет —
это и есть худший случай «день, реплик нет», от которого задание требует
проектировать.

---

## 4. Ловушки среды — что воспроизвелось, что нет

| Ловушка из хендоффа | Результат |
|---|---|
| Голдены не сравниваются без `-Proborazzi.test.verify=true` | **Воспроизведено** (§2.3): без флага `finalizeTestRoborazzi*` SKIPPED |
| `lint` падал на нетронутом `main`, это прятал кэш Gradle | **Больше не воспроизводится, и это правильно** — исправлено в `8b754c4`. Прогон с нуля: `./gradlew :core:body:lintDebug :core:creature:lintDebug --rerun-tasks --no-build-cache` → `BUILD SUCCESSFUL, 79 actionable tasks: 79 executed`. Лог `/tmp/lint-rerun.log` |
| `DayInLifeTest` виснет на импорте души, `DayInLifeTest.kt:258` | **Строка подтверждена**: :258 — `Espresso.closeSoftKeyboard()`, за которым `waitForCondition("import find button recomposes after IME hides")`. Сам прогон E2E не запускался (запрещён как способ навигации) |
| `RigGoldenTest` непригоден для измерения масштаба | **Подтверждено чтением**: `RigGoldenTest.kt:57-65` снимает Compose-узел `Image` с `testTag`, а не bitmap. Бокс узла не зависит от содержимого |
| `GlowSkinFalloffTest` ничего не знает о размере кадра | **Подтверждено**: все его утверждения — сравнения констант `GlowSkin.HALF_SIDE` и `GlowSkin.FALLOFF_RADII` плюс текстовая проверка AGSL. Кадра он не видит |
| `FrameScaleTest` меряет только `ALERT` | **Подтверждено**: `FrameScaleTest.kt:35` — `mood: Mood = Mood.ALERT`, и ни один из четырёх тестов другого значения не передаёт |
| Эмулятор `look35` существует | **Подтверждено**: `D:/Android/avd/look35.avd/` на месте, запущен этой сессией, поднимается (`adb devices` → `emulator-5554`). Удалять `*.lock` не потребовалось |
| Не запускать два Gradle одновременно | Соблюдалось; все прогоны последовательные |

---

## 5. Расхождения хендоффа и задания с деревом

Отдельным списком, как требует задание. Порядок — по тяжести.

### 5.1 🔴 «Девять не-тестовых `?: SPIRIT_ORB`» — недоучёт, и пропущены самые опасные

`grep -rn "SPIRIT_ORB" --include=*.kt core/ feature/ app/src/main | grep -v "/test"`
даёт 35 строк. Из них **мест с дефолтом-подстановкой — 14, а не 9**:

| Место | Хендофф |
|---|---|
| `core/data/.../SoulBackup.kt:55` | назван |
| `core/data/.../SoulBackup.kt:132` | назван |
| `core/model/.../CreatureConcept.kt:37` | назван |
| `feature/home/.../HomeViewModel.kt:761` | назван |
| `feature/home/.../BodyDiaryScreen.kt:150` | назван |
| `feature/rest/.../RestViewModel.kt:67` | назван |
| `feature/settings/.../SettingsScreen.kt:161` | назван |
| `feature/settings/.../WardrobeScreen.kt:82` | назван |
| `feature/onboarding/.../OnboardingScreen.kt:86` | назван (законный) |
| `feature/widget/.../AnimaWidget.kt:45` | назван косвенно, как «WidgetSnapshot» — фактически файл другой |
| **`feature/wallpaper/.../AnimaWallpaperService.kt:126`** | **ПРОПУЩЕН** |
| **`feature/soul/.../SoulViewModel.kt:151`** | **ПРОПУЩЕН** |
| **`feature/soul/.../SoulViewModel.kt:170`** | **ПРОПУЩЕН, и это место ЗАПИСИ** |
| **`feature/soul/.../StoryScreen.kt:75`** | **ПРОПУЩЕН** |

Плюс **шесть** объявлений дефолтного концепта в полях состояний, которые
хендофф не разбирает вовсе: `HomeViewModel.kt:76`, `BodyDiaryScreen.kt:79`,
`RestViewModel.kt:23`, `WardrobeScreen.kt:48`, `SoulViewModel.kt:36`,
`StoryScreen.kt:50`. Плюс жёстко зашитый `SPIRIT_ORB` в
`OnboardingScreen.kt:184` (`rememberCreature(CreatureConcept.SPIRIT_ORB, seed)`
для яйца до вылупления).

Готовый образец правильного поля есть в том же дереве:
`SettingsScreen.kt:62` объявляет `val concept: CreatureConcept? = null` —
без дефолта. Значит nullable-состояние в этом коде уже принято и работает.

Отдельно: `feature/soul/.../SoulViewModel.kt:101` —
`MutableStateFlow(CreatureConcept.SPIRIT_ORB to 0L)`. Это не подстановка на
случай сбоя, а **начальное значение потока**: экран Души рисует
`SPIRIT_ORB` и сид `0L` на первых кадрах *всегда*, при каждом открытии, а
не только при ошибке чтения.

Почему пропуски существенны:
- **Обои** (`AnimaWallpaperService.kt:126`) — это полноэкранная поверхность
  на домашнем экране. Ровно тот сценарий, который задание называет
  «человек не распознает это как ошибку», и его в списке не было.
- **Душа** — экран переноса, то есть платная часть продукта.
  `SoulViewModel.kt:170` подставляет дефолт в **markdown-экспорт**, то есть
  в файл, уходящий с устройства.

Итог: мест **записи** не два, а **три** — `SoulBackup.kt:55`,
`SoulBackup.kt:132` и `SoulViewModel.kt:170`.

### 5.2 🔴 `SoulBackup.kt:132` — мёртвый код; настоящий дефект на уровень ниже

```kotlin
// SoulBackup.kt:129
val concept = CreatureConcept.fromWire(creature.getValue("concept").jsonPrimitive.content)
// SoulBackup.kt:132
identity.hatch(name, concept ?: CreatureConcept.SPIRIT_ORB, seed, hatchedAt)
```

`CreatureConcept.fromWire` объявлена как `fun fromWire(wire: String): CreatureConcept`
(`CreatureConcept.kt:37`) — тип **не-nullable**. Значит `?: SPIRIT_ORB` в
строке 132 недостижим. Правка этой строки, как её описывает хендофф, не
изменит поведения ни в одном случае.

Настоящая подстановка происходит внутри `fromWire`:
`entries.firstOrNull { it.wire == wire } ?: SPIRIT_ORB`. Неизвестное
значение в файле бэкапа молча становится `SPIRIT_ORB` и затем **пишется в
долговременные данные** вызовом `identity.hatch`. Проверка версии
(`VERSION = 2`) от этого не защищает: добавление девятого облика в будущей
версии не меняет номер формата.

### 5.3 🟠 Ноль тестов у поверхностей из Задачи 1

`feature/widget`, `feature/soul`, `feature/settings` — 0 юнит-тестов
(§2.2). То есть у трёх из четырёх мест, которые задание требует
исправить, нет ни одного теста, который что-либо удержит. Задача 1г
(grep-guard) при таком раскладе — не дополнение, а единственная защита.

### 5.4 🟡 Auto Backup выключен — одна гипотеза закрыта

`android:allowBackup="false"` (`AndroidManifest.xml:30`). Значит путь
«DataStore восстановился, зашифрованная БД нет → концепт `null` при
`onboardingDone = true`» **невозможен**. Записываю, чтобы никто не выводил
его заново.

### 5.5 🟡 Число «392 теста»

Воспроизводится, но это выполнения, а не тесты; различных тест-методов
254 (§2.2). Формулировка хендоффа «392 tests» вводит в заблуждение при
любой попытке сверить её с числом `@Test` в исходниках (их 300 во всём
дереве, включая androidTest и env-gated).

### 5.6 ⚪ Мелкие, ожидаемые

- HEAD в хендоффе — `832bde2`, фактически `b3c81d6`. Разница — сам
  коммит хендоффа, написанный после. Ожидаемо.
- Коммитов в истории: хендофф пишет 69, фактически **70** — по той же
  причине.
- Утверждение задания «в исходнике 14 `@Test`, не 13» для
  `NetworkIsolationTest` — **верно**, подтверждено и grep'ом, и выводом
  прогона.
- Утверждение задания «versionName/versionCode остались 1.0.0/10» —
  **верно**.
- Утверждение задания «дерево чистое, всё запушено» — **верно**.

---

## 6. Что из этого меняет план работы

1. Задача 1в шире, чем описана: адресов не девять, а **22**
   (14 подстановок + 6 дефолтов полей состояний + начальное значение
   потока Души + жёстко зашитый облик в онбординге), и в них входят обои
   и Душа.
2. Задача 1а: правка `SoulBackup.kt:132` по букве хендоффа — no-op.
   Исправлять надо `CreatureConcept.fromWire`, а это изменение контракта
   разбора — разбирается в самой задаче, с артефактом.
3. Любой мой последующий отчёт о зелёном `check` обязан приводить
   `results-summary.json` и отсутствие `UP-TO-DATE` на тестовых тасках,
   иначе он ничего не значит (§2.1).
