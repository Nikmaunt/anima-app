# Задача 7 — независимое дизайн-ревью, дословно

Прогон v1.1c, 2026-08-01.

## Как это было устроено

Свежий read-only субагент, не участвовавший в работе. Ему дали **только**
скриншоты и явный запрет читать документацию, исходники и историю git. Он не
знал, кто это делал, какие были ограничения и что именно этот прогон менял.
Задача — оценить как продуктовый дизайнер, знающий современные Android-приложения:
что выглядит устаревшим, где нарушена иерархия, где композиция случайна, что бы
он изменил первыми тремя.

Файлы, которые он видел: `after-v11c/01-home`, `03-passport`, `05-shades`,
`06-rest`, `07-diary`, `08-soul`, плюс `device/02-home-wallpaper-day` и
`device/04-widget-frames-8x3`.

Ниже — его ответ **дословно и целиком**, включая то, что мне не понравилось.

---

**1. DATED**

The bottom bar of `01-home.png` is the loudest offender: four bare text labels ("Отдых / Душа / Дневник / Настройки") floating on the background with no icons, no container, no indicator, no divider. That is a 2013 ActionBar tab strip, not a 2026 nav bar, and the odd-one-out blue tint on "Настройки" makes it look like a broken web link row. It also violates touch-target convention — the labels are ~40px tall in a 200px dead zone.

The card language across `03-passport.png`, `07-diary.png` and `08-soul.png` is CardView-era: uniform rounded rectangles, one surface tone barely above the background, one flat elevation, ALL-CAPS letterspaced section labels ("ТЕЛО", "ЭНЕРГИЯ", "НЕДЕЛЯ В ФАКТАХ"). That caps-label-plus-list pattern reads as a Holo/early-Material settings screen dressed in dark mode.

The chart in `07-diary.png` is worse than dated, it's unfinished: three hairline rules, one lone dot, one orange tick at the right edge, no axis, no labels. And "Показать 7 дней" is styled as bare pink text with no button affordance — a 2011 hyperlink.

`05-shades.png` locked rows use flat grey desaturation on the label text plus a *full-colour* creature. Locked and unlocked read identically at the thumbnail; the greyed text is the only signal, which is the old "disabled = 50% alpha" trick.

**2. HIERARCHY**

`01-home.png`: the creature is the point of the app, and it sits dead centre with 700px of empty above it, so the eye lands first on "Nika" in the top-left corner — a label — and has to travel down. The composition gives the hero the weakest position on the screen.

`03-passport.png`: "Сохранить" is set in the same pink as "Назад" at similar weight, so the destructive-adjacent back affordance and the commit action compete as peers. Worse, the giant creature portrait eats the top 40% above the fold, pushing the actual content — the name field — to the middle and the ТЕЛО card mostly off-screen.

`08-soul.png` is the clearest failure. Three actions sit in one row at three different visual weights: a filled pink pill ("Экспорт души"), plain pink text ("Наша история…"), and a third that has collapsed into a vertical one-letter-per-line column ("Открытка"). The filled pill wins the eye, but a Markdown export is almost certainly not the primary action on a screen about memory — the search field and chips below are, and they're the quietest thing there.

`06-rest.png`: "Назад" is bold pink at the top of an otherwise empty 900px column, so it is the loudest element on a screen whose actual job is the duration chips at the bottom.

**3. ACCIDENTAL**

`08-soul.png`, the "Открытка" text broken to one character per line, is unambiguously a layout falling over, not a decision. Same screen: "кормёжек" wraps to "кормёж / ек" in the stat row, so the fourth stat's baseline sits lower than the other three and the row loses its grid.

`06-rest.png`: the "25 мин" chip is clipped by the right screen edge with no peek, no fade, no scroll cue. A four-item row that doesn't fit is a decision that was never made.

`03-passport.png`: inside the ТЕЛО card, "Вылупилось / 28 июля 2026 г." is a clean label-value pair, but the next row puts the two-line grey caption "число этого телефона..." *between* "Геном" and its value "V6F5-55VE", so three text styles interleave and the value column stops aligning. "Телефон / Google sdk_gphone64_x86_64" then aligns to nothing.

`01-home.png`: "вместе 5 дней" sits about 8px under a 40sp "Nika" with no breathing room, so they fuse into one blob, and both are jammed into the status bar's shadow.

`07-diary.png`: "Показать 7 дней" is indented ~30px further right than "ЭНЕРГИЯ" above it and the caption below it. Nothing on that card shares a left edge.

**4. FIRST THREE FIXES**

