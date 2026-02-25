## 1. Retry Infrastructure

- [x] 1.1 Create `InworldRateLimitException` class for retryable 429 errors
- [x] 1.2 Update `InworldApiClient.synthesizeSpeech()` to throw `InworldRateLimitException` on HTTP 429 (instead of generic error)

## 2. Parallel Generation

- [x] 2.1 Add `kotlinx-coroutines-core` dependency to `pom.xml` (if not already present) — already present
- [x] 2.2 Refactor `InworldTtsProvider.generateMonologue()` to use `coroutineScope { async/awaitAll }` with retry-on-429 (3 attempts, exponential backoff 1s/2s/4s)
- [x] 2.3 Refactor `InworldTtsProvider.generateDialogue()` to flatten all turn chunks and generate in parallel with the same retry logic

## 3. Testing

- [x] 3.1 Add unit tests for parallel monologue generation (verify ordering, verify concurrency)
- [x] 3.2 Add unit tests for parallel dialogue generation (verify flattened ordering across turns)
- [x] 3.3 Add unit tests for 429 retry logic (successful retry, exhausted retries)
