## 1. Pipeline run logging with elapsed time

- [x] 1.1 Add `[Pipeline]` start/complete logging with `measureTimedValue` in `BriefingGenerationScheduler` wrapping the full generation cycle
- [x] 1.2 Ensure early-exit paths (no sources, no articles, pending episode) still log completion with elapsed time and reason

## 2. Article processing progress logging

- [x] 2.1 Add batch progress logging in `ArticleProcessor` — log `[LLM] Processing article {i}/{total}: '{title}'` for each article
- [x] 2.2 Add batch summary logging in `LlmPipeline` after article processing — log total count, relevant count, and elapsed time with `[LLM]` prefix

## 3. Briefing composition logging

- [x] 3.1 Add `[LLM]` start log in `BriefingComposer` before the LLM call and wrap composition in `measureTimedValue` to log elapsed time on completion

## 4. TTS pipeline start logging

- [x] 4.1 Add `[TTS]` start log in `TtsPipeline` before audio generation begins

## 5. Source polling visibility

- [x] 5.1 Elevate `SourcePollingScheduler` debug log to info level and add `[Polling]` prefix

## 6. Consistent log prefixes

- [x] 6.1 Update existing log messages in `LlmPipeline`, `ArticleProcessor`, `BriefingComposer`, `TtsPipeline`, and `SourcePoller` to use the bracketed prefix format (`[Pipeline]`, `[LLM]`, `[TTS]`, `[Polling]`)
