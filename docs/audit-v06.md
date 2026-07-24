# Аудит v0.6 — свежий read-only проход (v0.7 Фаза 0, 2026-07-24)

Метод: каждое заявление CHANGELOG/handoff/README v0.6 сверено с кодом
(путь к файлу обязателен); полный локальный контур прогнан заново; впервые
проведён «проход глазами» на живом эмуляторе (AVD google_apis 34 на D:,
свежая установка debug-APK, прогон онбординга и всех основных экранов) и
просмотр всех 46 голденов как изображений. Традиция аудитов: v0.1 нашёл
незанулённую passphrase, v0.2 — фантомные тесты, v0.3 — незакоммиченное
дерево, v0.4 — маршрутизацию без кода, v0.5 — капсулы без FLAG_SECURE,
v0.6 — отсутствие иконки. Гипотеза «v0.6 тоже что-то заявил без
реализации» проверена по каждому пункту ниже.

## CONFIRMED — каждое заявление v0.6 найдено в коде

| Заявление v0.6 | Доказательство (путь) |
|---|---|
| targetSdk 36 / compileSdk 36 | `build-logic/src/main/kotlin/AnimaAndroidApplicationConventionPlugin.kt:20`, `KotlinAndroid.kt:20` |
| Подписание: upload-key вне репо | `app/build.gradle.kts:42-74` (`../anima-keys/keystore.properties`, graceful absence) |
| Реальный CI | `.github/workflows/ci.yml` — jobs `checks` (detekt/ktlint, units, Roborazzi, licenses-freshness, lintVital, debug+bundle) и `gmd` (atd34 c одним retry) |
| Экран OSS-лицензий | `feature/settings/src/main/kotlin/.../LicensesScreen.kt`, маршрут `app/src/main/kotlin/app/anima/AnimaRoot.kt:129`; вживую: «Anima is built on 197 open source libraries», список рендерится |
| «Пожаловаться на ответ» | UI `feature/home/.../HomeScreen.kt:499-527` (testTag `home.chat.report`), логика `HomeViewModel.kt:602-607` — chat.remove + `JournalKind.REPLY_FLAGGED`; строки во всех 6 локалях (`home_report_reply`) |
| EncryptedUpgradeDeviceTest | `core/data/src/androidTest/kotlin/.../EncryptedUpgradeDeviceTest.kt` — v1-файл, созданный самим SQLCipher (schema 1 DDL дословно + identity hash), прогнан через продакшен-билдер c MIGRATION_1_2, integrity_check, проверка что файл остался шифротекстом |
| Адаптивная + monochrome иконка | `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` (background/foreground/monochrome), все 3 drawable существуют; вживую: иконка в app drawer и hotseat — орб, не платформенный робот |
| FLAG_SECURE на капсулах | общий `core/ui/.../SecureWhile.kt`; `HomeScreen.kt:229` (доставка), `BodyDiaryScreen.kt:412` (черновик), `SoulScreen.kt:70`; вживую: `screencap` Soul-экрана вернул 0 байт при открытом экране (uiautomator его видит) |
| Трипвайр LocalMindEngine для дневника | `app/src/test/kotlin/app/anima/NetworkIsolationTest.kt:122-135` (`body diary retelling speaks only to the local mind`) |
| mind_failure_http de-Gemma'd | `feature/settings/src/main/res/values*/strings.xml:113` — 6 локалей, упоминаний Gemma нет |
| versionCode 6 / versionName 0.6.0 | `app/build.gradle.kts:35-36` |
| README «13 tests» NetworkIsolationTest | `grep -c @Test` = 13 |
| Бюджет разрешений | смёрженный RELEASE-манифест: ровно 8 uses-permission + `app.anima.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` — точно документированный набор ADR-004; телеметрийные компоненты DataTransport отсутствуют |

## FABRICATED

Не найдено. Первый прогон, в котором каждое заявление CHANGELOG
подтвердилось кодом. (Гипотеза аудита не подтвердилась — но см. DEGRADED.)

## DEGRADED

1. **Устаревшие «~530 МБ» в копии приложения — 18 строк, 3 ключа × 6
   локалей.** `home_banner_asleep`, `mind_metered_hint`,
   `mind_download_button` (feature/home и feature/settings, values*).
   ADR-017/018 сменили дефолт реестра на Qwen2.5-1.5B int4 (~1.0–1.1 GB);
   стор-листинг v5/v6 уже исправлен на «~1 GB», а приложение всё ещё
   обещает 530 МБ на хоуме и в экране загрузки. Подтверждено вживую на
   эмуляторе (карточка «Bring a mind» и кнопка Download). → Фаза 3.
2. **CHANGELOG v0.6 «Roborazzi … (GMD)» в CI-заявлении честен, но локально
   голдены живут в 46 файлах, а не «44»** — расхождение только с устным
   числом из постановки задач прошлых прогонов, в репо числа нет; строго
   говоря, не дефект репозитория. Зафиксировано для точности.

## EYES-ONLY-FINDINGS (класс «дефект иконки» — видно только глазами)

