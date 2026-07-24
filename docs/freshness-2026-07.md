# Freshness research — v0.7 (2026-07-24)

Три независимых веб-исследования этой сессии: (A) рантайм LiteRT-LM /
tasks-genai, (B) политики Play + EU AI Act, (C) свежесть зависимостей.
Каждый факт — с датой проверки и ссылкой; гипотезы помечены явно.
Выводы в ADR-020 (рантайм) и аддендумах.

## §B. Политики Play и EU AI Act (проверено 2026-07-24)

### GenAI-политика Play
- «AI-Generated Content» policy прямо включает text-to-text
  conversational chatbots; локальность модели скоуп не меняет.
  Обязательное требование: in-app флаг/репорт оффенсивного контента без
  выхода из приложения (с 31.01.2024).
  https://support.google.com/googleplay/android-developer/answer/13985936
  → У Anima уже есть (v0.6, long-press → report; audit-v06 CONFIRMED).
- Safety testing — «Best Practices to Safeguard AI-Generated Content»
  (SAIF, OWASP GenAI Red Teaming): рекомендации, не требования.
  https://support.google.com/googleplay/android-developer/answer/16353813
  Утверждение «Google может запросить документацию тестирования» — из
  поисковой выдачи Help Center, при фетче 2026-07-24 в явном виде не
  подтверждено (частично подтверждено).
- Отдельной GenAI-декларации в Play Console по состоянию на 2026-07-24
  не обнаружено (гипотеза: вопросы появляются в Data safety/ratings).
- Анонс политики 2026-07-15: User Data requirements распространены на
  сторонние AI-интеграции (disclosure/consent, срок ≥30 дней с 15.07).
  https://support.google.com/googleplay/android-developer/answer/17134731
  → Релевантно BYOK: явный consent-экран при включении облака — в
  backlog v0.8 (сейчас облако включается вручную с вводом endpoint+key,
  что уже является осознанным действием; формальный consent-текст
  усилит соответствие).

### Target API
- К 31.08.2026 новые приложения/обновления — target API 36; существующие
  — минимум API 35; продление до 01.11.2026 по запросу.
  https://support.google.com/googleplay/android-developer/answer/11926878
  → targetSdk 36 (audit-v06 CONFIRMED) закрывает 2026 полностью.

### Верификация разработчиков
- С 30.09.2026 установка приложений на сертифицированных устройствах в
  Бразилии, Индонезии, Сингапуре, Таиланде требует верифицированного
  разработчика; глобально — 2027. Play-разработчики покрыты обычным
  онбордингом (>99% авто-зарегистрированы).
  https://android-developers.googleblog.com/2026/06/android-developer-verification.html
  https://support.google.com/android-developer-console/answer/16561738
  → Closed testing НЕ блокируется.

### Closed testing (персональный аккаунт)
- Подтверждено дословно: «a minimum of 12 testers who have been opted-in
  for at least the last 14 days continuously» (аккаунты после
  13.11.2023). 20→12 снижено 11.12.2024; в 2025–2026 без изменений.
  Opt-out обнуляет отсчёт.
  https://support.google.com/googleplay/android-developer/answer/14151465
  → closed-testing-plan.md актуален.

### EU AI Act с 02.08.2026 (анализ, не юрконсультация)
- Ст. 50 (transparency) применяется с 02.08.2026; Digital Omnibus
  (июнь 2026) перенёс high-risk дедлайны (Annex III → 02.12.2027), но
  ст. 50 оставил; для систем, вышедших до 02.08.2026, машиночитаемая
  маркировка 50(2) — переходный срок до 02.12.2026.
  https://digital-strategy.ec.europa.eu/en/policies/guidelines-transparency-ai-generated-content
  https://www.gibsondunn.com/eu-ai-act-omnibus-agreement-postponed-high-risk-deadlines-and-other-key-changes/
