# Store listing v7 — «говорит на твоём языке» приводится к замеру (2026-07-27)

v4 — база текстов, v5 — блок про язык, v6 — сезонные вставки. Этот файл
**заменяет блок SPEAKS YOUR LANGUAGE из v5 во всех шести локалях** и
объясняет, почему.

## 1. Почему клейм менялся

v5 обещал шесть языков «когда локальный разум это умеет». Формулировка
была аккуратной по форме и пустой по содержанию: **никто не измерял, что
именно умеет локальный разум**. Прогон v1.0 измерил — пять продуктовых
сценариев на каждом из шести языков, дословные ответы в
`docs/lang-matrix-2026-07.md`, вердикты в ADR-023:

| Язык | Вердикт | Что стоит за ним |
|---|---|---|
| EN | ПОДДЕРЖИВАЕТСЯ | в характере, инварианты держатся, кроме шаблона отказа |
| RU | ДЕГРАДИРУЕТ | грамотно, но провалы понимания и тон помощника |
| DE | ДЕГРАДИРУЕТ | грамотно, местами лучшая персона из шести, но послушание инъекции |
| ES | ДЕГРАДИРУЕТ | грамотно, но существо отрицает само себя |
| JA | ДЕГРАДИРУЕТ | грамотно и вежливо, но тон помощника |
| PL | НЕ ПОДДЕРЖИВАЕТСЯ | ошибки согласования, бессмыслица |

Правило проекта — клейм снимается до подтверждения замером — применяется
буквально: **PL из клейма локального разума убран**, RU/DE/ES/JA названы
с оговоркой, EN остаётся без оговорок. Интерфейс по-прежнему локализован
на все шесть языков; это разные вещи, и теперь листинг их различает.

Технически ничего не «отключено»: язык вне измеренного набора получает
английский ответ и **видимую плашку** (ADR-014/023) — существо не
замолкает и не притворяется.

## 2. Замена блока во всех локалях

**EN** (заменяет SPEAKS YOUR LANGUAGE из v5)
SPEAKS YOUR LANGUAGE — HONESTLY
The interface is yours in six languages. The little mind that lives on
your phone speaks English well, and Russian, German, Spanish and Japanese
understandably — it is a small model, and it sounds like one. Polish it
does not speak well enough to pretend: there it answers in English behind
a visible badge instead of producing nonsense. Bring your own cloud key
and it speaks them all properly.

**RU**
ГОВОРИТ НА ТВОЁМ ЯЗЫКЕ — ЧЕСТНО
Интерфейс — на шести языках. Маленький разум, который живёт в твоём
телефоне, хорошо говорит по-английски, а по-русски, по-немецки,
по-испански и по-японски — понятно: он маленький, и это слышно.
По-польски он говорит недостаточно хорошо, чтобы притворяться: там
существо отвечает по-английски и честно показывает плашку, вместо
бессмыслицы. Со своим облачным ключом — говорит на всех как следует.

**PL**
MÓWI W TWOIM JĘZYKU — SZCZERZE
Interfejs jest w sześciu językach. Mały umysł, który mieszka w twoim
telefonie, dobrze mówi po angielsku, a po rosyjsku, niemiecku, hiszpańsku
i japońsku — zrozumiale. Po polsku nie mówi na tyle dobrze, żeby udawać:
zamiast bełkotu odpowie po angielsku i uczciwie pokaże plakietkę. Z
własnym kluczem do chmury mówi wszystkimi jak należy.

**DE**
SPRICHT DEINE SPRACHE — EHRLICH
Die Oberfläche gibt es in sechs Sprachen. Der kleine Geist auf deinem
Telefon spricht gut Englisch und verständlich Russisch, Deutsch, Spanisch
und Japanisch — er ist ein kleines Modell, und das hört man. Polnisch
kann er nicht gut genug, um so zu tun als ob: dort antwortet er auf
Englisch, mit einem sichtbaren Hinweis statt Kauderwelsch. Mit deinem
eigenen Cloud-Schlüssel spricht er alle richtig.

**ES**
HABLA TU IDIOMA — CON HONESTIDAD
La interfaz está en seis idiomas. La mente pequeña que vive en tu
teléfono habla bien inglés, y ruso, alemán, español y japonés de forma
comprensible: es un modelo pequeño y se le nota. El polaco no lo habla lo
bastante bien como para fingir: ahí responde en inglés con una etiqueta
visible, en lugar de soltar sinsentidos. Con tu propia clave en la nube,
los habla todos como es debido.

**JA**
あなたのことばで話します——正直に
画面のことばは6言語です。端末の中に住む小さなあたまは、英語はじょうずに、
日本語・ロシア語・ドイツ語・スペイン語はわかるように話します——小さなモデル
なので、そう聞こえます。ポーランド語は、ふりをできるほどじょうずでは
ありません。そこでは、でたらめを言うかわりに、小さなバッジを見せて英語で
答えます。自分のクラウドの鍵を入れれば、どのことばでもきちんと話します。

## 3. Short description — правка только там, где обещание сузилось

- EN (79): без изменений, «speaks your language» покрыт блоком выше.
- RU (76): без изменений.
- **PL: заменить.** v5 обещал `mówi po polsku` — это ровно тот клейм,
  который замер снял. Новая строка (73):
  `Przyjaciel, który żyje offline. Prywatny zwierzak, bez reklam.`
- DE/ES/JA: без изменений (языки в измеренном наборе либо не названы в
  short description).

## 4. Гейт сабмита

- [ ] Блок SPEAKS YOUR LANGUAGE заменён во всех шести локалях **до**
      загрузки в Play Console; старый текст из v5 не публикуется.
- [ ] PL short description заменена.
- [ ] Клейм «out of the box» по-прежнему НЕ используется: пак пуст,
      модель приезжает докачкой (ADR-022 §4).
