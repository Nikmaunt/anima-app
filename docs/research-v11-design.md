# Research v1.1 — визуальный язык, движение, типографика

Фаза 1 прогона v1.1, 2026-07-28. Read-only.
Пометки: **[дока]** — первоисточник; **[артефакт]** — проверено
инструментом на этой машине; **[гипотеза]** — моё суждение.

---

## 1.1 Референсы: честная граница того, что я смог проверить

Задание просило 5–8 конкретных приложений 2025–2026 с разбираемыми
приёмами. **Я не могу выполнить это добросовестно и говорю об этом
прямо.** В этой среде нет способа открыть чужое Android-приложение и
посмотреть на его экраны; поиск отдаёт подборки-листиклы
(«DesignRush: Best App Designs With Dark UI of 2026», Muzli, appschopper),
которые перечисляют названия и хвалят их общими словами
(«clean interface», «abundant white space»), но не дают ни кегля, ни
отступа, ни того, что именно сделано. Пересказать их как свой разбор
значило бы выдать чужую рекламную аннотацию за наблюдение.

Поэтому вместо выдуманного разбора — то, что **можно** проверить и что
реально влияет на решения v1.1: опубликованное исследование самой
Google, которая три года изучала ровно этот вопрос («как заставить глаз
найти главное»).