- Анализ для Anima: разработчик — вероятно провайдер AI-СИСТЕМЫ
  (интеграция GPAI-модели в продукт); on-device не выводит из-под акта.
  50(1) имеет исключение «obvious … reasonably well-informed person»
  (https://artificialintelligenceact.eu/article/50/): для виртуального
  питомца, где пользователь сам приносит модель и осознанно говорит с
  ИИ-существом, ИИ-природа очевидна из контекста — исключение, скорее
  всего, применимо. Дешёвая страховка: строка «отвечает при помощи ИИ»
  в онбординге (уже частично покрыто контрактом онбординга и
  Trust-экраном; отдельная явная строка — backlog v0.8).
- 50(2) для приватного неопубликуемого чат-вывода: явного исключения не
  найдено на 2026-07-24 (гипотеза о низком практическом риске);
  вариант буквального соответствия — метаданные в экспортируемом
  тексте. Отслеживать финальные гайдлайны Комиссии от 20.07.2026.
- 50(4) (deepfakes/публичные тексты) — не применимо: Anima ничего не
  публикует.

## §A. Рантайм: tasks-genai / LiteRT-LM (проверено 2026-07-24)

### MediaPipe tasks-genai
- Официально **maintenance-only**: «now in maintenance-only mode. New
  features and optimizations will be focused on LiteRT-LM», с прямой
  рекомендацией мигрировать на LiteRT-LM Android (Kotlin) API.
  https://developers.google.com/edge/mediapipe/solutions/genai/llm_inference
- Последняя версия на Google Maven — **0.10.35**, lastUpdated
  2026-04-27; с апреля релизов нет (наш пин уже 0.10.35).
  https://dl.google.com/android/maven2/com/google/mediapipe/tasks-genai/maven-metadata.xml
- .litertlm в tasks-genai подтверждён для официальных моделей (Gemma 3n
  и т.п.); для КАСТОМНЫХ .litertlm из litert-torch гарантий нет
  (гипотеза: новые версии формата в maintenance-режиме поддерживаться
  не будут) — ещё один аргумент за второй движок.

### LiteRT-LM
- Артефакты группы com.google.ai.edge.litertlm на Google Maven:
  **litertlm-android 0.14.0** и **litertlm-jvm 0.14.0** (идентичные
  наборы версий; Windows нативно с v0.11.0). Релиз v0.14.0 —
  2026-07-08. https://dl.google.com/android/maven2/com/google/ai/edge/litertlm/group-index.xml,
  https://github.com/google-ai-edge/LiteRT-LM/releases
- Kotlin API помечен «Stable» в README, но версия 0.x, semver не обещан
  (issue #2292 фиксирует недокументированные breaking-изменения
  0.10.x→0.11.0). Поверхность: `Engine`, `EngineConfig(modelPath,
  backend)`, `engine.initialize()`, `engine.createConversation()` →
  `Conversation.sendMessage / sendMessageAsync → Flow`; бэкенды —
  `Backend.CPU()` / `Backend.GPU()` / NPU; ошибки —
  `LiteRtLmJniException`. (В Kotlin API «Session» НЕТ — это C++-слой.)
  https://developers.google.com/edge/litert-lm/android

### GPU на Samsung — статус issues (все проверены 2026-07-24, все open)
- **#2114**: GPU Engine init fails — Galaxy **S26** Exynos (Xclipse 960
  + ANGLE-CL), Clspv отклоняет MLDrift-кернел. Открыт 2026-04-29, НЕ
  починен, без версии фикса.
- **#2292**: v0.11.0 `Engine.initialize()` fails on **S24 Ultra**
  (Adreno 750) — OpenCL discovery заблокирован One UI, OpenGL-fallback
  UNIMPLEMENTED; наружу generic LiteRtLmJniException. **CPU работает**
  (~12.6 s init, ~1.66 tok/s). Автофолбэка GPU→CPU в рантайме НЕТ.
- Кластер: #2611 (Xclipse 530 GPU-мусор в decode), #1864 (Exynos 2600
  крэш), #2211 (GPU-сэмплеры не dlopen-ятся). Отдельного issue про
  **Exynos 2400 / Xclipse 940 / базовый S24 не найдено**; гипотеза:
  тот же ANGLE-CL/Clspv путь → тот же исход. #2080: sampler-параметры
  на GPU/NPU молча игнорируются, на CPU работают.
- → Решение v0.7: **CPU-only, запрещено кодом и тестом** (не
  «try-GPU→catch», как допускал ADR-016 — GPU не пробуем вовсе, пока
  жив кластер issues; пересмотр — отдельным решением при закрытии
  #2114/#2292).

### Формат .litertlm / конвертация
- Формат — FlatBuffers-контейнер (header + секции); официальный тулинг
  litert-lm-builder. **Матрицы совместимости «версия рантайма ↔ версия
  формата» не существует** (проверено: доки file_builder + README).
  https://developers.google.com/edge/litert-lm/file_builder
- litert-torch: latest **0.9.1** (2026-05-19); export_hf выдаёт
  .litertlm «compatible for CPU and GPU inference»; int4 = blockwise
  INT4 (group 32) + INT8 embeddings.
  https://pypi.org/project/litert-torch/
- **#1748** (открыт 2026-03-26, без ответа мейнтейнеров): комьюнити
  int4 block32/128 конверсии Qwen2.5-1.5B/Phi-4-mini **стабильно
  работают на CPU (Linux и Pixel 8)**; desktop-GPU ненадёжен. Статус
  «официальной поддержки» комьюнити-конверсий не подтверждён.
  → Считать: «litert-torch 0.9.1 → LiteRT-LM 0.14.0 CPU — работает;
  GPU для кастомных int4 — не гарантировано»; доверять связке версий,
  подтверждённой смоуком, а не формату.
- Наименьший готовый .litertlm в litert-community (перебор через HF
  API): **litert-community/gemma-3-270m-it →
  `gemma3-270m-it-q8.litertlm`, 304 005 120 байт (~290 MiB)**.
  У SmolLM-135M и Qwen2.5-0.5B файлов .litertlm нет вовсе.
  https://huggingface.co/litert-community/gemma-3-270m-it

### JVM-хост (для смоука)
- `com.google.ai.edge.litertlm:litertlm-jvm:0.14.0` (google() repo),
  API идентичен Android. Windows x64 — нативно; #1857 (Windows GPU
  segfault, CPU работает), #2612 (нет ARM64-бинарей). → Смоук: JVM +
  Backend.CPU() + gemma3-270m-it-q8.litertlm.

