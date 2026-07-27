# Дефект E2E-этажа: DayInLifeTest виснет на импорте души (2026-07-27)

Статус: **воспроизведён с трассой**, не починен. Это второе из двух
честных состояний, которые допускала постановка Фазы C; «почти чинится»
не заявляется.

## Что нового по сравнению с v0.7/v0.9

1. **Эмулятор ожил.** v0.9 объявила его расходником на один запуск: после
   жёсткого `Stop-Process` он «не грузился вообще». Оказалось — лечится
   уборкой: удалить `hardware-qemu.ini.lock`, `multiinstance.lock` и
   `snapshots` в `<avd>.avd/`, затем стартовать. `mind8g` поднялся с
   первой попытки, `adb devices` → `emulator-5554 device`,
   `sys.boot_completed` → `1`. Гвоздь среды v0.9 **снят**.
2. **Место зависания названо точнее.** v0.7 писал «хвост сценария
   (post-export)». Факт: тест не доходит до экспорта. Он встаёт на
   **импорте души**, сразу после `Espresso.closeSoftKeyboard()`
   (`DayInLifeTest.kt:258`).
3. **Страховочный таймаут не срабатывает, и понятно почему.**
   `TIMEOUT_MILLIS = 240_000` (`DayInLifeTest.kt:384`) сторожит только
   тело `waitForCondition`. Зависание — **вне** его: в вызове, который
   сам не возвращает управление. Тест простоял **13 минут** вместо того,
   чтобы упасть через 4.

## Трасса

Логкат целиком: `build/runlogs/phaseC-logcat.txt` (72 461 строка, 9.3 МБ,
писался в файл по правилу −1.4 handoff). Хвост активности приложения:

```
07-27 03:34:32.128 I/TestRunner( 5713): run started: 1 tests
07-27 03:34:32.132 I/TestRunner( 5713): started:
    day_in_life_onboarding_chat_rest_recreate_widget_export(app.anima.DayInLifeTest)
...
07-27 03:35:04.563 D/IdlingRegistry( 5713): Registering idling resources:
    [androidx.test.espresso.action.CloseKeyboardAction$CloseKeyboardIdlingResult…
07-27 03:35:04.567 W/CloseKeyboardAction( 5713): Attempting to close soft keyboard,
    while it is not shown.
07-27 03:35:04.819 I/ImeTracker( 5713): app.anima:4b483b0: onHidden
07-27 03:35:12.198 E/FrameTracker( 5713): force finish cuj, time out:
    J<IME_INSETS_ANIMATION::1@1@app.anima>
07-27 03:35:12.254 W/System  ( 5713): A resource failed to call release.
07-27 03:35:15.507 E/FrameTracker( 5713): force finish cuj, time out:
    J<IME_INSETS_ANIMATION::1@1@app.anima>
```

Дальше до самой остановки (13 минут) в логе только `EGL_emulation
app_time_stats` от процесса приложения: **кадры рисуются, прогресса
нет.** Ни одной строки `TestRunner: finished`. Процесс жив, экран
обновляется, тест не движется.

Два сигнала стоят рядом и, скорее всего, связаны:
`CloseKeyboardAction: Attempting to close soft keyboard, while it is not
shown` (Espresso закрывает клавиатуру, которой в его картине мира нет) и
дважды `force finish cuj, time out: IME_INSETS_ANIMATION` (система
доводит анимацию инсетов по таймауту). То есть IME-анимация и
приостановленные часы ComposeTestRule расходятся во мнении о том, что
происходит на экране, — ровно тот класс гонки, который v0.7 уже дважды
ловил в этом же месте (`performScrollTo` и swipe-петля).

## Воспроизведение

```powershell
$env:ANDROID_AVD_HOME="D:\Android\avd"
# уборка обязательна, если эмулятор до этого убивали жёстко:
Remove-Item "D:\Android\avd\mind8g.avd\*.lock" -Recurse -Force
Remove-Item "D:\Android\avd\mind8g.avd\snapshots" -Recurse -Force
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd mind8g `
    -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect
# в отдельном процессе: adb logcat -v time > build\runlogs\phaseC-logcat.txt
.\gradlew :app:connectedDebugAndroidTest `
    "-Pandroid.testInstrumentationRunnerArguments.class=app.anima.DayInLifeTest"
```

Гашение — **мягкое**: `adb emu kill`, ждать до 30 с, и только если жив —
`Stop-Process`. В этой сессии мягкого хватило, процессы ушли чисто.

## Что делать следующему прогону (не сделано здесь)

1. **Закрыть дыру в страховке первой.** Пока `performTextInput`,
   `performTouchInput` и `closeSoftKeyboard` не под дедлайном, любой
   зависший прогон стоит столько, сколько его терпят. Дешёвый вариант —
   обернуть шаг импорта в собственный wall-clock watchdog, который
   роняет тест с внятным сообщением.
2. **Проверить гипотезу IME × paused clock.** Убрать IME из уравнения:
   заполнять поле импорта через `performSemanticsAction(SetText)` вместо
   `performTextInput`, чтобы клавиатура не поднималась вообще.
3. **Прогнать на CI (Linux KVM).** Этот дефект ни разу не наблюдался вне
   этой машины, и три прогона подряд не проверяли этого.

Пока пункты 1–3 не сделаны, **E2E-этаж считается неработающим**, и
чеклист S24 §2 остаётся с пометкой «не проверять».
