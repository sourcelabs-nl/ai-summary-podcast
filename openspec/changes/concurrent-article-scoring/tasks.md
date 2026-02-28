## 1. Core Implementation

- [x] 1.1 Refactor `ArticleScoreSummarizer.scoreSummarize()` to use `runBlocking(Dispatchers.IO)` + `supervisorScope` + `async` for concurrent article processing. Each article's LLM call + save runs in its own `async` block with a `try/catch` returning `null` on failure. Collect results with `.awaitAll().filterNotNull()`.

## 2. Tests

- [x] 2.1 Update existing `ArticleScoreSummarizerTest` tests to work with the coroutine-based implementation (wrap with `runBlocking` if needed, verify mocks still behave correctly)
- [x] 2.2 Add test: one article failure does not cancel others — submit 3 articles where the 2nd throws, verify 1st and 3rd are returned and saved
- [x] 2.3 Add test: all articles fail gracefully — submit 3 articles that all throw, verify empty list returned and no saves
