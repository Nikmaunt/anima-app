# Аудит v0.4 — независимая read-only проверка

Дата: 2026-07-18. HEAD: `3959610` (main, «v0.4.0: version bump + stale-check reruns (DoD verifier findings)»).
Метод: каждое утверждение подкреплено чтением файла (путь:строка) или командой; ничего не взято из памяти
предыдущих сессий. Код не изменялся; единственный созданный файл — этот.

---

## 1. WAL-фикс (провал v0.2/v0.3 «зануления ключа»)

**Вердикт: фикс на месте и запинен девайс-тестом.**

- `core/data/src/main/kotlin/app/anima/core/data/crypto/SoulKeyHolder.kt:28-33` — единственное состояние:
  `@Volatile private var bytes: ByteArray?` + `@Synchronized fun passphrase()` с кэшированием навсегда.
  Никакого `fill(0)` / зануления нет (grep по `fill|zero|wipe|Arrays` в `core/data/src/main` — только
  комментарии, объясняющие ПОЧЕМУ зануление убрано). KDoc (строки 9-20) фиксирует причину: sqlcipher-android
  алиасит массив и перечитывает его при открытии каждого нового физического соединения пула
  (`SQLiteConnection.open()` → `nativeKey`), проверено по байткоду 4.17.0.
- `core/data/src/main/kotlin/app/anima/core/data/crypto/SoulVaultWarmer.kt:9-14` — прогрев БД на старте
  явно НЕ скрабит ключ («The passphrase is NOT scrubbed afterwards»).
- `core/data/src/main/kotlin/app/anima/core/data/di/DataModule.kt:36-43` — фабрика получает
  `keyHolder.passphrase()` напрямую, с комментарием про WAL pool growth.
- **WalPoolKeyDeviceTest**: `core/data/src/androidTest/kotlin/app/anima/core/data/WalPoolKeyDeviceTest.kt`.
  Что пинит (строки 59-105): БД открывается с `JournalMode.WRITE_AHEAD_LOGGING` (строка 67, явно, чтобы
  форсировать рост пула и на эмуляторе), тест-тред держит write-транзакцию на primary-соединении
  (строка 81), параллельный тред читает через DAO (строки 85-96) — это требует ВТОРОГО физического
  соединения, которое sqlcipher ключует из того же массива. До фикса это давало
  SQLiteNotADatabaseException; тест падает, если ритуал зануления вернётся. Проверка
  `isWriteAheadLoggingEnabled` (строка 74) защищает тест от вакуумного прохода без WAL.
- **sqlcipher**: `gradle/libs.versions.toml:36` — `sqlcipher = "4.17.0"` (ожидание подтверждено);
  комментарий на строке 33 привязывает бамп к CVE-2025-29087/6965.

## 2. Бюджет разрешений

**Вердикт: соблюдён. INTERNET ровно в двух модулях; UsageStats/контактов/локации/аналитики нет нигде.**

Исходные (не build/) манифесты — 8 штук; фактические `uses-permission` по модулям:

| Модуль | Разрешения | Где |
|---|---|---|
| `app` | ACCESS_NETWORK_STATE, VIBRATE | `app/src/main/AndroidManifest.xml:18-19` |
| `core/model-delivery` | INTERNET | `core/model-delivery/src/main/AndroidManifest.xml:16` |
| `core/cloud-mind` | INTERNET | `core/cloud-mind/src/main/AndroidManifest.xml:12` |
| `feature/notifications` | — (сервис под системным BIND_NOTIFICATION_LISTENER_SERVICE) | `feature/notifications/src/main/AndroidManifest.xml:10-17` |
| `feature/widget` | — (receiver APPWIDGET_UPDATE) | `feature/widget/src/main/AndroidManifest.xml:11-21` |
| `feature/rest` | — (QS-tile под BIND_QUICK_SETTINGS_TILE) | `feature/rest/src/main/AndroidManifest.xml:6-18` |
| `feature/wallpaper` | — (сервис под BIND_WALLPAPER) | `feature/wallpaper/src/main/AndroidManifest.xml:6-17` |
| `core/voice` | — (пустой манифест, ADR-013: TTS в чужом процессе) | `core/voice/src/main/AndroidManifest.xml:7` |

- Телеметрия ML Kit (DataTransport) вырезана `tools:node="remove"`: `app/src/main/AndroidManifest.xml:30-38`.
- Смёрженный бюджет запинен точным allowlist в `app/src/test/kotlin/app/anima/NetworkIsolationTest.kt:70-100`:
  свои три (INTERNET, ACCESS_NETWORK_STATE, VIBRATE) + библиотечный осадок (FOREGROUND_SERVICE,
  FOREGROUND_SERVICE_DATA_SYNC, WAKE_LOCK, RECEIVE_BOOT_COMPLETED, aicore BIND_SERVICE,
  DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION). Любое НОВОЕ разрешение валит сборку.
