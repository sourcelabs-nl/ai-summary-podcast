## 1. Configuration

- [x] 1.1 Add `ScoringProperties` data class with `concurrency: Int = 10` and `maxRetries: Int = 3` to `AppProperties.kt`, nested under `LlmProperties` as `scoring`
- [x] 1.2 Add `scoring` defaults to `application.yaml` under `app.llm`

## 2. Core Implementation

- [x] 2.1 Add `Semaphore(concurrency)` to `ArticleScoreSummarizer.scoreSummarize()` — each `async` block acquires a permit before the LLM call and releases it after
- [x] 2.2 Add retry loop inside the `async` block: wrap the LLM call in a `repeat(maxRetries)` loop with exponential backoff (`delay(1000 * 2^attempt)`). Log each retry at WARN level with attempt number, article title, and error message. On final failure, fall through to existing `catch` block

## 3. Fix `lastGeneratedAt` advancement

- [x] 3.1 Remove `podcastRepository.save(podcast.copy(lastGeneratedAt = ...))` from the null-result branch in `PodcastService.generateBriefing()` — `lastGeneratedAt` should only advance when an episode is actually created

## 4. Tests

- [x] 4.1 Add test: concurrency is limited (verify that with concurrency=2 and 4 articles, at most 2 LLM calls run simultaneously)
- [x] 4.2 Add test: retry succeeds on 2nd attempt (mock LLM to fail once then succeed, verify article is in result)
- [x] 4.3 Add test: all retries exhausted returns null (mock LLM to fail 3 times, verify article excluded from result)
- [x] 4.4 Update existing tests to pass `AppProperties` or scoring config to `ArticleScoreSummarizer`
