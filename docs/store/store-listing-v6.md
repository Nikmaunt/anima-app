# Store listing v6 — delta over v5 (2026-07-19)

v4 остаётся базой текстов, v5 — блоком «говорит на твоём языке». Этот
файл: сверка перед сабмитом в closed testing + новые вставки v0.6.

## 1. Сверка утверждений против кода v0.6 (этот прогон)

| Утверждение | Код v0.6 | Вердикт |
|---|---|---|
| "no network access in the app itself" | NetworkIsolationTest v6, 13 тестов, INTERNET по-прежнему только у двух модулей | ✅ |
| "speaks your language out of the box" (v5) | ⚠️ ПАК ПУСТ. **ОБНОВЛЕНО v0.8 (ADR-021):** причина сменилась — не «артефакт не произведён» (исход 3), а «дефолтный рантайм его не читает» (исход 2). **Гейт «прогнать convert.sh» ОТМЕНЁН**: конверсия этого не лечит. Остаётся единственный рабочий выход — смягчить формулировку до "speaks your language once the mind model is downloaded". Ждать наполненного пака НЕ надо | ⚠️ ГЕЙТ САБМИТА (условие изменено) |
| "optional AI model download (~1 GB)" | int4 ≈1.0–1.1 GB (оценка research-v6 §A.4) | ✅ цифра v5 верна |
| «письма в будущее», «прощание на ночь» | v0.5 код жив, E2E v2 их проходит | ✅ |
| FLAG_SECURE на самом сокровенном | теперь ВКЛЮЧАЯ капсулы (SecureWhile) | ✅ усилилось |

## 2. Новые вставки full description (после блока NEW IN THIS SEASON v5)

**EN**
LITTLE THINGS THIS SEASON
Free up some storage and your creature will stretch happily — the burrow
got roomier. Mute your phone and it whispers. Long-press any of its
replies to report one that felt wrong — it disappears, and only a local
note remains. And there is finally a proper app icon, with a themed
monochrome variant.

**RU**
МЕЛОЧИ ЭТОГО СЕЗОНА
Освободи место на телефоне — существо радостно потянется: норка стала
просторнее. Выключи звук — оно перейдёт на шёпот. Долгое нажатие на его
реплику — пожаловаться на неудачный ответ: реплика исчезнет, останется
только локальная пометка. И наконец-то настоящая иконка приложения, с
монохромным themed-вариантом.

**PL**
DROBIAZGI TEGO SEZONU
Zwolnij trochę miejsca, a stworzenie z radością się rozciągnie — norka
zrobiła się przestronniejsza. Wycisz telefon — będzie szeptać. Przytrzymaj
jego odpowiedź, by zgłosić nieudaną — zniknie, zostanie tylko lokalna
notatka. I nareszcie prawdziwa ikona aplikacji, z monochromatycznym
wariantem themed.

**DE**
KLEINIGKEITEN DIESER SAISON
Räum etwas Speicher frei, und dein Wesen streckt sich glücklich — der Bau
ist geräumiger geworden. Stell das Telefon stumm, und es flüstert. Halte
eine seiner Antworten gedrückt, um eine misslungene zu melden — sie
verschwindet, nur eine lokale Notiz bleibt. Und endlich ein richtiges
App-Icon, mit monochromer Themed-Variante.

**ES**
PEQUEÑOS DETALLES DE ESTA TEMPORADA
Libera algo de espacio y tu criatura se estirará feliz: la madriguera está
más amplia. Silencia el teléfono y susurrará. Mantén pulsada una de sus
respuestas para denunciar una que no estuvo bien: desaparece y solo queda
una nota local. Y por fin un icono de verdad, con variante monocroma.

**JA**
今シーズンの小さなこと
ストレージを空けると、いきものがうれしそうにのびをします——すみかが
ひろくなったから。マナーモードにすると、ささやき声に。へんじを長押し
すると、よくなかったへんじを報告できます——消えて、ローカルなメモだけが
残ります。そして、ついにちゃんとしたアプリアイコン（モノクロのテーマ
対応つき）。

## 3. Скриншоты и ассеты

- Сценарий v0.4 остаётся; снимать по closed-testing-plan §5 (риг, 6 локалей).
- Иконка 512 px: экспорт из адаптивной иконки (фон NightDeep→NightSurface,
  орб по центру); feature graphic 1024×500 — сцена «существо в ночи».
- Двухпанельный скриншот с фолда — опционально в набор планшетных.
