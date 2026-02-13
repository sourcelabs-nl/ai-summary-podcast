## Context

The application runs a multi-stage pipeline: source polling → LLM processing (article filtering + summarization + briefing composition) → TTS generation. Most stages already have some info-level logging, but there are gaps:

- **ArticleProcessor** logs individual results but not batch progress (no "3/12" counters)
- **BriefingComposer** only logs completion, not start
- **SourcePollingScheduler** uses debug-level for its polling check — invisible at default log level
- No stage logs elapsed time, making it hard to spot slowdowns
- No top-level summary of a full pipeline run

All logging uses SLF4J with Logback (Spring Boot default). No custom logging configuration exists.

## Goals / Non-Goals

**Goals:**
- Make each pipeline stage clearly visible in logs at INFO level
- Add elapsed time to long-running operations (LLM calls, TTS generation, full pipeline run)
- Add batch progress counters where multiple items are processed sequentially
- Keep log output concise and scannable

**Non-Goals:**
- Structured/JSON logging format — out of scope, can be added later
- Metrics/observability framework (Micrometer, OpenTelemetry) — separate concern
- Custom log configuration (logback-spring.xml) — not needed for this change
- Correlation IDs across log statements — would require threading context, out of scope

## Decisions

**1. Use `kotlin.time` for elapsed time**
Kotlin's stdlib provides `measureTimedValue` which cleanly captures both the result and duration. For methods with multiple return points (e.g. `BriefingGenerationScheduler.generateBriefing`), use `TimeSource.Monotonic.markNow()` + `elapsedNow()` instead, which works naturally with early returns.

Alternative considered: Spring StopWatch — heavier API, no advantage for simple timing.

**2. Log elapsed time at stage boundaries, not individual calls**
Timing will be added at the level of: full pipeline run, article processing batch, briefing composition, TTS pipeline. Individual LLM/TTS API call timing is not added — the batch-level timing is sufficient to identify which stage is slow.

Alternative considered: Timing every LLM call — too noisy, and ArticleProcessor already logs per-article results.

**3. Use consistent log message format**
All pipeline progress logs will follow: `[Stage] action — detail`. Example:
```
[Pipeline] Starting briefing generation for podcast abc-123
[LLM] Processing articles: 3/12
[LLM] Article processing complete — 12 articles in 45.2s (8 relevant)
[TTS] Generating audio — 6 chunks
[Pipeline] Briefing generation complete for podcast abc-123 — total 92.3s
```

This makes logs grep-friendly by stage prefix.

**4. Elevate SourcePollingScheduler to info level**
The current `log.debug("Checking {} enabled sources for polling")` is useful operational information. Elevate to `log.info` so it's visible at default log level.

## Risks / Trade-offs

**[Slightly more verbose logs]** → Acceptable trade-off for operational visibility. All additions are info-level and can be suppressed per-package if needed.

**[Timing overhead]** → `measureTimedValue` uses `System.nanoTime()` internally — negligible compared to LLM/TTS API calls that take seconds.