- Тест «INTERNET ровно в двух модульных манифестах»: NetworkIsolationTest.kt:123-135
  (`containsExactlyElementsIn` по списку из строк 24-28).

## 3. Голдены Roborazzi

**Вердикт: 38 голденов, все закоммичены 2026-07-18 (свежие, одним днём — см. Риски).**

| Каталог | Кол-во | Что покрывает | Последний коммит |
|---|---|---|---|
| `core/creature/src/test/screenshots/rig/` | 32 | 8 существ (ember, fox_kit, jelly, moth, pixel_pet, robot, spirit_orb, sprout) × 4 состояния (alert-day, asleep-night, eating-charge, sleepy-low) | `8f96dad` 2026-07-18 |
| `core/ui/src/test/screenshots/ds/` | 2 | design-system light/dark | `8f96dad` 2026-07-18 |
| `feature/rest/src/test/screenshots/rest/` | 4 | pick-light, pick-dark, running-dark, completed-light | `42113f5` 2026-07-18 |

Тесты-генераторы: `core/creature/src/test/.../RigGoldenTest.kt`, `core/ui/src/test/.../DesignSystemGoldenTest.kt`,
`feature/rest/src/test/.../RestContentGoldenTest.kt`. Плагин roborazzi 1.69.0 (`gradle/libs.versions.toml:73`),
подключён в build.gradle.kts модулей creature/ui/rest. Экраны home/settings/soul/onboarding голденами НЕ покрыты.

## 4. Граница LocalMindEngine (ADR-011 data scope)

**Вердикт: граница держится типами DI; digest запинен тестом, diary retell — НЕТ (см. Расхождения).**

- Тип: `core/model/src/main/kotlin/app/anima/core/model/Mind.kt:89` — `interface LocalMindEngine : MindEngine`
  (KDoc 82-88: «вид разума, который никогда не выберет облачный tier»).
- DI: `core/mind/src/main/kotlin/app/anima/core/mind/di/MindModule.kt:24-26` —
  `@Provides fun localMindEngine(impl: TieredMindEngine) = impl.localOnly`.
- Реализация: `core/mind/src/main/kotlin/app/anima/core/mind/TieredMindEngine.kt:95-101` —
  `localOnly = object : LocalMindEngine` с `active(includeCloud = false)`: CLOUD пропускается даже когда
  включён и онлайн.
- Потребители: `feature/notifications/.../NotificationsViewModel.kt:45` — `private val mind: LocalMindEngine`
  (комментарий 43-44: «notification titles must never…»);
  `feature/home/.../BodyDiaryScreen.kt:93` — diary-retell VM тоже `LocalMindEngine` (комментарий 91-92,
  audit-v03 F1b).
- Cloud-mind контента не получает: `CloudMindEngine` живёт только за `TieredMindEngine`; статический
  трипвайр NetworkIsolationTest.kt:102-120 проверяет, что NotificationsViewModel содержит `LocalMindEngine`,
  не содержит `val mind: MindEngine`, и ни один .kt файла модуля notifications не упоминает `CloudMindBackend`.
- **Пробел**: трипвайр покрывает только `feature/notifications`; аналогичной проверки для diary retell
  (`feature/home/BodyDiaryScreen.kt`) нет — граница там держится только на типе поля.

## 5. Анимационные бюджеты (ADR-012)

**Вердикт: 0 fps steady-state реализован; policy-объект оттестирован; сервис-уровень — только косвенно.**

- `feature/wallpaper/src/main/kotlin/app/anima/feature/wallpaper/WallpaperBudget.kt:8-37` —
  `MIN_STATE_REDRAW_INTERVAL_MS = 30_000L` (строка 10), `allowsDraw(reason, visible, now, last)`:
  невидимый — никогда; SURFACE — всегда; STATE — не чаще одного кадра в 30 с.
- `AnimaWallpaperService.kt:49-51` — `onCreateEngine(): Engine = StillEngine()`; каждый redraw проходит
  через гейт `WallpaperBudget.allowsDraw(...)` (строка 111). В файле НЕТ Choreographer / postDelayed /
  animator (grep пуст) — петли кадров физически нет, redraw = одиночный кадр.
- Тесты: `feature/wallpaper/src/test/.../WallpaperBudgetTest.kt` — 3 теста: невидимый не рисует (8-15),
  SURFACE всегда рисует при видимости (17-22), STATE-дебаунс ровно 30 с (24-40).
- Чего нет: unit-теста на сам StillEngine (что каждый вызов draw идёт через гейт) — это ловится только
  ручным чек-листом §0b (ночь с обоями).

