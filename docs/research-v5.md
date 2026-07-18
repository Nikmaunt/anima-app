# Research v5 — Разум v2, локализация, продукт (2026-07-18)

Собрано веб-агентами этой сессии 2026-07-18. Несверенное помечено UNVERIFIED.

## §A. Рантайм: LiteRT-LM vs MediaPipe tasks-genai

- LiteRT-LM v0.14.0 (2026-07-08), релизы ~ежемесячно; Maven `com.google.ai.edge.litertlm:litertlm-android:0.14.0` (Google Maven); Kotlin-first API (Engine/Conversation, `sendMessageAsync` → Flow). Kotlin/C++/Python «Stable», но версия 0.x — semver не гарантирован.
  - https://github.com/google-ai-edge/LiteRT-LM/releases
  - https://developers.google.com/edge/litert-lm/android
  - https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md
- minSdk 31 (Android 12+) по DeepWiki-гайду — точное значение UNVERIFIED (наш minSdk = 31, совместимо).
- MediaPipe LLM Inference **официально maintenance-only**: «We recommend migrating your projects to LiteRT-LM» — https://developers.google.com/edge/mediapipe/solutions/genai/llm_inference. Патчи продолжаются (v0.10.35, 2026-04-28). Это maintenance, не deprecation.
- Форматы: MediaPipe принимает **и .task, и .litertlm**; LiteRT-LM документирует только .litertlm (чтение .task — UNVERIFIED/не заявлено). Готовые модели: https://huggingface.co/litert-community (многие репо несут оба формата).
- Constrained decoding: **LiteRT-LM умеет** (Regex/JSON Schema/Lark через LLGuidance) — но задокументировано только в C++ API (https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/cpp/constrained-decoding.md); из Kotlin — только tool-calling (@Tool/OpenApiTool). tasks-genai constrained decoding НЕ поддерживает (только topK/temp/seed/LoRA).
- Стабильность на устройствах (issues 2026): GPU-бэкенд фрагментирован — Pixel 8 Pro крэш (нет libOpenCL, нет auto-fallback, #1860), Galaxy S26 Exynos init fail (#2114), Qualcomm NPU требует вендорных либ (#1377). CPU-бэкенд относительно надёжен. В проде обязателен try-GPU-catch-CPU.

## §B. Матрица моделей — см. docs/model-matrix.md

Ключевые сдвиги 2026:
- **Gemma 4 (2026-04-02)**: E2B/E4B/26B-MoE/31B, лицензия **Apache 2.0** (впервые у Gemma), 140+ языков, готовый .litertlm 2.58 GB (E2B). https://cloud.google.com/blog/products/ai-machine-learning/gemma-4-available-on-google-cloud , https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm
- **Qwen3.5 (2026-03-02)**: 0.8B/2B/4B/9B, 201 язык, Apache 2.0 — но на Android пока только GGUF (litert-конверсия не найдена). https://huggingface.co/Qwen/Qwen3.5-0.8B
- Gemma 3 1B (текущая модель) — **официально English-only**: мультиязычного качества RU/PL/JA от неё ждать нельзя; это подтверждает костыль ADR-014.
- SmolLM2 — English-only; SmolLM3-3B — 6 языков без RU/JA/PL. Llama 3.2 1B — 8 языков, без RU/JA/PL, лицензия с обвязкой («Built with Llama», префикс имени производных).
- Qwen2.5-1.5B-Instruct — 29 языков (RU/DE/ES/JA/PL подтверждены), Apache 2.0, **готовые .task и .litertlm** (https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct), замеры S25U: 34 tok/s CPU, RAM-пик 2.0 GB (int8).
- Qwen3 1.7B — 119 языков, Apache 2.0, ~1.0–1.2 GB Q4, но litert-артефакт не найден (конверсия litert-torch — путь подтверждён для Qwen3-0.6B).
- Лицензия Gemma ToU (Gemma 3/3n): редистрибуция в pack разрешена с обвязкой (копия соглашения, NOTICE, флоу-даун PUP, right-to-terminate у Google). https://ai.google.dev/gemma/terms

## §C. Облачные BYOK-пресеты (проверено 2026-07)

См. таблицу в research-cloud-presets (перенесена в §C.1 ниже). Главное:
- Живые free-тиры: OpenRouter (`:free`-модели, 50–1000 req/день), Groq (30 RPM без карты), Google AI Studio (Flash бесплатен, Pro платный с 2026-04), Mistral Experiment (~2 RPM), Cerebras (1M tok/день, волатильный список моделей).
- Together: free-endpoint снят. OpenAI/Anthropic: без free-тиров; у Anthropic есть **официальный OpenAI-compat слой** (base `https://api.anthropic.com/v1/`, Bearer): https://platform.claude.com/docs/en/api/openai-sdk
- Валидация ключа: у всех `GET {base}/models` (Bearer) → 200/401, КРОМЕ OpenRouter (`/models` публичный → валидировать `GET /api/v1/key`) и Anthropic-compat (models UNVERIFIED → дешёвый POST chat/completions max_tokens=1).
- Free-тиры волатильны (Google урезал 2026-04, Together снял, Cerebras меняет модели) → пресет = base-url + префикс ключа + предзаполненная модель, подтверждаемая пользователем.

### §C.1 Таблица пресетов
| Провайдер | Base URL | Free-тир | Модель по умолчанию | Префикс ключа |
|---|---|---|---|---|
| OpenRouter | https://openrouter.ai/api/v1 | да | qwen/qwen3-next-80b-a3b-instruct:free | sk-or-v1- |
| Groq | https://api.groq.com/openai/v1 | да | openai/gpt-oss-20b | gsk_ |
| Google AI Studio | https://generativelanguage.googleapis.com/v1beta/openai/ | Flash | gemini-3-flash (ID сверять по /models, UNVERIFIED) | AIza |
| Mistral | https://api.mistral.ai/v1 | да | mistral-small-latest | (32 alnum) |
| Cerebras | https://api.cerebras.ai/v1 | да | из /models (волатильно) | csk- |
| OpenAI | https://api.openai.com/v1 | нет | gpt-5-mini | sk- |
| Anthropic | https://api.anthropic.com/v1 | нет | claude-haiku-4-5 | sk-ant- |

Источники: openrouter.zendesk.com/hc/en-us/articles/39501163636379 · console.groq.com/docs/rate-limits · ai.google.dev/gemini-api/docs/openai · docs.mistral.ai/admin/user-management-finops/tier · inference-docs.cerebras.ai/support/rate-limits · developers.openai.com/api/docs/pricing · platform.claude.com/docs/en/api/openai-sdk · together.ai/models/llama-3-3-70b-free

## §D. Продуктовый ресёрч (Фаза 3) — конкуренты и тренды

### Конкуренты
- **Finch**: любят «no-guilt» заботу и цель «разреши себе отдохнуть»; жалобы — агрессивный limited-offer после вылупления, разрыв цен Android $70/год vs iOS $15/год, краши с потерей питомца, «геймификация → обязаловка к 3 месяцу». $30M ARR без VC (UNVERIFIED, один источник). https://www.metafilter.com/207203/Habit-forming-for-good-with-Finch · https://habitbox.app/blog/finch-app-review
- **Widgetable**: любят co-parenting (4.8/859K); жалобы — реклама на каждое действие, Pro $69.99, питомец ломается без сети (антипаттерн). https://justuseapp.com/en/app/1641107226/widgetable-lock-screen-widget/reviews
- **Forest**: механика вины («убил дерево») отталкивает; жалобы на батарею 10–20%/день (UNVERIFIED, вторичный источник); статистика ушла в подписку. https://www.primeproductiv4.com/apps-tools/forestapp-review
- **one sec**: доказан эффект (PNAS, −57% открытий соцсетей), но пауза «протапывается на автопилоте» со временем. https://www.pnas.org/doi/10.1073/pnas.2213114120
- **Opal**: $99.99/год, VPN-подход → privacy-опасения. https://justuseapp.com/en/app/1497465230/opal-save-time-daily/reviews

### Тренды 2026
- Сдвиг «supportive, not scolding»; микро-привычки вместо streak-давления. https://liveintently.app/blog/best-digital-wellbeing-apps-2026/ · https://dasroot.net/posts/2026/04/digital-wellbeing-apps-what-actually-works/
- Тамагочи-ренессанс CES 2025–2026: сдвиг к «долгим отношениям с памятью» (Sweekar, Zumi). https://www.ibtimes.co.uk/ces-2026-tamagotchi-clone-sweekar-does-more-give-people-virtual-pet-1768600
- Calm Tech Institute + сертификация «Calm Tech Certified». https://calmtech.com/
- Google забросил системный Digital Wellbeing → ниша открыта. https://9to5google.com/2026/02/20/google-has-ignored-androids-digital-wellbeing-tools-for-years-so-whats-next/
- Parasocial/attachment research: приватность = этическое ядро; асимметрия власти платформы «переписать» компаньона апдейтом — страх пользователей. https://www.sciencedirect.com/science/article/pii/S2949882126000757 · https://arxiv.org/pdf/2508.09998 · https://arxiv.org/pdf/2603.23315

### Барометр (погодное чувство без сети)
- `Sensor.TYPE_PRESSURE`: флагманы да (S22–S24, Pixel 7–10 подтверждены), мидрейндж часто нет → graceful degradation обязательна. https://www.gsmarena.com/results.php3?chkBarometer=selected
- Алгоритм Zambretti (~90% на 12 ч, давление+сезон): https://en.wikipedia.org/wiki/Zambretti_Forecaster · https://integritext.net/DrKFS/zambretti.htm · https://github.com/HAuser1234/homeassistant-local-weather-forecast
- Шум от смены высоты (карман/этажи) → сглаживание тренда за часы; публикаций про телефонный шум не нашли (UNVERIFIED, инженерный вывод).
