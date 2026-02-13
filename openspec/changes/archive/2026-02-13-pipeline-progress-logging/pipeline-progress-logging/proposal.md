## Why

When running the application, it's difficult to understand what step the pipeline is currently executing and how far along it is. Key operations like LLM calls, TTS generation, and source polling lack timing information, and there's no clear start/end boundary logging for the overall pipeline run. This makes it hard to monitor progress and diagnose slowdowns in production.

## What Changes

- Add structured start/end logging with elapsed time for each pipeline stage (source polling, article processing, briefing composition, TTS generation)
- Add batch progress logging for article processing (e.g., "Processing article 3/12")
- Add timing metrics for LLM calls and TTS API calls
- Elevate key `debug`-level logs to `info` where they provide operational visibility (e.g., `SourcePollingScheduler`)
- Add a top-level pipeline run log that summarizes the full generation cycle with total elapsed time

## Capabilities

### New Capabilities

- `pipeline-observability`: Structured info-level logging across all pipeline stages with timing, progress counters, and run summaries

### Modified Capabilities

_None — this change adds logging statements to existing code without changing any functional requirements._

## Impact

- **Code**: Logging additions across `BriefingGenerationScheduler`, `LlmPipeline`, `ArticleProcessor`, `BriefingComposer`, `TtsPipeline`, `TtsService`, `SourcePollingScheduler`, `SourcePoller`
- **APIs**: No API changes
- **Dependencies**: No new dependencies (uses existing SLF4J/Logback)
- **Performance**: Negligible — info-level string formatting only
