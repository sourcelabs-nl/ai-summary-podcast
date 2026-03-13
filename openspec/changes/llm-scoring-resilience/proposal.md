## Why

The `ArticleScoreSummarizer` fires all LLM scoring requests concurrently with no concurrency limit. When 50+ articles need scoring, all requests hit OpenRouter simultaneously, which can trigger rate limiting or overload responses (observed: OpenRouter returning `application/octet-stream` instead of JSON, causing all 57 articles to fail in a single batch). Failed articles are silently dropped from the result, meaning an entire episode can end up with no content.

## What Changes

- **Windowed concurrency**: Limit the number of parallel LLM requests using a configurable concurrency window (e.g., 10 concurrent requests at a time) instead of unbounded parallelism.
- **Retry with backoff**: Add retry logic (up to 3 attempts with exponential backoff) for transient LLM failures before giving up on an article. Only retries on exceptions that indicate transient issues (network errors, non-JSON responses, 429/5xx status codes).
- **Improved failure logging**: Log the retry attempts so operators can distinguish transient vs. permanent failures.
- **Fix `lastGeneratedAt` advancement on empty pipeline**: `PodcastService.generateBriefing()` was advancing `lastGeneratedAt` even when the pipeline returned no results (no relevant articles). This caused all articles published before the timestamp to disappear from the upcoming view, orphaning scored content.

## Capabilities

### New Capabilities

_None_

### Modified Capabilities

- `llm-processing`: The "Score, summarize, and filter stage" requirement changes to add concurrency limiting and retry behavior for individual article LLM calls.
- `episode-article-tracking`: The "Shared episode creation logic" requirement clarifies that `lastGeneratedAt` SHALL only advance when an episode is actually created, not when the pipeline returns no results.

## Impact

- `ArticleScoreSummarizer.kt` — main implementation change (concurrency window + retry loop)
- `ArticleScoreSummarizerTest.kt` — new test scenarios for windowed concurrency and retry behavior
- `application.yaml` — new configuration properties for concurrency limit and retry count
- `PodcastService.kt` — remove erroneous `lastGeneratedAt` update on empty pipeline result
- No API changes, no database changes, no breaking changes