## 6. Локализационный долг (ADR-014)

**Вердикт: долг фактически ВЕСЬ на месте, отдача перенесена в v0.5; инфраструктура частична.**

Числа (grep по main-исходникам, без build/):
- `stringResource(` — **0** использований во всём проекте.
- Литералы `Text("...")` в composables — **24**: home 4, notifications 3, onboarding 1, settings 11, soul 5.
- Более широкая оценка англоязычных фраз-литералов в UI-модулях (feature/* + core/model|creature|voice,
  паттерн «слово пробел слово») — **~386** (верхняя граница, включает немного не-UI); ADR-014 оценивает
  долг в ~240 — порядок совпадает.
- `strings.xml` существуют ровно 3, суммарно **7 строк**: `feature/rest` 1 (rest_tile_label),
  `feature/wallpaper` 2, `app` 4 (в основном ярлыки для манифестов).
- Каталогов `res/values-*` (локалей) — **0**. Ни RU, ни одной из «шести локалей» решения ADR-014 в коде нет.
- Псевдолокали: `app/build.gradle.kts:24` — `isPseudoLocalesEnabled = true` в debug (комментарий 21-23
  привязывает к ADR-014). Это единственная реально существующая часть «инфраструктуры».

## 7. Версия и mind-pack

- `app/build.gradle.kts:14-15` — `versionCode = 4`, `versionName = "0.4.0"`.
- `mind-pack/build.gradle.kts` — плагин `com.android.asset-pack`, `packName = "mind_pack"`,
  `deliveryType = "fast-follow"`. В git модель НЕ лежит: `mind-pack/src/main/assets/` содержит только
  `GEMMA_NOTICE.txt` и `README.md` (владелец кладёт лицензированный `.task` руками перед сборкой бандла).
- Модель: Gemma 3 1B int4 через MediaPipe LLM Inference —
  `core/mind/src/main/kotlin/app/anima/core/mind/GemmaMindEngine.kt:13-14,31,40`.
- Слот доставки: `core/model-delivery/.../MindModelResolver.kt:57-86` — `MindModelLocator`; приоритет:
  пользовательский файл (download/SAF) > pack; RAM-гейт ~5.2 GB (`RamGate`, строки 31-50, порог
  `52L*1024^3/10`) + override `force_gemma_below_ram_gate` (строка 67). Pack как StateFlow:
  `PackModelSource.kt:129` (`PACK_NAME = "mind_pack"`).

## 8. Тесты

**Unit (23 файла)**: app — NetworkIsolationTest; core/cloud-mind — CloudKeyVaultTest, SseChatTest;
core/creature — CreatureSurfaceUiTest, RigGoldenTest, CreatureEngineTest; core/data — AnimaDatabaseTest,
SoulBackupCodecTest; core/mind — MindTest; core/model — CareTest, DreamAndChartTest, EvolutionTest,
MilestonesTest, MoodEngineTest, NotifCaptureFilterTest, RestTest, SeedAndPromptTest, SoulPortTest;
core/ui — DesignSystemGoldenTest, PaletteContrastTest; core/voice — VoiceCharacterTest;
feature/rest — RestContentGoldenTest; feature/wallpaper — WallpaperBudgetTest.

**Instrumented (5)**: core/data — SchemaMigrationDeviceTest, SoulVaultDeviceTest, **WalPoolKeyDeviceTest**,
crypto/KeystoreSoulKeySourceDeviceTest; feature/widget — WidgetSnapshotDeviceTest.

**NetworkIsolationTest (v4, 10 тестов)** — `app/src/test/kotlin/app/anima/NetworkIsolationTest.kt`:
merged-манифест обязан существовать (отсутствие = FAIL, закрыт вакуумный проход v0.1; строки 42-46),
release-вариант проверяется когда собран (47-52); телеметрия вырезана (56-67); точный allowlist разрешений
(70-100); digest → только LocalMindEngine (102-120); INTERNET ровно в 2 модульных манифестах (123-135);
запрещённые сетевые API в исходниках вне пары (needle-список из 14, включая openStream/SocketChannel/
WebView/DownloadManager; 138-169); каталог версий без сетевых стеков (172-183); координаты зависимостей
по всем .gradle.kts (186-208); allowlist зависимостей notifications (211-234) и cloud-mind (237-258);
cloud-mind без логирования (261-275); soul-export не трогает cloud.key/CloudKeyVault (278-292).

## 9. detekt / ktlint

- Корневой `build.gradle.kts:15-16` — плагины detekt+ktlint `apply false`; строки 25-36 — применяются к
  КАЖДОМУ модулю (`apply(plugin=...)` в subprojects-блоке), detekt-конфиг из
  `config/detekt/detekt.yml` (существует, 55 строк, «zero findings enforced, detekt в check»).
- ktlint: движок 1.7.1 (`gradle/libs.versions.toml:64`), корневой `.editorconfig` на месте.

## 10. Расхождения (docs ↔ код)

1. **ADR-014 «routing policy шипится в v0.4» — не реализовано.** Amendment (ADR-014:57-61) утверждает, что
   v0.4 отдаёт «…and this ADR's routing policy». Типа `MindLanguage` в коде НЕТ (grep по всем .kt — 0
   совпадений); ни в PromptBuilder, ни в TieredMindEngine нет языковой маршрутизации. Реально отдана
   только псевдолокаль-инфраструктура + store-тексты.
2. **ADR-014 «new modules (rest, wallpaper) resource-based from birth» — неверно для rest.**
   `feature/rest/src/main/kotlin/app/anima/feature/rest/RestScreen.kt` хардкодит ~13 строк продуктовой
   копии («you're back — I kept your place»:218, «rest with me while I eat?»:275, «leave anytime…»:308 и
   др.); в ресурсах у rest только ярлык тайла. Wallpaper (2 строки в res, UI-копии нет) — соответствует.
3. **Трипвайр LocalMindEngine покрывает не всех потребителей.** NetworkIsolationTest.kt:102-120 пинит
   только `feature/notifications`; diary retell (`feature/home/BodyDiaryScreen.kt:93`, audit-v03 F1b) тем
   же тестом не защищён — рефакторинг поля на `MindEngine` пройдёт сборку молча.
4. **Checklist v4 §5/§6 «if shipped in the final tree»** — оба ШИПНУТЫ: milestones
   (`core/model/.../Milestones.kt`, `feature/settings/.../WardrobeScreen.kt`) и postcard
   (`feature/soul/.../SoulScreen.kt`, `SoulViewModel.kt`). Условность в чек-листе устарела — пункты
   обязательны.
5. **Checklist v4 §7 «Локализация (если вошла…)» — не вошла** (0 локалей, см. §6) — пункт мёртвый для
   v0.4, переносится в v0.5 как есть.
6. Мелкое: handoff.md и манифест app ссылаются на «NetworkIsolationTest v3», сам файл — уже v4
   (заголовок NetworkIsolationTest.kt:9). Косметика, но при следующей правке комментариев стоит выровнять.
7. handoff.md:21-23 (Gemma `.task` не в git; dummy удалён) — подтверждено содержимым
   `mind-pack/src/main/assets/` (только README + NOTICE). Расхождения нет — фиксирую как проверенное.

## 11. Риски v0.5

1. **Строковая миграция — крупнее оценки.** ~240 по ADR-014, фактическая верхняя граница ~386 литералов,
   из них многие рождаются в ViewModel'ях (нужен контекст/ресурсы в VM — правка сигнатур). RestScreen
   придётся мигрировать вопреки «resource-based from birth». Риск: частичная локаль (сам ADR называет её
   «хуже, чем никакой»).
2. **Незапиненная граница diary→local.** До начала любых рефакторингов mind-слоя стоит расширить
   digest-трипвайр на feature/home (или на ВСЕХ потребителей `LocalMindEngine` разом), иначе повторение
   истории «зелёные тесты ≠ работает».
3. **Ключ живёт в памяти процесса весь uptime** (осознанный трейд-офф ADR-003 v0.4). Любая v0.5-попытка
   «улучшить безопасность» повторным занулением вернёт WAL-краш; WalPoolKeyDeviceTest — единственный
   страж, гонять его в GMD обязательно (с `ANDROID_AVD_HOME=D:/Android/avd`, handoff.md:19-20).
4. **Голдены отрендерены одним днём на одной машине** (все коммиты 2026-07-18). Смена JDK/Robolectric/
   шрифтов даст массовый дифф; фиксация окружения рендера (или запись terms в README тестов) — до
   первого чужого CI.
5. **NetworkIsolationTest требует собранного merged-манифеста** (падает без `assembleDebug`) — в чистом
   CI порядок задач обязан быть закреплён, иначе красный тест по инфраструктурной причине.
6. **Store-блокер**: без лицензированного `.task` в `mind-pack/src/main/assets/` релизный бандл едет без
   разума; шаг остаётся ручным и нигде не проверяется автоматически.
7. **Непроверенное на железе** (checklist v4 §0/§0b): скорость Gemma на Exynos 2400 и ночь батареи с
   живыми обоями. Обе — go/no-go для ADR-010/012; закрыть до любых новых фич v0.5.
8. **Голдены не покрывают home/settings/soul/onboarding** — именно эти экраны больше всех тронет
   строковая миграция v0.5; без голденов дифф-контроль миграции будет слепым.
