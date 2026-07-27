# Audit v1.0 → v1.1, Фаза 0 (read-only)

Дата: 2026-07-27. Ветка `main`, дерево чистое на момент аудита.
Метод: чтение исходников и артефактов в репозитории. Ни одна строка не
изменена. Всё ниже перепроверено самостоятельно; расхождения с
формулировками задания вынесены в §7 отдельно.

---

## 0.1 Состояние репозитория

| Факт | Значение | Источник |
|---|---|---|
| Ветка | `main` | `git rev-parse --abbrev-ref HEAD` |
| `git status` | пусто | `git status --short` |
| versionName | `1.0.0` | `app/build.gradle.kts:105` |
| versionCode | `10` | `app/build.gradle.kts:104` |
| Последний коммит | `79e5c24` | `git log --oneline -1` |

Последние 15 коммитов — прогон v0.9 (контейнер/лицензии/персона) и
финализация 1.0.0 (`83a081b`, `79e5c24`).

Тестов в исходниках (число аннотаций `@Test`, не число прогонов):
**268** всего, из них `./tools/litertlm-smoke/src/test` — 27, и они
env-gated (см. 0.5). Runtime-число будет взято из реального прогона
`check`, а не отсюда.

---

## 0.2 Точки входа в чат и в экран «Разум»

Ключевой факт: **отдельного экрана/маршрута чата не существует**. Чат —
это `ChatPanel`, встроенная в `HomeScreen` как один из слотов
`HomeAdaptiveScaffold`. Следовательно чат достижим ровно тем же
множеством путей, что и Home, то есть **всеми**.

| Файл:строка | Поверхность | Как достижимо |
|---|---|---|
| `feature/home/src/main/kotlin/.../HomeScreen.kt:325-346` | `chat` слот собирается на Home | всегда, безусловно |
| `.../HomeScreen.kt:404` | чат в правой панели (expanded ≥840dp) | планшет/раскрытый фолд |
| `.../HomeScreen.kt:416` | чат под существом (compact) | телефон |
| `.../HomeScreen.kt:740` | поле ввода `home.chat.input` | тап по Home |
| `.../HomeScreen.kt:761` | кнопка отправки `home.chat.send` | тап по Home |
| `app/src/main/kotlin/app/anima/AnimaRoot.kt:96` | `startDestination = HOME` | запуск приложения |
| `app/src/main/kotlin/app/anima/AnimaRoot.kt:100` | онбординг → Home | завершение онбординга |
| `app/src/main/res/xml/shortcuts.xml:7-15` | шорткат **Talk** (`shortcutId="talk"`) | долгий тап по иконке лаунчера |
| `feature/widget/src/main/kotlin/.../AnimaWidget.kt:72,85` | тап по виджету → launch intent → Home | домашний экран |
| `feature/rest/src/main/kotlin/.../RestTileService.kt:38,53` | QS-тайл → `ACTION_REST` → Rest | шторка (на Rest, не на чат; но back → Home) |
| `AnimaRoot.kt:89` + `shortcuts.xml:17-25` | шорткат Diary → Diary | back → Home |

Шорткат `Talk` не имеет собственного действия: его intent — `MAIN` на
`MainActivity` (`shortcuts.xml:11-13`), то есть он просто открывает Home,
где чат и находится. Отдельного `ACTION_OPEN_CHAT` в коде нет.

Экран «Разум» (`MindScreen`) уже живёт **внутри Настроек**:

| Файл:строка | Поверхность |
|---|---|
| `feature/settings/src/main/kotlin/.../SettingsScreen.kt:359` | кнопка `settings_mind_open` → `Routes.MIND` |
| `app/src/main/kotlin/app/anima/AnimaRoot.kt:121,134-136` | маршрут `mind` |

То есть требование 2.2 наполовину уже выполнено структурно: перенос не
нужен, нужен только гейт «выключено по умолчанию».

Уведомления: `feature/notifications` **не создаёт** PendingIntent в чат —
единственный intent в модуле открывает системный экран Notification
Access (`NotificationsViewModel.kt:166`). Вход в чат из уведомлений
отсутствует.

---

## 0.3 Инвентаризация тела

**Рендер существа:** `core/creature`.
Живой экран — `CreatureSurface.kt` + `CreatureController.kt`;
внеэкранный кадр — `render/StillRender.kt` (виджет и живые обои).

**8 обликов** (`core/model/.../CreatureConcept.kt`), по одному рендереру:

| Concept | Рендерер |
|---|---|
| `SPIRIT_ORB` | `render/SpiritOrbRenderer.kt` |
| `FOX_KIT` | `render/FoxKitRenderer.kt` |
| `JELLY` | `render/JellyRenderer.kt` |
| `PIXEL_PET` | `render/PixelPetRenderer.kt` |
| `ROBOT` | `render/RobotRenderer.kt` |
| `SPROUT` | `render/SproutRenderer.kt` |
| `EMBER` | `render/EmberRenderer.kt` |
| `MOTH` | `render/MothRenderer.kt` |