First, fix `08-soul.png`'s action row and the "кормёжек" wrap. This is above everything else because it isn't a taste question — it is visible breakage, and a user who sees a word running vertically down the screen stops trusting the app immediately.

Second, rebuild the `01-home.png` nav. It's on every screen's return path, it's the single most dated element in the set, and it currently fails both the era test and the touch-target test. Icons plus label, a selected state, and a real surface.

Third, `07-diary.png`'s energy chart. It is the only data visualisation in the app and it currently communicates nothing — either give it axes, a baseline and labelled days, or replace it with an honest empty state until there's data. Below the nav because it's one screen, not all of them.

**GOOD**

The creature art is genuinely strong and it is the app's whole identity. The fox in `01-home.png` — flat gradient masses, no outline, the offset cream tail-tip echoing the muzzle — is confident, and it holds up at every size from full-screen to the 200px widget cells.

`04-widget-frames-8x3.png` is the best-designed asset here. Eight creatures, three states, one drawing grammar: the same closed-eye crescent means "asleep" on the orb, the fox, the jelly and the robot. The state read is instant without a single word. The `pixel_pet` deliberately breaking the shared vocabulary with a hard-pixel style is a good call, not an inconsistency.

The wallpaper in `02-home-wallpaper-day.png` is right: the creature sits above the icon grid, doesn't fight Gmail or Chrome for colour, and the warm bone background makes the Google icon row look intentional rather than dumped on top.

The `05-shades.png` intro paragraph does its explanatory job in three lines at a comfortable measure, and the palette discipline — one warm red-brown family everywhere, no stray accent — is real and rare.

---

## Что я с этим сделал

| Находка | Действие |
|---|---|
| `08-soul` — «Открытка» столбиком по букве, «кормёжек» переносится | **Исправлено** этим прогоном: `ActionRow` и `StatRow` вместо голых `Row`. Это дефект D1 — тот самый класс, ради которого в v1.1 построили примитивы раскладки, — живой на экране, который никто не переснимал |
| `06-rest` — «Назад» громче содержимого, пустая нижняя половина | **Исправлено**: содержимое село на оптическую середину, проверено на устройстве |
| `01-home` — ряд навигации из четырёх голых подписей; «Настройки» другого оттенка | **Не сделано.** Разный оттенок — это `GhostButton(quiet = true)` → `textDim`, который остался нейтральным, когда акцент стал производным от существа. Перекрашивать текстовые токены — значит ставить под сомнение доказательство контраста `SignatureHueContrastTest` на 360 оттенках, и делать это в конце прогона нельзя |
| `07-diary` — график энергии ничего не сообщает | **Не сделано.** Пустое состояние у него теперь есть (D9), но с данными он по-прежнему без осей и подписей |
| `03-passport` — колонка значений перестаёт выравниваться на строке «Геном» | **Не сделано** |
| Карточки читаются как CardView-эпоха | **Не сделано.** Это переработка дизайн-системы, а не дефект |
| `05-shades` — закрытый и открытый оттенок различаются только серостью подписи | **Не сделано** |

## Одно несогласие, которое я записываю, а не прячу

Ревьюер пишет про `06-rest`: «the "25 мин" chip is clipped by the right screen
edge **with no peek, no fade, no scroll cue**». Затухание там **есть** —
`ScrollableChipRow`, ровно этот дефект D2 и правился, и на
`after-v11c/06-rest.png` я его вижу своими глазами.

Два объяснения, и я не знаю, какое верное: он прочитал файл до того, как я его
перезаписал (я менял композицию Отдыха в том же промежутке), либо затухание
слишком слабое, чтобы читаться как приглашение листать. Второе — это тоже
находка, просто другая. Проверять на устройстве.

## Моя оценка самого ревью

Полезно, и полезнее всего там, где я смотрел и не увидел. «Открытка» столбиком
была у меня на экране, я снимал этот скриншот и не заметил — потому что искал
то, что менял. Ровно для этого задача 7 и существует.

Две оценки считаю спорными:

- **«Существо в центре — самая слабая позиция, глаз идёт сначала на "Nika"».**
  Прошлый прогон измерял это по пикселям и сознательно посадил тело **ниже**
  геометрического центра, собрав воздух в одну зону сверху. Ревьюер видит один
  кадр без реплик; в кадре с содержимым текст идёт сразу под телом.
- **«Портрет в паспорте съедает верхние 40 %».** Это read-only паспорт, его
  предмет — существо. Живой портрет и есть содержание; поле имени под ним —
  единственный элемент управления, и он именно там, где заканчивается портрет.
