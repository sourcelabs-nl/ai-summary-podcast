## 1. Database

- [x] 1.1 Create Flyway migration `V5__add_llm_cache.sql` with the `llm_cache` table (columns: `prompt_hash`, `model`, `response`, `created_at`, composite PK on `prompt_hash` + `model`)
- [x] 1.2 Create `LlmCache` entity in `com.aisummarypodcast.store`
- [x] 1.3 Create `LlmCacheRepository` with `findByPromptHashAndModel` query and `deleteOlderThan` modifying query

## 2. Core Implementation

- [x] 2.1 Create `CachingChatModel` decorator implementing `ChatModel` — checks cache on `call()`, delegates to wrapped model on miss, stores response on miss, reconstructs minimal `ChatResponse` on hit
- [x] 2.2 Modify `ChatClientFactory.createForPodcast()` to wrap `OpenAiChatModel` in `CachingChatModel`

## 3. Configuration and Cleanup

- [x] 3.1 Add `app.llm-cache.max-age-days` property to `AppProperties`
- [x] 3.2 Create `LlmCacheCleanup` scheduled component that deletes entries older than `max-age-days` (only active when property is set)

## 4. Tests

- [x] 4.1 Unit test for `CachingChatModel`: cache miss delegates and stores, cache hit returns without delegating, different model produces miss
- [x] 4.2 Unit test for `LlmCacheCleanup`: cleanup runs when configured, cleanup skipped when not configured