**7 состояний** (`core/model/.../Mood.kt`): `ALERT`, `BORED`, `SLEEPY`,
`EATING`, `ANXIOUS`, `ASLEEP`, `HOT`.

**Голдены:** `core/creature/src/test/screenshots/rig/`, 32 PNG —
8 обликов × 4 состояния (`alert-day`, `asleep-night`, `eating-charge`,
`sleepy-low`). Пинятся `RigGoldenTest.kt:53` (один `@Test`, внутри цикл
по всем комбинациям). Голдены пинят именно `StillRender`, то есть код
виджета и обоев, а не экранный путь.

**Источники сигналов устройства:** `core/body/BodySensors.kt`,
`core/body/WeatherFeel.kt`; типы — `core/model/BodySignals.kt`
(`ChargeKind`, `NetSense`, `ThermalSense`).

### 🔴 Находка 0.3-A — статичный кадр не отражает состояние

Хэши голденов (`md5sum core/creature/src/test/screenshots/rig/*.png`)
показывают, сколько **различных** изображений приходится на 4 состояния:

| Concept | Различных кадров из 4 | Что совпадает |
|---|---|---|
| `JELLY` | **1** | все четыре байт-в-байт идентичны |
| `SPROUT` | **1** | все четыре байт-в-байт идентичны |
| `SPIRIT_ORB` | 2 | alert = asleep = sleepy |
| `EMBER` | 2 | alert = eating = sleepy |
| `MOTH` | 2 | alert = eating = sleepy |
| `PIXEL_PET` | 2 | alert = eating = sleepy |
| `FOX_KIT` | 3 | alert = eating; asleep = sleepy |
| `ROBOT` | **4** | единственный, где все состояния различны |

Для пяти обликов из восьми виджет — фактически постоянная картинка.
Это прямо противоречит цели 4.3 («что с телефоном прямо сейчас»).

**Корневая причина найдена, `core/creature/.../render/StillRender.kt:36-40`:**

```kotlin
val engine = CreatureEngine(seed, genome)
engine.setReducedMotion(true)   // ← settleToStatics() при mood = ALERT (дефолт)
engine.setMood(mood)            // ← только выставляет target'ы пружин
engine.onResume()               // ← lastFrameNanos = Long.MIN_VALUE
engine.advance(ONE_TICK_NANOS)  // ← первая ветка: якорит время и выходит
```

Две независимые ошибки складываются:

1. `CreatureEngine.advance` (`engine/CreatureEngine.kt:161-164`) при
   `lastFrameNanos == Long.MIN_VALUE` только запоминает время и делает
   `return`. После `onResume()` это ровно наш случай — **симуляция не
   выполняется ни разу**. KDoc `StillRender` утверждает «advanced exactly
   one tick»; фактически ноль тиков.
2. Из-за (1) не вызывается `advanceReduced`
   (`CreatureEngine.kt:366-381`) — а это единственное место, где поза
   получает `pose.energy = profile.energy`, `pose.lidDroop =
   profile.lidClosure`, `pose.flush`. `settleToStatics` (:383) эти три
   канала не трогает, и вызывается он **до** `setMood`.

Итог: поза статичного кадра всегда «ALERT по умолчанию» независимо от
mood. Различия между кадрами возникают только там, где рендерер сам
читает `mood`/`night`/`charging` напрямую — у `ROBOT` читает, у `JELLY`
и `SPROUT` не читает вовсе.

Наблюдаемое подтверждение: `ember-asleep-night.png` — глаза открыты,
хотя `MoodProfile` для `ASLEEP` задаёт закрытые веки.

Исправление ожидается однострочным (снять преждевременный `onResume()`
либо якорить время отдельным вызовом), но голдены при этом изменятся
массово — и это как раз то «осознанное принятие диффа», о котором
говорит 3.4.

### 🟠 Находка 0.3-B — облики несопоставимы по качеству

Просмотр всех восьми `*-alert-day.png` (изображения читались, не
описывались по имени файла):

- Сильные: `ROBOT` (закончен, контрастен, деталь-заряд в теле),
  `FOX_KIT` (обаятелен, заполняет кадр), `SPIRIT_ORB` (мягкий, читаемый).
- Средние: `JELLY` (силуэт хорош, но щупальца — тонкие однотонные
  линии), `PIXEL_PET` (сильная графическая идея, но лицо читается как
  «уши-рога», кислотно-зелёный).
