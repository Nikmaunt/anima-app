# Store listing v5 — delta over v4 (2026-07-18)

v4 остаётся базой (тексты всех 6 локалей там). Этот файл: (1) сверка
v4-утверждений с фактическим кодом v0.5, (2) новый блок «говорит на твоём
языке» + новые фичи для вставки в full description, (3) правки short.

## 1. Сверка v4-утверждений против кода v0.5 (аудит сессии 2026-07-18)

| Утверждение листинга | Код v0.5 | Вердикт |
|---|---|---|
| "no network access in the app itself" | INTERNET только в model-delivery/cloud-mind; NetworkIsolationTest 12 тестов | ✅ держится |
| "cloud chat … off by default, clearly labeled" | CloudMindConfig.enabled=false деф.; лейбл CLOUD в шапке чата | ✅ |
| "never dies, never guilts" | инварианты v0.5 не тронуты; goodnight/тихие механики без стриков | ✅ усилилось |
| "Your words never leave the device" (on-device AI) | верно для локальных тиров; про облако сказано отдельно | ✅ |
| "Export or wipe it any time" | SoulScreen export (+capsules в payload v2), forget-flow | ✅ |
| "optional AI model download (~530 MB)" | дефолт пака теперь Qwen2.5-1.5B — int4 ~1.1 GB | ⚠️ ОБНОВИТЬ цифру в EN/DE/ES/RU/PL/JA полных текстах: "~1 GB" |
| подразумевался разговор на EN | v0.5: RU/PL/DE/ES/JA нативно при мультиязычной модели | ➕ добавить блок ниже |

## 2. Новые блоки для full description (вставлять после «TALKS TO YOU»)

**EN**
SPEAKS YOUR LANGUAGE
Your creature talks in your language — Russian, Polish, German, Spanish,
Japanese or English — right on the device, when the local mind supports it.
And it is honest: if the little model can only think in English, it says so
with a small badge instead of pretending.

NEW IN THIS SEASON
Letters to your future self: write one, and your creature holds it until
the day comes. A goodnight ritual — say goodnight, it curls up; skip it,
and nothing is lost. Weather in its bones: on phones with a barometer, it
feels the rain coming — completely offline.

**RU**
ГОВОРИТ НА ТВОЁМ ЯЗЫКЕ
Существо разговаривает по-русски (и ещё на пяти языках) прямо на
устройстве, когда локальный разум это умеет. И честно: если маленькая
модель думает только по-английски, оно скажет об этом плашкой, а не будет
притворяться.

НОВОЕ В ЭТОМ СЕЗОНЕ
Письма себе в будущее: напиши — существо будет хранить письмо до
назначенного дня. Вечернее прощание: пожелай спокойной ночи — оно
свернётся клубком; забудешь — ничего не потеряется. Погода в костях: на
телефонах с барометром существо чует приближение дождя — полностью офлайн.

**PL**
MÓWI W TWOIM JĘZYKU
Stworzenie rozmawia po polsku (i w pięciu innych językach) prosto na
urządzeniu, gdy lokalny umysł to potrafi. I jest szczere: jeśli mały model
myśli tylko po angielsku, powie o tym plakietką, zamiast udawać.

NOWOŚCI SEZONU
Listy do siebie z przyszłości: napisz, a stworzenie przechowa list do
wyznaczonego dnia. Wieczorne pożegnanie: powiedz dobranoc — zwinie się w
kłębek; pominiesz — nic nie przepada. Pogoda w kościach: na telefonach z
barometrem czuje nadchodzący deszcz — całkiem offline.

**DE**
SPRICHT DEINE SPRACHE
Dein Wesen spricht Deutsch (und fünf weitere Sprachen) direkt auf dem
Gerät, wenn der lokale Geist es kann. Und es ist ehrlich: Denkt das kleine
Modell nur auf Englisch, sagt es das mit einem kleinen Hinweis, statt zu
schauspielern.

NEU IN DIESER SAISON
Briefe an dein zukünftiges Ich: schreib einen — dein Wesen hütet ihn bis
zum Tag X. Ein Gute-Nacht-Ritual: sag gute Nacht, es rollt sich ein;
lässt du es aus, geht nichts verloren. Wetter in den Knochen: auf Handys
mit Barometer spürt es den Regen kommen — komplett offline.

**ES**
HABLA TU IDIOMA
Tu criatura habla español (y cinco idiomas más) directamente en el
dispositivo, cuando la mente local sabe hacerlo. Y es honesta: si el
modelito solo piensa en inglés, lo dice con una etiqueta en vez de fingir.

NOVEDADES DE TEMPORADA
Cartas a tu yo del futuro: escribe una y tu criatura la guardará hasta el
día señalado. Ritual de buenas noches: despídete y se acurruca; si lo
olvidas, no se pierde nada. El tiempo en los huesos: en móviles con
barómetro siente llegar la lluvia — completamente offline.

**JA**
あなたのことばで話します
ローカルのあたまが対応していれば、いきものは日本語（ほか5言語）で、端末の上だけで
おしゃべりします。そして正直です。小さなモデルが英語でしか考えられないときは、
ふりをせず、小さなバッジでそう伝えます。

今シーズンの新機能
未来の自分への手紙：書いておくと、いきものがその日まで大切に持っています。
おやすみの儀式：おやすみを言うと丸くなって眠ります。言い忘れても、何も失われません。
骨で感じる天気：気圧センサーのある端末では、雨の気配を感じます——完全オフラインで。

## 3. Short description v5 (по лимиту 80)

- EN (79): `A friend that lives offline — and speaks your language. Private pet, no ads.`
- RU (76): `Друг, который живёт офлайн — и говорит по-русски. Приватный питомец, без рекламы.` (81 — сократить при сабмите: убрать «Приватный»)
- PL (78): `Przyjaciel, który żyje offline — i mówi po polsku. Prywatny, bez reklam.`
- DE (75): `Ein Freund, der offline lebt — und Deutsch spricht. Privat, ohne Werbung.`
- ES (72): `Un amigo que vive sin internet — y habla español. Privado, sin anuncios.`
- JA: `オフラインで生きる友だち。日本語で話せて、広告なし。` (26 руническими — ок)

Все счётчики символов пересчитать в Play Console перед сабмитом (v4-правило).
JA-тексты — native review до релиза (ADR-014, остаётся в силе).
