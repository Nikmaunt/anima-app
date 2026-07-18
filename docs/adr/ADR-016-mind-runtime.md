# ADR-016: Рантайм локального разума — dual-runtime поэтапно, v0.5 остаётся на tasks-genai

Дата: 2026-07-18 (v0.5 Фаза 1A). Статус: принято.

## Контекст

MediaPipe LLM Inference API (`tasks-genai`), несущий GEMMA-тир с v0.2,
официально переведён в **maintenance-only**; Google прямо рекомендует
мигрировать на **LiteRT-LM** (v0.14.0, 2026-07-08, Maven
`com.google.ai.edge.litertlm:litertlm-android`). Полные ссылки —
docs/research-v5.md §A.

Что даёт LiteRT-LM: Kotlin-first API, поддержку новых моделей (Gemma 4,
Qwen, Phi), NPU/GPU-бэкенды и — критично для извлечения фактов — настоящий
**constrained decoding** (JSON Schema / Regex / Lark через LLGuidance).

Что останавливает сегодня:
1. Версия 0.x, релизы ежемесячно, semver не обещан.
2. Constrained decoding задокументирован только в C++ API; из Kotlin — лишь
   tool-calling-обход.
3. GPU-бэкенд фрагментирован: крэши на Tensor (нет libOpenCL, без
   auto-fallback), фейлы Exynos Xclipse, NPU требует вендорных библиотек.
4. Наш проверенный сценарий (Gemma .task на S24, GMD-эмулятор без GPU)
   рантайм-миграцией ничего не выигрывает прямо сейчас: MediaPipe читает
   И `.task`, И `.litertlm` — многомодельность (ADR-017) достигается без
   смены рантайма.

## Решение

**Dual-runtime как направление, поэтапно; v0.5 не подключает LiteRT-LM.**

- v0.5: реестр моделей (ADR-017) + формат-слой (PLAIN/CHATML, стоп-токены,
  StreamTrimmer) живут в `core/model` и НЕ привязаны к рантайму. GEMMA-тир
  остаётся на tasks-genai, который читает оба формата файлов.
- v0.6 (гейт): добавить `LiteRtMindEngine` за тем же `MindEngine`-интерфейсом,
  когда выполнится любое из: (а) Kotlin-API constrained decoding
  задокументирован, (б) нужный модельный артефакт существует только в
  `.litertlm` и не открывается MediaPipe, (в) tasks-genai получает
  деградацию/EOL-датировку. Первая цель — извлечение фактов по JSON-схеме
  вместо tolerant-парсера FactJson.
- Обязательный паттерн при подключении: try-GPU → catch → CPU-fallback;
  на GMD только CPU.
- INTERNET-инвариант не меняется: LiteRT-LM — on-device библиотека, попадёт
  только в `:core:mind`; NetworkIsolationTest продолжает пинить бюджет.

## Последствия

+ Проверенный стек не трогается в релиз v0.5; многоязычие получено данными.
+ Формат-слой уже готов к обоим рантаймам; миграция сведётся к одному классу.
− Извлечение фактов до v0.6 остаётся на tolerant-парсере (FactJson), без
  жёсткой схемы.
− Отслеживать судьбу tasks-genai в каждом релизном цикле (research-чек).