## §C. Зависимости (проверено 2026-07-24 по Google Maven / Maven Central)

### Security-вывод (главное)
- Единственная CVE-история квартала по нашему стеку: SQLite FTS5 до
  3.53.2 — CVE-2026-11822 (memory corruption, CVSS 8.5) и
  CVE-2026-11824 (heap overflow fts5ChunkIterate, CVSS 7.8), исправлены
  в SQLite 3.53.2 (2026-06-03). **Наш пин SQLCipher 4.17.0 (релиз
  2026-07-08) несёт SQLite 3.53.3 — обе CVE уже закрыты**; новее 4.17.0
  нет. НЕ откатываться ниже 4.17.0.
  https://github.com/advisories/GHSA-6qj8-gw6p-hc5p
  https://github.com/advisories/GHSA-8g48-4wfm-7247
  https://www.zetetic.net/blog/2026/07/08/sqlcipher-4.17.0-release/
- По остальным пинам опубликованных CVE/advisory не найдено (NVD/GHSA,
  2026-07-24). **Security-обязательных бампов НЕТ.**

### Доступные обновления в своих линиях (выборка)
| Пин | Доступно | Решение v0.7 |
|---|---|---|
| AGP 8.13.0 | 8.13.2 (патч) | бампить (Фаза 3.3) |
| lifecycle 2.9.0 | 2.9.4 (патч) | бампить |
| navigation 2.9.0 | 2.9.8 (патч) | бампить |
| WorkManager 2.10.1 | 2.10.5 (патч) | бампить |
| truth 1.4.4 / turbine 1.2.0 | 1.4.5 / 1.2.1 (патчи) | бампить |
| Kotlin 2.2.0 | 2.2.21 (+KSP 2.2.21-2.0.5, Hilt 2.60.1 — атомарная связка) | НЕ в этом прогоне: без security-нужды связка toolchain не трогается |
| Compose BOM 2025.06.01 | 2025.12.01 (M3 1.3.2→1.4.0!) | НЕ в этом прогоне: визуальный риск для 46 голденов без security-нужды |
| Glance 1.2.0-rc01 | стабильного 1.2.0 НЕ вышло; линия ушла в 1.3.0-alpha | держать rc01, ждать 1.3.0 stable |
| Room 2.7.2 + sqlite 2.5.2 | связанная пара (2.8.4 требует sqlite 2.6+) | не трогать |
| detekt 1.23.8 | последний 1.x; 2.0 не stable (dev.detekt alpha) | не трогать |
| ktlint-gradle 13.1.0 / engine 1.7.1 | 13.x патчей нет; engine 1.7.2 | engine можно, не обязателен |
| Robolectric 4.14.1 / Roborazzi 1.69.0 | 4.16.1 / 1.70.0 (миноры) | не в этом прогоне (тестовая инфра, без нужды) |
| mediapipe 0.10.35, asset-delivery 2.3.0, splashscreen 1.2.0, junit 4.13.2, benchmark/profileinstaller 1.4.1, androidx.test * | latest | без изменений |
| ML Kit genai-prompt beta3 | beta4 доступна | не в этом прогоне: verified-API beta3 (память v0.1), бета-бамп без нужды не делается |

Обоснование фильтра: мандат v0.7 — «только патчи/минорные с
security-обоснованием»; security-нужды нет, поэтому берём только чистые
same-line патчи с нулевой поверхностью API (build-plumbing и test-libs),
тремя атомарными батчами с полным зелёным контуром после каждого
(Фаза 3.3). Всё отложенное — кандидаты v0.8 с прогоном голденов.


