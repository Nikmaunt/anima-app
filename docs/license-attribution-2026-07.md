# Лицензионное расхождение 227 / 202 / 192 — закрыто (v0.9 Фаза L, 2026-07-26)

Находка v0.8 (audit-v07 §DEGRADED 1) состояла в том, что закоммиченный
`aboutlibraries.json` несёт 227 записей, а до пользователя доезжает 202
(debug APK) или 192 (release bundle), и CI-проверка свежести диффает не
тот артефакт. v0.8 оставил выбор «правильного числа» владельцу как
юридическое решение.

Проверка инструментом показала, что **юридического решения здесь не
требуется: дыры в атрибуции нет ни одной.** Вопрос был не юридический, а
арифметический, и на него можно было ответить в прошлом прогоне.

## Метод (почему текстовый вывод `dependencies` не годится)

Первая попытка сверяла имена библиотек с текстом
`gradlew :app:dependencies --configuration releaseRuntimeClasspath`. Так
делать нельзя, и это ловушка, в которую легко попасть дважды:

1. Дерево зависимостей **сокращает повторяющиеся поддеревья** маркером
   `(*)`. Из 192 библиотек release-графа в тексте нашлись только 158 —
   то есть 34 «отсутствия» были артефактом усечения, а не фактом.
2. Проверка подстрокой даёт ложные срабатывания:
   `androidx.compose.material:material` — префикс
   `…:material-android`, `androidx.compose.ui:ui-tooling` — префикс
   `…:ui-tooling-data`. Наивный `in` показал две библиотеки как
   присутствующие в release-графе, чего в действительности нет.

Правильный метод — резолвить конфигурацию программно. Init-скрипт
печатает `ResolutionResult.allComponents` для
`releaseRuntimeClasspath` и `debugRuntimeClasspath`:

```
COORD lines: 394
resolved distinct: release=192  debug=202
```

Эти числа **совпали с числами вариантных JSON точно**, и сверка в обе
стороны дала нули:

| Контроль | Результат |
|---|---|
| библиотеки release-JSON, отсутствующие в resolved `releaseRuntimeClasspath` | **0** |
| библиотеки debug-JSON, отсутствующие в resolved `debugRuntimeClasspath` | **0** |

То есть генерируемый вариантный файл — это ровно runtime-граф варианта,
без приписок и без пропусков. Это и делает дальнейший вывод надёжным.

## L.1–L.2: дельта 227 − 192 = 35, поимённо и по конфигурациям

**Ни одна из 35 не присутствует в `releaseRuntimeClasspath`.**
Разбивка: 10 приходят из debug-графа, 25 не входят ни в один runtime-граф
(это `testImplementation` / `androidTestImplementation` / KSP-обвязка).