**[дока]** Google Design, «Expressive Material Design: Google research»:
три года, **46 отдельных исследований**, сотни макетов, более
**18 000 участников**; методы — айтрекинг, опросы, фокус-группы.
Ключевой результат для нас: в айтрекинге участники находили ключевой
элемент интерфейса **до четырёх раз быстрее** в expressive-варианте,
чем в обычном M3, и разрыв «пожилые находят медленнее» **исчезал**.
Предпочтение expressive — до 87 % в группе 18–24.
([design.google](https://design.google/library/expressive-material-design-google-research),
[dezeen](https://www.dezeen.com/2025/05/28/google-ushers-in-age-of-expressive-interfaces-with-material-design-update/))

**Вывод для нас [гипотеза, но опирается на дока-факт].** Проблема
нашего Home — не «недостаточно красиво», а «глазу не за что зацепиться»
(это ровно D7 и D10 владельца). Айтрекинговый результат Google говорит,
что лечится это не украшением, а **резким различием в весе между
главным и второстепенным**. У нас сейчас существо и текстовые плашки
имеют сопоставимый визуальный вес — значит герой не назначен.

**Что из общих подборок можно взять как ориентир, помечая как чужое
мнение [дока о том, что так пишут; не проверено мной]:** тёмная тема
2026 у авторов подборок связывается с «vibrant gradients and glowing
accent colors» и с трактовкой пустоты как активного элемента
(«Active Breathing Room»), а не остатка.
([appschopper](https://www.appschopper.com/blog/top-trending-mobile-app-design-practices-year/),
[uidesignz](https://uidesignz.com/blogs/mobile-ui-design-best-practices))
Это совпадает с нашим направлением (свечение от генома + воздух вместо
карточек), но доказательством не является.

---

## 1.2 Compose сегодня: что доступно БЕЗ подъёма версий

Наш пин: `composeBom = "2025.06.01"`, `androidxNavigation = "2.9.8"`,
`minSdk = 31`, `compileSdk = 36` (`gradle/libs.versions.toml:19-20`,
`build-logic/src/main/kotlin/KotlinAndroid.kt:23`).

**[артефакт]** BOM 2025.06.01 разрешается в `compose.animation 1.8.3`
и `compose.foundation 1.8.3` — обе версии лежат в кэше
`D:\gradle\caches\modules-2\files-2.1\`.

### SharedTransitionLayout — ДОСТУПЕН, апгрейд не нужен

**[артефакт]** Распакован
`androidx.compose.animation/animation-android/1.8.3/.../animation-release.aar`
→ `classes.jar`. В пакете `androidx/compose/animation/` присутствуют:

- `SharedTransitionScopeKt$SharedTransitionLayout$1.class` и `$2.class`
  — то есть composable `SharedTransitionLayout` скомпилирован в этой версии;
- `SharedTransitionScope.class`, `SharedTransitionScopeImpl.class`
  с методами `sharedElement`, `sharedBounds`,
  `sharedElementWithCallerManagedVisibility`,
  `renderInSharedTransitionScopeOverlay`;
- `SharedBoundsNode.class`, `SkipToLookaheadNodeKt.class`;
- `ExperimentalSharedTransitionApi.class` — API помечен opt-in, но
  присутствует.

**Вывод:** переход существа между экранами как shared element
реализуем на закреплённом BOM. Цена — аннотация
`@OptIn(ExperimentalSharedTransitionApi::class)`, не апгрейд.

**[дока]** Для связки с навигацией нужен `navigation-compose ≥ 2.8.0`;
у нас **2.9.8** — с запасом.
([Navigation with shared elements](https://developer.android.com/develop/ui/compose/animation/shared-elements/navigation))

### Предиктивный back — уже включён, анимаций нет

**[дока]** Предиктивные анимации включены по умолчанию на Android 15+
(API 35); при `targetSdk ≤ 34` нужен
`android:enableOnBackInvokedCallback="true"`. Navigation Compose с 2.8.0
делает кросс-фейд автоматически; собственная анимация задаётся через
`popEnterTransition` / `popExitTransition`.
([Set up Predictive back](https://developer.android.com/develop/ui/compose/system/predictive-back-setup),
[Add support for the predictive back gesture](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture))

**[артефакт]** У нас флаг уже стоит:
`app/src/main/AndroidManifest.xml:29` —
`android:enableOnBackInvokedCallback="true"`. А вот переходов нет:
в `app/.../AnimaRoot.kt:92-160` ни у `NavHost`, ни у одного `composable`
не задано `enterTransition`/`exitTransition`/`popEnterTransition`.
То есть инфраструктура для D11 готова, не хватает ровно указания
переходов.

### AGSL RuntimeShader — доступен, но с чётким полом

**[дока]** AGSL появился в Android 13, доступен через `RuntimeShader`,
то есть **API 33+**.
([AGSL: Made in the Shade(r), Chet Haase](https://medium.com/androiddevelopers/agsl-made-in-the-shade-r-7d06d14fe02a))

**[артефакт]** Наш `minSdk = 31`. Значит на API 31–32 шейдера нет и
нужна запасная ветка — она уже есть в `GlowSkin.kt:20` (`SDK_INT >= 33`)
и в `GlowSkin.kt:45` (проверка `isHardwareAccelerated`). Именно
несогласованность этих двух веток и даёт D5 (см. Фазу 0).
`Modifier.blur` доступен с API 31, то есть ровно с нашего пола —
размытие для глубины можно использовать без гейта.

### Haptics

**[артефакт]** Уже используется: `HapticFeedbackType.Confirm`,
`.LongPress`, `.Reject` в `HomeScreen.kt:328,338,344`. Комментарий
`KotlinAndroid.kt:11` прямо называет «modern haptics» одной из причин
`minSdk 31`. Новых зависимостей не требуется.

### Что БЕЗ апгрейда недоступно

**[артефакт]** `androidx.compose.material3:material3` в кэше — только
**1.3.2** (`D:\gradle\caches\modules-2\files-2.1\androidx.compose.material3\material3\`),
это версия из нашего BOM. `MotionScheme`, expressive-компоненты, 35
новых shape'ов и morph — появились позже. Их взять нельзя без подъёма
BOM. Числа из 1.3 можно перенести руками (см. ниже) — это не апгрейд.

---

## 1.3 Material 3 Expressive: числа, которые прошлая сессия не достала

Прошлый прогон записал: «точные значения токенов получить не удалось,
числа сознательно не приводятся». **В этот раз достал — из
первоисточника, из исходников androidx, а не из пересказов.**

**[дока]** `androidx/compose/material3/tokens/ExpressiveMotionTokens.kt`
(ветка `androidx-main`):

| Токен | Damping | Stiffness |
|---|---|---|
| `SpringDefaultSpatial` | **0.8** | **380.0** |
| `SpringFastSpatial` | **0.6** | **800.0** |
| `SpringSlowSpatial` | **0.8** | **200.0** |
| `SpringDefaultEffects` | **1.0** | **1600.0** |
| `SpringFastEffects` | **1.0** | **3800.0** |
| `SpringSlowEffects` | **1.0** | **800.0** |

**[дока]** `.../tokens/StandardMotionTokens.kt`:

| Токен | Damping | Stiffness |
|---|---|---|
| `SpringDefaultSpatial` | **0.9** | **700.0** |
| `SpringFastSpatial` | **0.9** | **1400.0** |
| `SpringSlowSpatial` | **0.9** | **300.0** |
| `SpringDefaultEffects` | **1.0** | **1600.0** |
| `SpringFastEffects` | **1.0** | **3800.0** |
| `SpringSlowEffects` | **1.0** | **800.0** |

Источник: `github.com/androidx/androidx`, ветка `androidx-main`,
`compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/`
— файлы `ExpressiveMotionTokens.kt` и `StandardMotionTokens.kt`.
Промежуточный файл `MotionScheme.kt` только ссылается на эти константы
по именам; численные значения лежат именно в token-файлах, поэтому
прошлая сессия их и не нашла.

Что эти числа подтверждают качественно **[дока]**
([M3 motion](https://m3.material.io/styles/motion/),
[M3 Expressive motion](https://m3.material.io/blog/m3-expressive-motion-theming)):

- **Все** effects-токены имеют damping ровно **1.0** в обеих схемах —
  то есть цвет и прозрачность **никогда** не перелетают. Это не совет,
  это жёсткое правило системы.
- Различие схем целиком в spatial-затухании: 0.9 (standard) против
  0.6–0.8 (expressive). Никакого «expressive = быстрее» нет —
  `SpringDefaultSpatialStiffness` у expressive **ниже** (380 против 700),
  то есть expressive-движение **медленнее и мягче**, а «выразительность»
  берётся из перелёта, а не из скорости.
- Три скорости различаются жёсткостью примерно вдвое-вчетверо
  (200 / 380 / 800 у expressive), затухание при этом почти не меняется.

**[артефакт]** Как это ложится на нас: наш собственный движок
(`core/creature/.../engine/Springs.kt`) уже пружинный и уже разделяет
каналы. Наши текущие числа — `SpringChain(stiffness = 120, damping = 10)`,
`DART_STIFFNESS = 420` — заданы в другой параметризации (damping как
коэффициент, а не как безразмерное отношение), поэтому **прямо
подставлять токены M3 в движок нельзя**. Токены применимы к хрому:
появление экранов, отклик кнопок, перемещение существа между экранами.

---

## 1.4 Типографические шкалы

**[дока]** Базовая шкала M3 (`androidx/compose/material3/tokens/TypeScaleTokens.kt`,
`androidx-main`), кегль / межстрочный, sp:

| Роль | Large | Medium | Small |
|---|---|---|---|
| Display | 57 / 64 | 45 / 52 | 36 / 44 |
| Headline | 32 / 40 | 28 / 36 | 24 / 32 |
| Title | 22 / 28 | 16 / 24 | 14 / 20 |
| Body | 16 / 24 | 14 / 20 | 12 / 16 |
| Label | 14 / 20 | 12 / 16 | 11 / 16 |

Наблюдаемые отношения внутри шкалы: 57/45 ≈ 1.27, 45/36 = 1.25,
36/32 = 1.125, 32/28 ≈ 1.14, 28/24 ≈ 1.17. То есть **шаг между
соседними ступенями — примерно 1.12–1.27**, и он сознательно неровный:
крупные ступени разнесены сильнее, мелкие плотнее.

**[дока]** M3 Expressive добавил вторую параллельную шкалу из тех же 15
ролей — **emphasized**, отличающуюся более тяжёлым начертанием;
типографика при этом объявлена основным инструментом иерархии, а не
просто способом показать текст.
([M3 Typography](https://m3.material.io/styles/typography),
[Material 3 Expressive: New Components, Motion, Shapes](https://supercharge.design/blog/material-3-expressive))

**Прямой ответ на вопрос «какой контраст между цифрой-героем и подписью
считается хорошим» [гипотеза, выведенная из дока-чисел].** В самой
шкале M3 расстояние Display Large → Label Medium составляет
**57 / 12 ≈ 4.75×**, а Display Small → Label Small — **36 / 11 ≈ 3.3×**.
Наша текущая пара — `headlineMedium` (28) и `labelMedium` (12), то есть
**2.3×**, и это самый низкий контраст из всех, что шкала допускает для
«герой + подпись». Ровно это владелец описал как D8. Целиться надо в
**≥ 3×**, и делать это не только кеглем, но и весом и цветом:
одинаковый вес при разном кегле читается слабее, чем разный вес при том
же кегле.

---

## 1.5 Пустые состояния как жанр

**[дока]** NN/g, «Designing Empty States in Complex Applications»:
пустые состояния — возможность сообщить состояние системы, повысить
обучаемость и дать **прямой путь к ключевой задаче**, а не просто
сообщение об отсутствии данных.
([nngroup](https://www.nngroup.com/articles/empty-state-interface-design/))

**[дока]** Практические своды (Mobbin, Setproduct, Eleken) сходятся на
трёх вещах, и все три у нас нарушены:
1. Иллюстрация несёт эмоцию и метафору, а не украшает пустоту.
2. Текст объясняет контекст и даёт следующий шаг; «нет данных», «пусто»
   — ленивая заглушка.
3. Пустое состояние — часть пути, а не дыра в нём.
([Mobbin glossary](https://mobbin.com/glossary/empty-state),
[Setproduct](https://www.setproduct.com/blog/empty-state-ui-design),
[Eleken](https://www.eleken.co/blog-posts/empty-state-ux))

**[дока]** Отдельно, из уже собранного в прошлом прогоне и всё ещё
действующего: рубрика качества виджетов Google, `WT-1` — «Zero and empty
states are **intentional** and show the value» — Tier 2. То есть Google
считает неоформленное пустое состояние не мелочью, а понижением яруса
качества.
([Widget quality](https://developer.android.com/docs/quality-guidelines/widget-quality))

**[артефакт]** Наши три пустых состояния и что с ними не так:

| Экран | Сейчас | Нарушено |
|---|---|---|
| «Я и Anima» (`15-story.png`) | одна веха, три строки, 90 % чёрного | (1) нет образа, (3) выглядит как ошибка |
| График энергии (`16-diary.png`) | пустая сетка, одна точка у края | (1), (2) — подпись объясняет, но сетка кричит «сломано» |
| Память Души (`10-soul.png`) | существо в квадрате + цитата | ближе всех к норме; портит только квадрат из D5 |

Память Души — единственное место, где принцип уже применён правильно
(существо говорит о себе от первого лица). Это и есть образец, к
которому надо привести два других.

---

## Моя гипотеза, не проверено

1. Referens-разбор чужих приложений (1.1) в этой среде недостижим;
   решения v1.1 я строю на исследовании Google и на собственных
   скриншотах, а не на анализе конкурентов. Если владельцу нужен именно
   разбор конкретных приложений — это задача для человека с телефоном.
2. Целевой контраст «герой / подпись» ≥ 3× выведен мной из
   пропорций шкалы M3, а не взят из нормы. Нормы с числом я не нашёл.
3. Токены M3 Expressive получены из ветки `androidx-main`, то есть из
   разрабатываемой версии. На момент их стабилизации числа могут
   отличаться; для нас это не критично, потому что мы всё равно
   переносим их руками, а не берём библиотеку.
4. Утверждение «effects никогда не перелетают» — моя формулировка
   наблюдения, что все шесть effects-токенов имеют damping 1.0.
   Явного запрета в тексте M3 нет, есть слово «undesirable».
