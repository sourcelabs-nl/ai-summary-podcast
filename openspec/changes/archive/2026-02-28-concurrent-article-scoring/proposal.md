## Why

Article scoring and summarization (Stage 1 of the LLM pipeline) processes articles sequentially, making one LLM HTTP call at a time. Each call takes 2-5 seconds, so a batch of 20 articles takes 40-100 seconds. Since these calls are independent, they can run concurrently with fault isolation — one failure should not cancel the others.

## What Changes

- `ArticleScoreSummarizer.scoreSummarize()` will process articles concurrently using Kotlin coroutines with `supervisorScope`, so individual article failures don't cancel sibling tasks
- Uses `runBlocking(Dispatchers.IO)` as the bridge from the non-suspend caller, matching the existing pattern in `InworldTtsProvider`
- Individual article failures are caught and logged, returning only successfully processed articles (preserving current error semantics)

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `llm-processing`: Stage 1 (score+summarize) SHALL process articles concurrently rather than sequentially. Individual article failures SHALL NOT cancel processing of other articles.

## Impact

- `ArticleScoreSummarizer.kt` — main change: concurrent processing with coroutines
- `ArticleScoreSummarizer` test — may need updates for coroutine context
- No API changes, no database schema changes, no new dependencies (kotlinx.coroutines already in use)
