## Context

`ArticleScoreSummarizer.scoreSummarize()` uses `supervisorScope` + `async` to fire all LLM scoring requests concurrently. With 50+ articles, this sends 50+ simultaneous HTTP requests to OpenRouter, which can trigger rate limiting or cause the API to return non-JSON error pages (`application/octet-stream`). When this happens, Spring AI's `RestClient` throws a `RestClientException` and the article is silently dropped from results.

The current implementation has no concurrency limit and no retry logic. A single transient API hiccup can wipe out an entire episode's worth of content.

## Goals / Non-Goals

**Goals:**
- Limit concurrent LLM requests to a configurable window size to avoid overwhelming OpenRouter
- Retry transient failures with exponential backoff before giving up on an article
- Make both settings configurable via `application.yaml`

**Non-Goals:**
- Circuit breaker pattern (too complex for this use case)
- Per-model or per-provider rate limiting (single provider for now)
- Retry logic for the composition stage (single call, different failure mode)

## Decisions

### 1. Kotlin `Semaphore` for concurrency limiting

Use `kotlinx.coroutines.sync.Semaphore` to limit the number of concurrent LLM requests. The existing `supervisorScope` + `async` pattern stays — each article still gets its own `async` block, but acquires a semaphore permit before making the HTTP call.

**Why not chunking (e.g., `chunked(10).forEach`):** Chunking waits for an entire batch to finish before starting the next, wasting time when some articles are fast and others slow. A semaphore provides a sliding window where a new request starts as soon as any previous one completes.

### 2. Retry loop inside the `async` block

Each article's `async` block wraps the LLM call in a retry loop (configurable, default 3 attempts). On failure, wait with exponential backoff (1s, 2s, 4s) before retrying. On the final attempt, the exception propagates to the existing `catch` block which logs and returns `null`.

**Why not Spring Retry:** Adding `spring-retry` for a single call site is overkill. A simple `repeat` loop with `delay()` is idiomatic Kotlin coroutines and keeps the retry within the coroutine context (respects cancellation).

### 3. Configuration via `AppProperties`

Add `scoring` section under `app.llm` with two properties:
- `concurrency` (default: `10`) — max parallel LLM requests
- `max-retries` (default: `3`) — max attempts per article (1 = no retry)

```yaml
app:
  llm:
    scoring:
      concurrency: 10
      max-retries: 3
```

### 4. Remove `lastGeneratedAt` advancement on empty pipeline

`PodcastService.generateBriefing()` updated `lastGeneratedAt` even when `llmPipeline.run()` returned `null` (no relevant articles to compose). This was intended to prevent re-scoring already-scored articles, but it has a destructive side effect: the upcoming view uses `lastGeneratedAt` as the cutoff for `findAllSince`, so advancing it hides all articles published before that timestamp.

**Fix:** Remove the `podcastRepository.save(podcast.copy(lastGeneratedAt = ...))` call from the null-result branch. `lastGeneratedAt` should only advance when an actual episode is created (already handled in `EpisodeService.createEpisodeFromPipelineResult()`). Re-scoring is not a concern because the scorer queries `relevance_score IS NULL`, which naturally skips already-scored articles.

## Risks / Trade-offs

- **Slower total scoring time**: With a concurrency limit of 10, scoring 50 articles takes ~5x longer than unbounded parallelism. This is acceptable — reliability is more important than speed, and the pipeline runs on a schedule (not user-facing latency).
- **Retry delays**: 3 retries with exponential backoff adds up to ~7s per article in the worst case. Since retries happen within the semaphore, they don't block other articles from starting.
- **Backoff not jittered**: Simple exponential backoff without jitter. For a single-user app hitting one API, thundering herd isn't a concern.