- Слабые: **`SPROUT`** — генерическая ромашка на коричневом эллипсе,
  глаза-точки, «почва» читается как грязь; **`EMBER`** — фигура занимает
  ~25% кадра, силуэт читается как лист/щит, очень светлая заливка (на
  светлой подложке контраст почти отсутствует); **`MOTH`** — коричневое
  тело, мелкий масштаб.

> **Поправка к этому разделу (внесена после Фазы 1).** Светлый фон на
> голденах — **не часть кадра**. `RigGoldenTest` захватывает не bitmap, а
> Compose-узел `Image` внутри окна Robolectric
> (`RigGoldenTest.kt:57-65`), и `#FAFAFA` — это фон тестового окна.
> Подтверждение того, что сам кадр прозрачен, — `WidgetSnapshotDeviceTest.kt:36-37`:
> тест считает пиксели с **ненулевой альфой** и проверяет `> 0`, что имеет
> смысл только на прозрачном холсте. Ни один рендерер не заливает кадр
> целиком (единственные `drawRect` — ограниченный glow в `GlowSkin.kt:58`
> и клетки пиксель-сетки). Оценка силы/слабости обликов выше остаётся в
> силе, но читать её надо как «на светлой подложке»; на тёмной картина
> другая и никем ещё не виденная.

Вторая, независимая от стиля проблема: **разнобой масштаба**. `FOX_KIT`
и `PIXEL_PET` заполняют кадр, `EMBER`/`MOTH`/`SPROUT` висят мелкими в
центре. Выбора у пользователя нет (облик назначается), значит по
критерию 3.2 минимум три облика сейчас «проигрышные».

---

## 0.4 Виджет Glance и живые обои

### Виджет

| Параметр | Значение | Источник |
|---|---|---|
| Размер по умолчанию | `targetCellWidth=2`, `targetCellHeight=2` | `anima_widget_info.xml:10-11` |
| Минимум | 110×110 dp | `anima_widget_info.xml:8-9` |
| Resize | `horizontal\|vertical` | `anima_widget_info.xml:12` |
| Период обновления | `1800000` мс (30 мин) | `anima_widget_info.xml:13` |
| Категория | `home_screen` | `anima_widget_info.xml:14` |
| **`previewImage` / `previewLayout`** | **отсутствуют** | тот же файл |
| `description` | отсутствует | тот же файл |
| `maxResizeWidth/Height` | отсутствуют | тот же файл |
| Конфигурационная активность | нет | тот же файл |

Отрисовка: `AnimaWidget.kt:61-70` — один bitmap 512×512
(`WidgetSnapshot.SIZE_PX`) через `StillRender.tile`, вставленный как
`Image(ContentScale.Fit)`. Никакой Glance-разметки, никакого текста, кроме
`contentDescription` (`AnimaWidget.kt:77-83`). То есть требование 4.3
(«никакого текста, который надо читать») уже соблюдено — но ценой того,
что и информации в кадре почти нет (см. 0.3-A).

Данные: `WidgetSnapshot.readVitals` (`WidgetSnapshot.kt:26-31`) читает
`BatteryManager` **в момент отрисовки**; `night` — из `Calendar`
(`AnimaWidget.kt:58-59`); облик/сид/имя — из `IdentityRepository`, каждый
под `runCatching` с дефолтами (`SPIRIT_ORB`, seed `0L`, `"Anima"`).

Триггеры обновления (`WidgetRefresh.kt`): 30-минутный системный
heartbeat + два одноразовых WorkManager-задания
(`requiresCharging`, `requiresBatteryNotLow`), перевзводящиеся после
срабатывания. Признанный в ADR-007 пробел: у «батарея села» нет
отрицательного constraint, этот фронт ждёт heartbeat.

### 🔴 Находка 0.4-A — при недоступности данных виджет врёт молча

Отдельной ветки «данные недоступны» **не существует**. Если процесс не
поднимается (см. 1.2 — Samsung deep sleep), `provideGlance` не
вызывается, и на домашнем экране остаётся **последний отрисованный
bitmap** — с процентом батареи, снятым неизвестно когда, и без единого
признака устаревания. Ни метки времени, ни поля «когда снято» в
`WidgetSnapshot.Vitals` нет. Это именно тот сценарий, который 1.2
называет «пустым или врущим»; сейчас — врущим.

Смягчающее обстоятельство: `readVitals` читает батарею синхронно при
отрисовке, поэтому *свежий* кадр никогда не устаревший. Проблема
исключительно в том, что кадр может не обновляться сутками.

### Живые обои

`feature/wallpaper/AnimaWallpaperService.kt` + `WallpaperBudget.kt`.
Бюджет — «0 fps в покое» в строгой форме: `allowsDraw` разрешает кадр
только при `visible`, причём для причины `STATE` не чаще одного раза в
`MIN_STATE_REDRAW_INTERVAL_MS = 30_000` мс; причина `SURFACE`
(создание/поворот/появление) разрешена всегда. Тест —
`WallpaperBudgetTest.kt` (3 `@Test`). Отрисовка использует тот же
`StillRender`, значит находка 0.3-A касается и обоев.