1. **Подписи концептов в онбординг-пикере схлопнуты до ~2dp и не видны.**
   `core/creature/ConceptGallery.kt:44,58,78` — фиксированный
   `cellHeight = 148.dp` при `aspectRatio(1.25f)` картинки: на устройстве
   шириной 411dp содержимому ячейки (163dp ширины) картинка забирает
   ~130dp из 132dp, подписи остаётся ~2dp (uiautomator: bounds высотой
   4px, [206,747][372,751]). В Settings те же подписи видны (46px) —
   другая геометрия экрана. Скриншоты: scratchpad `eyes-sheet-2/3`.
   → Фаза 3 (визуальный дефект).
2. **Иконка лончера — дефект v0.5 закрыт**: настоящий орб в app drawer,
   hotseat, сплеш. (`e01-launcher-home.png`, `e02-app-drawer.png`).
3. **Пустая панель существа в adaptive-голденах хоума — задокументированное
   намерение**, не дефект: `HomeAdaptiveGoldenTest.kt:29-31` (frame-loop
   CreatureSurface несовместим с Roborazzi; см. также audit-v04 gotcha).
4. **Обрезанный 4-й чип «25 мин» в rest-голденах RU/JA — by design**:
   `RestScreen.kt:313-317` — горизонтальный скролл вместо переноса
   (находка pseudolocale-прогона v0.5, комментарий в коде).
5. **`completed-light.png` (320×470) обрезает кнопку «Thanks» по нижней
   кромке** — артефакт фиксированного окна захвата компонентного голдена:
   в device-size голденах той же поверхности (`completed-ru.png`,
   `completed-ja.png`, 1078×2399) кнопка видна целиком. Не дефект кода.
6. Онбординг-контракт («памятка честности») присутствует на экране имени:
   «It lives on this phone… Settings → Why no internet» — соответствует
   инварианту «факты только через подтверждение».
7. Trust-экран («Why no internet»), Mind-экран, Settings, Diary, Rest
   пройдены вживую — рендер чистый, контрасты читаемы, обрезок нет.

## Полный локальный контур (Фаза 0.2)

- `detekt ktlintCheck :app:assembleDebug testDebugUnitTest
  verifyRoborazziDebug :app:lintVitalRelease :app:bundleRelease` —
  BUILD SUCCESSFUL (exit 0).
- Форс-перегон `testDebugUnitTest verifyRoborazziDebug --rerun-tasks` —
  BUILD SUCCESSFUL: **92 юнит-теста, 0 падений** (сумма по XML
  test-results), все 46 голденов verify-зелёные.
- GMD-сьют `atd34DebugAndroidTest`: **ЖЁЛТЫЙ, диагностирован.**
  Заход 1 упал до эмулятора (AVD на переполненном C:, нужно 7.2 GB);
  заход 2 (AVD на D:): 5 сьютов исполнено, 4 зелёных
  (EncryptedUpgradeDeviceTest, MindBakeoffHarness, WidgetSnapshot ×2 —
  остальные UP-TO-DATE от зелёного прогона v0.6), 1 падение —
  `DayInLifeTest`: timeout 60s «node 'onboarding.hatch' exists»; ретрай
  по CI-политике — то же падение. Диагностика этой сессии: тот же тест
  на connected google_apis-эмуляторе падает ТОЛЬКО на первом прогоне
  после холодного бута и дважды зелёный на прогретом (скриншоты
  mon-серии: hatch → Home → чат E2E). Вывод: wall-clock safety-net 60s
  не переживает холодный старт эмулятора с D:-диска (v0.6 гонял AVD с
  C:-SSD); дефект тест-инфры, НЕ регрессия приложения. Фикс (подъём
  safety-net) — Фаза 3.1, после него GMD перегнан заново (статус — в
  финальном отчёте прогона).

## Security-проход (Фаза 0.4)

- Логирование секретов: `grep Log.[diewv](|println(` по всем `src/main`
  **пусто**; для cloud-mind это дополнительно закон (`NetworkIsolationTest`
  `cloud-mind never logs`).
- Ключ BYOK: `CloudKeyVault.kt` — `noBackupFilesDir` + AtomicFile + GCM;
  зануление входного массива в `finally`; тест `soul export never touches
  the cloud key` держит границу экспорта.
- Бэкапы: `app/src/main/res/xml/data_extraction_rules.xml` — полное
  исключение всех доменов из cloud-backup и device-transfer.
- FLAG_SECURE: покрытие Soul/капсулы подтверждено кодом и вживую (см. выше).
- Смёрженный release-манифест: разрешения — ровно документированный набор;
  компоненты — только ожидаемые (Glance/WorkManager/Play Core asset
  packs/ML Kit/своих три сервиса); `JobInfoSchedulerService` и
  `AlarmManagerSchedulerBroadcastReceiver` отсутствуют.

## Очередь дефектов в Фазу 3 (по серьёзности)

1. (визуальное) Подписи концептов в онбординге невидимы — ConceptGallery.
2. (доки/копия) 18 строк «~530 МБ» — привести к реальности реестра
   (данные из `MindModelRegistry`, а не хардкод в строках).