| Библиотека | relRT | dbgRT | Откуда |
|---|---|---|---|
| `androidx.compose.ui:ui-tooling` (+ `-android`) | нет | **да** | `build-logic/.../AndroidCompose.kt`, debug-тулинг Compose |
| `androidx.compose.ui:ui-tooling-data` (+ `-android`) | нет | **да** | транзитив ui-tooling |
| `androidx.compose.ui:ui-test-manifest` | нет | **да** | debug-тулинг Compose |
| `androidx.compose.material:material` (+ `-android`) | нет | **да** | транзитив debug-тулинга |
| `com.google.ai.edge.litertlm:litertlm-android` | нет | **да** | `core/mind/build.gradle.kts`, `debugImplementation` (ADR-020) |
| `com.google.code.gson:gson` | нет | **да** | транзитив litertlm |
| `org.jetbrains.kotlin:kotlin-reflect` | нет | **да** | транзитив litertlm |
| `androidx.compose.ui:ui-test`, `-android`, `-junit4`, `-junit4-android` | нет | нет | androidTest |
| `androidx.test:core`, `core-ktx`, `runner`, `monitor`, `annotation` | нет | нет | androidTest |
| `androidx.test.espresso:espresso-core`, `espresso-idling-resource` | нет | нет | androidTest |
| `androidx.test.ext:junit`, `androidx.test.services:storage` | нет | нет | androidTest |
| `androidx.multidex:multidex` | нет | нет | androidTest |
| `com.google.dagger:hilt-android-testing` | нет | нет | androidTest |
| `junit:junit`, `org.hamcrest:hamcrest-core/-library/-integration` | нет | нет | test |
| `com.google.truth:truth`, `com.google.auto.value:auto-value-annotations` | нет | нет | test |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` (+ `-jvm`) | нет | нет | test |
| `com.squareup:javawriter`, `org.ow2.asm:asm` | нет | нет | тулинг/KSP-обвязка |

**Вердикт L.2 — первая ветка постановки.** Все 35 — test / androidTest /
debugImplementation / тулинг. Юридически корректное число для release —
**192**; 227 раздуто на отладочную и тестовую обвязку. Дыры в атрибуции
нет: `release ⊆ committed` (0 библиотек в release-графе отсутствует в
закоммиченном экспорте), то есть закоммиченный файл **пере**-атрибутирует,
а не **недо**-атрибутирует. Пере-атрибуция никому не вредит; пропуск был
бы блокером публикации. Публикация **не блокируется**.

## L.3: чинится CI-проверка, а не число

Ловушка, которую стоит записать: **вариантные export-таски плагина не
дают поставляемое множество.** И `:app:exportLibraryDefinitions`, и
`:app:exportLibraryDefinitionsRelease` (запущенный с `--rerun-tasks`)
пишут в закоммиченный файл **227** записей — фильтр по варианту к пути
экспорта не применяется. Единственный артефакт с поставляемым множеством —
генерируемый ресурс `app/build/generated/aboutLibraries/release/res/raw/`.
Поэтому «просто перегенерировать закоммиченный файл под release» не
работает, и проверку пришлось писать самим.

Добавлен таск `:app:verifyReleaseLicenseAttribution` (`app/build.gradle.kts`)
с `dependsOn("generateLibraryDefinitionsRelease")` — без этого он на
чистом дереве краснеет на несгенерированном входе (проверено: первый
запуск дал именно такой ложный красный). Инвариант, который он держит, —
юридический, а не косметический: **всё, что реально едет пользователю,
должно быть покрыто публикуемой атрибуцией.** Надмножество допустимо,
пропуск — нет.

Фактический вывод на чистом дереве:

```
> Task :app:verifyReleaseLicenseAttribution
license attribution: committed=227 shipped(release)=192 committed-only=35
BUILD SUCCESSFUL
```

Проверка проверена отрицательным контролем — из закоммиченного экспорта
удалена настоящая release-зависимость `androidx.room:room-runtime`:

```
> Release ships 1 librar(ies) with no entry in the committed attribution
  — regenerate it (gradlew :app:exportLibraryDefinitions):
    - androidx.room:room-runtime
BUILD FAILED
```

После восстановления файла — снова зелено, дерево чистое. То есть таск
ловит именно то, ради чего написан, а не проходит вхолостую.

В CI (`.github/workflows/ci.yml`) старый шаг свежести **оставлен** — он
детектор изменения зависимостей и в этой роли полезен — и рядом добавлен
шаг «Release attribution covers everything that ships». Две проверки
отвечают на два разных вопроса, и подменять одну другой было исходной
ошибкой.

## Что этим НЕ сделано

- **Полнота текстов лицензий** (а не списка библиотек) не проверялась:
  таск сверяет множества `uniqueId`, не содержимое лицензий. → UNVERIFIED.
- **Экран лицензий вживую** на этом прогоне не открывался; что показывает
  UI, проверено только по артефакту. → остаётся в чеклисте.
- Числа сняты на текущем графе зависимостей. Любой бамп меняет их; ровно
  поэтому проверка теперь в CI, а не в отчёте.
