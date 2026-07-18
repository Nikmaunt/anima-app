# Матрица моделей для слота «локальный разум» (v0.5, 2026-07-18)

Собрано веб-ресёрчем этой сессии (агенты, свежесть 2026-07). Всё несверенное
помечено UNVERIFIED. Решение — в ADR-017; реализация — `MindModelRegistry`.

## Контекст 2026, изменивший картину

- **Gemma 4** (2026-04-02): E2B/E4B/26B-MoE/31B, лицензия **Apache 2.0**
  (впервые у Gemma), 140+ языков, готовый `.litertlm` у litert-community.
- **Qwen3.5** (2026-03-02): 0.8B/2B/4B/9B, 201 язык, Apache 2.0 — но для
  Android пока только GGUF (litert-артефактов не найдено).
- MediaPipe LLM Inference API — официально **maintenance-only**, развитие
  ушло в LiteRT-LM (см. ADR-016).
- **Gemma 3 1B (текущая модель v0.2–v0.4) — официально English-only.**
  Это делает костыль ADR-014 («существо пока говорит по-английски»)
  свойством модели, а не приложения — и мотивирует всю Фазу 1.

## Матрица

| Модель | Q4-файл | Формат для нас | RAM-пик (замер) | tok/s (флагман) | RU/DE/ES/JA/PL | Лицензия / asset pack | Контекст / шаблон |
|---|---|---|---|---|---|---|---|
| Gemma 3 1B it | 0.53 GB .task | .task + .litertlm готовые | 1.0 GB CPU / 1.2 GB GPU (S24U) | 55 CPU / 49 GPU (S24U) | **нет — EN-only** | Gemma ToU: можно, с обвязкой | 2k артефакт; PLAIN проверен |
| Gemma 3n E2B | ~3 GB .litertlm (UNVERIFIED) | .litertlm | нет публичного замера | 16 CPU/GPU (S24U) | да — 140 языков | Gemma ToU | 32k arch |
| Gemma 4 E2B | 2.58 GB .litertlm (2.0 text-only) | .litertlm готовый | 0.68 GB GPU S26U / 1.45 GB iPhone 17 Pro | 52 GPU (S26U) | да — 140+ | **Apache 2.0** | 32k |
| Qwen3 0.6B | ~0.4–0.5 GB | **.litertlm готовый** (litert-community) | UNVERIFIED | 5–10 (6GB-класс, GGUF) | заявлено 119; качество на 0.6B слабое | Apache 2.0 | 32k; ChatML |
| **Qwen3 1.7B** | ~1.0–1.2 GB (UNVERIFIED) | GGUF; litert-конверсия UNVERIFIED | ~1.5–2 GB (оценка) | 10–20 (GGUF) | **да — 119 языков** | Apache 2.0 | 32k; ChatML |
| **Qwen2.5 1.5B** | 1.6 GB int8 .task; int4 собрать самим (UNVERIFIED) | **.task + .litertlm готовые** | **2.0 GB CPU / 1.85 GB GPU (S25U, int8)** | 34 CPU / 31 GPU (S25U) | **да — 29 языков, все 5 подтверждены** | Apache 2.0 | 32k arch; ChatML |
| Llama 3.2 1B | ~0.8 GB | .task (litert-community) | UNVERIFIED | ~класс Gemma 1B (UNVERIFIED) | нет: DE/ES есть, RU/JA/PL нет | Llama Community: обвязка «Built with Llama» | 128k arch |
| SmolLM2 1.7B | ~1.0 GB | GGUF/ONNX | UNVERIFIED | UNVERIFIED | **нет — EN-only** | Apache 2.0 | 8k; ChatML |
| SmolLM3 3B | ~1.7 GB (UNVERIFIED) | GGUF | UNVERIFIED | низкая (3B) | частично: DE/ES; RU слаб, JA/PL нет | Apache 2.0 | 64k; ChatML |
| Qwen3.5 2B (наблюдать) | ~1.2 GB Q4_K_M | GGUF only | UNVERIFIED | 10–18 (GGUF) | да — 201 язык | Apache 2.0 | ChatML |

Замеры S24U/S25U/S26U — из публичных бенчмарков litert-community карточек и
блогов (ссылки в research-v5.md §B); GGUF-цифры не переносимы на MediaPipe
напрямую (другой рантайм) — при выборе это учтено как риск, снимается
бейк-офф харнессом на живом S24 (чеклист §0).

## Лицензии — редистрибуция в asset pack

- **Apache 2.0** (Qwen 2.5/3/3.5, SmolLM, Gemma 4): кладём в pack, шипим
  LICENSE/NOTICE. Чисто.
- **Gemma ToU** (Gemma 3/3n): редистрибуция разрешена при: копии соглашения
  получателю, NOTICE-файле, флоу-дауне Prohibited Use Policy в EULA, пометке
  изменённых файлов. Плюс right-to-terminate у Google. Работало для v0.2–v0.4
  (GEMMA_NOTICE.txt в паке), но Apache-модель снимает весь этот хвост.
- **Llama Community License**: формально можно (обвязка «Built with Llama»,
  копия лицензии, AUP), но без RU/JA/PL это бессмысленно для нас.

## Выбор (детали и последствия — ADR-017)

1. **Дефолт пака v0.5: Qwen2.5-1.5B-Instruct** — единственный кандидат с
   ГОТОВЫМИ артефактами под наш текущий рантайм (.task; litert-community),
   чистой Apache 2.0 и подтверждённым покрытием всех пяти языков.
   Известный компромисс: int8-артефакт 1.6 GB превышает лимит fast-follow
   пака Play (1.5 GB) — в пак идёт **int4-сборка (~1.1 GB, собрать
   litert-инструментами — UNVERIFIED до фактической сборки)**; до её
   появления пак может продолжать нести Gemma 3 1B, а Qwen приходит через
   download/SAF. Реестр и слот принимают оба файла уже сейчас.
2. **Альтернатива-цель: Qwen3-1.7B** (119 языков, свежее поколение) — как
   только подтверждается litert-конверсия; вписывается в 1.5 GB int4.
3. **Премиум-альтернатива: Gemma 4 E2B** (.litertlm 2.6 GB, Apache 2.0,
   140+ языков) — для устройств 12 GB+ через download/SAF; в пак не лезет.
4. Gemma 3 1B остаётся в реестре как legacy (EN-only, маршрутизация Фазы 1D
   честно показывает плашку «отвечает по-английски»).