---

## 0.5 Приёмочный слой и все SKIPPED

**Подтверждаю:** `PersonaAcceptanceTest` пропускается в обычном прогоне.
`tools/litertlm-smoke/src/test/.../PersonaAcceptanceTest.kt:55` —
`assumeTrue("gated: set ANIMA_PERSONA_ACCEPT=1", System.getenv(...) == "1")`
в `@Before`, то есть JUnit рапортует SKIPPED, а не PASSED.

Полный список гейтов в проекте (`grep -rn "@Ignore\|assumeTrue\|Assume\."`):

| Файл:строка | Гейт |
|---|---|
| `tools/litertlm-smoke/.../PersonaAcceptanceTest.kt:55` | `ANIMA_PERSONA_ACCEPT=1` |
| `tools/litertlm-smoke/.../LiteRtLmJvmSmokeTest.kt:29` | `ANIMA_JVM_LLM_SMOKE=1` |
| `tools/litertlm-smoke/.../LocalMindBenchTest.kt:64,76` | `ANIMA_JVM_LLM_BENCH` + непустой список моделей |
| `core/mind/src/androidTest/.../AndroidRuntimeReadsLitertlmTest.kt:53` | условие среды |
| `core/mind/src/androidTest/.../MindBakeoffHarness.kt:77` | условие среды |

Аннотаций `@Ignore` / `@Disabled` в проекте нет — все пропуски
сделаны через `Assume`, то есть видимы в отчёте как SKIPPED.

---

## 0.6 Состав релизного бандла

**`:mind-pack` входит в релизный артефакт**: `app/build.gradle.kts:158`,
`assetPacks += ":mind-pack"`; модуль подключён в
`settings.gradle.kts` и объявлен как `fast-follow`
(`mind-pack/build.gradle.kts`).

Существенная поправка к состоянию «сейчас»: **слот пака пуст**. В
`mind-pack/src/main/assets/` лежат только `README.md` и
`MODEL_NOTICES.txt`; сам `.task`/`.litertlm` не в git (`du -sh mind-pack/`
→ 18K). Поэтому измерение «размер бандла без пака» на текущем дереве
покажет практически базу и без изменений — исключение пака здесь
структурное решение, а не немедленная экономия байт.

Историческое измерение при заполненном слоте
(ADR-021, `build/runlogs/a2-getsize.log`, bundletool 1.18.1):

| Измерение | Байт |
|---|---|
| `get-size total --modules=mind_pack` (пак + base), MAX | 1 393 113 554 |
| `get-size total --modules=base`, MAX | 15 903 078 |
| пак сам по себе (вычитанием) | ≈ 1 377 210 476 |

`bundletool-all.jar` найден на машине: `D:\Hermes-tmp\bundletool-all.jar`
(32.5 МБ), вне репозитория. Свежий замер под 2.4 будет сделан этим
инструментом с логом в `build/runlogs/`.

---

## 7. Расхождения с формулировками задания

1. **«`NetworkIsolationTest` (13 тестов)»** — в исходнике **14**
   аннотаций `@Test` (`app/src/test/kotlin/app/anima/NetworkIsolationTest.kt`,
   строки 56, 69, 102, 122, 137, 152, 186, 202, 216, 241, 267, 305, 329,
   346). Число 13 неверно; актуальная цифра будет подтверждена выводом
   прогона, а не подсчётом аннотаций.
2. **«демонтаж чата»** предполагает отдельный экран чата — его нет.
   Чат встроен в `HomeScreen`, поэтому «сделать недостижимым» означает
   не убрать маршрут, а вынуть слот из Home и создать отдельный
   гейтированный маршрут. Объём работ больше, чем «спрятать кнопку».
3. **«Экран „Разум“ уходит туда же»** — `MindScreen` уже находится
   внутри Настроек (`SettingsScreen.kt:359`). Требуется только флаг
   «выключено по умолчанию».
4. **«Онбординг: три экрана»** — их уже три
   (`HATCH → CHOOSE → NAME`, `OnboardingScreen.kt:78-91`). Меняется
   только финальный: имя → установка виджета. Имя при этом должно куда-то
   переехать, иначе теряется — это решение выносится в план Фазы 1.
5. **Контактный лист 8×4** частично уже существует как 32 голдена; но
   ни темы, ни подложки в них нет: `StillRender` принимает `night`, а сам
   кадр прозрачен (см. поправку в 0.3-B), и всё, что видно на голденах
   вокруг существа, — фон тестового окна. Контактный лист на реальных
   подложках потребует расширения рига захвата, а не контракта
   `StillRender`.
