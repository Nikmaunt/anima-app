# Глоссарий локализации (v0.5, Фаза 2)

Правила: строки существа — продуктовый копирайтинг, не подстрочник; голос —
тёплый, простой, первое лицо, без канцелярита. JA — мягкий разговорный стиль
(плейн-формы, хирагана там, где кандзи холодит); RU/PL — «ты»; DE — «du»;
ES — «tú». Названия языков в UI — самоназвания (MindLanguage.selfName).
Никакого машинного тона: короче — лучше. Плюрали — только через plurals/ICU.

| EN | RU | PL | DE | ES | JA |
|---|---|---|---|---|---|
| creature | существо | stworzenie | Wesen | criatura | いきもの |
| soul | душа | dusza | Seele | alma | たましい |
| soul vault | хранилище души | skarbiec duszy | Seelen-Tresor | cofre del alma | たましいの金庫 |
| mind | разум | umysł | Geist | mente | あたま |
| the mind sleeps | разум спит | umysł śpi | der Geist schläft | la mente duerme | あたまはおやすみ中 |
| body | тело | ciało | Körper | cuerpo | 体 |
| burrow (storage) | норка | norka | Bau | madriguera | 巣 |
| hearing (network) | слух | słuch | Gehör | oído | 耳 |
| energy (battery) | энергия | energia | Energie | energía | エネルギー |
| eating (charging) | ест / кормится | je | isst gerade | comiendo | 食事中 |
| warmth (thermal) | тепло | ciepło | Wärme | calor | ぬくもり |
| hatch / hatching | вылупление | wyklucie | Schlüpfen | eclosión | たんじょう |
| rest / rest session | отдых | odpoczynek | Ruhepause | descanso | ひとやすみ |
| rest together | отдохнуть вместе | odpocznijmy razem | zusammen ausruhen | descansar juntos | いっしょにひとやすみ |
| diary | дневник | dziennik | Tagebuch | diario | 日記 |
| our story | наша история | nasza historia | unsere Geschichte | nuestra historia | ふたりの物語 |
| fact (soul fact) | факт | fakt | Erinnerung (Fakt) | dato | おぼえたこと |
| remember this | запомнить это | zapamiętaj to | merk dir das | recuerda esto | おぼえておいて |
| wardrobe | гардероб | garderoba | Garderobe | vestuario | きせかえ |
| milestone | веха | kamień milowy | Meilenstein | hito | 節目 |
| widget | виджет | widżet | Widget | widget | ウィジェット |
| wallpaper | обои | tapeta | Hintergrund | fondo | 壁紙 |
| mind file / model | файл разума / модель | plik umysłu / model | Geist-Datei / Modell | archivo de mente / modelo | あたまのファイル / モデル |
| cloud mind | облачный разум | umysł w chmurze | Cloud-Geist | mente en la nube | クラウドのあたま |
| check key | проверить ключ | sprawdź klucz | Schlüssel prüfen | comprobar clave | キーを確認 |
| free tier | бесплатный тариф | darmowy plan | Gratis-Kontingent | plan gratuito | 無料枠 |
| the creature stays | существо остаётся | stworzenie zostaje | das Wesen bleibt | la criatura se queda | いきものはここにいる |
| days together | дней вместе (plurals!) | dni razem (plurals!) | Tage zusammen | días juntos | いっしょの日々 |
| quiet hours | тихие часы | ciche godziny | Ruhezeiten | horas de silencio | しずかな時間 |
| honest / honestly | честно | szczerze | ehrlich | con honestidad | 正直に |

Технические указания агентам миграции:
1. Ресурсы модульные: `feature/<m>/src/main/res/values[-ru|-pl|-de|-es|-ja]/strings.xml`;
   имена `home_…`, `mind_…`, `rest_…` и т.д. (модульный префикс, snake_case).
2. Composable: `stringResource(...)`; счётные — `pluralStringResource` +
   `<plurals>` во ВСЕХ локалях (у RU/PL 4 формы: one/few/many/other!).
3. VM-строки: закрытые наборы → enum/sealed + маппинг на ресурсы в UI
   (образец: MindFailure); свободные тексты с параметрами → sealed c args.
   `@ApplicationContext getString` — только когда рефактор на enum ломает
   больше, чем даёт (пометить `// l10n: context-bound` для ревью).
4. Строки, попадающие в BASE (сообщения чата существа из скриптов,
   дневниковые записи) — локализуются В МОМЕНТ СОЗДАНИЯ (текущая локаль),
   исторические записи не перерисовываются задним числом.
5. Не переводить: имена собственные (Anima), «wi-fi», технические ID моделей,
   URL, префиксы ключей.
6. Даты/числа — `java.text`/ICU через локаль по умолчанию, никаких ручных
   форматов.
