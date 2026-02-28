## Context

`ArticleScoreSummarizer.scoreSummarize()` processes articles sequentially via `.mapNotNull {}`. Each article makes an independent LLM HTTP call (2-5s each). With 20 articles, this takes 40-100 seconds. The LLM calls are independent — there's no data dependency between articles.

The project already uses coroutines with `supervisorScope` in `SourcePollingScheduler` (parallel host group polling) and `runBlocking` + `async` in `InworldTtsProvider` (parallel TTS chunk synthesis).

## Goals / Non-Goals

**Goals:**
- Process articles concurrently to reduce Stage 1 wall-clock time from O(N) to ~O(1)
- Isolate failures so one article's LLM error doesn't cancel processing of other articles
- Follow existing coroutine patterns in the codebase

**Non-Goals:**
- Adding concurrency limits (semaphore/chunking) — article counts per run are small enough that unbounded parallelism is fine
- Making the function `suspend` — that would ripple changes to `LlmPipeline.run()` and its callers
- Adding concurrency to Stage 2 (composition) — it processes all articles in a single LLM call

## Decisions

### Decision 1: Use `runBlocking(Dispatchers.IO)` as the coroutine bridge

The caller `LlmPipeline.run()` is a regular (non-suspend) function. Rather than propagating `suspend` up the call chain, we use `runBlocking(Dispatchers.IO)` inside `scoreSummarize()`.

**Why**: Matches the existing pattern in `InworldTtsProvider.generate()`. Contained change — no signature changes to callers.

**Alternative considered**: Making `scoreSummarize` a `suspend fun`. Rejected because it would require `LlmPipeline.run()` to become suspend, rippling up to `BriefingGenerationScheduler`.

### Decision 2: `supervisorScope` + `async` with per-task `try/catch`

Each article is launched as an `async` coroutine inside a `supervisorScope`. The `try/catch` lives inside each `async` block (returning `null` on failure), preserving the current error semantics where failed articles are silently excluded.

**Why**: `supervisorScope` ensures one child's failure doesn't cancel siblings. This matches the `SourcePollingScheduler` pattern exactly.

**Alternative considered**: `coroutineScope` (structured concurrency without supervisor). Rejected because a single article failure would cancel all in-flight sibling coroutines.

### Decision 3: Unbounded parallelism (no semaphore)

All articles launch concurrently without a concurrency limit.

**Why**: Typical article counts per pipeline run are small (5-30). OpenRouter handles concurrent requests well. The SQLite writes serialize naturally (single-writer) but are fast (~ms each). Adding a semaphore would add complexity for no practical benefit at current scale.

## Risks / Trade-offs

- **[OpenRouter rate limits]** → Mitigated by small article counts per run. If this becomes an issue in the future, a `Semaphore(N)` can be added without changing the overall structure.
- **[SQLite write contention]** → SQLite serializes writes, but each `articleRepository.save()` is fast. Concurrent coroutines will briefly queue on writes. No practical impact.
- **[Log interleaving]** → Concurrent processing means log lines from different articles may interleave. Each log line already includes the article title, so traceability is maintained.
