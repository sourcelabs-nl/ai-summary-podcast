## 1. Database migration

- [ ] 1.1 Write `V<next>__add_episode_history_fts.sql` creating the FTS5 virtual table, sync triggers on `episodes` and `episode_articles`, and one-shot backfill of existing `GENERATED` episodes
- [ ] 1.2 Verify the migration applies cleanly against a copy of `./data/ai-summary-podcast.db` and FTS row count matches `GENERATED` episode count

## 2. Repository

- [ ] 2.1 Implement `EpisodeHistoryRepository.search(podcastId, query, limit=5)` using `MATCH` against `episode_history_fts` ranked by `bm25`, returning `episodeId`, `generatedAt`, `topics`, `recapSnippet` (capped at ~280 chars)
- [ ] 2.2 Integration-test the repository against an in-process SQLite DB with seeded `GENERATED` episodes (case-insensitive matches, podcast scoping, snippet truncation)

## 3. Tool-calling groundwork

- [ ] 3.1 Implement `ToolBudget` request-scoped wrapper that caps invocations and short-circuits with `budgetExhausted=true` on overflow (no exceptions)
- [ ] 3.2 Unit-test `ToolBudget` cap behavior under concurrent compose calls (each compose gets its own counter)
- [ ] 3.3 Extend `ChatClientFactory` with a new `createForCompose(userId, resolvedModel, podcast, toolBudget)` entry point that registers tools; keep `createForModel(...)` tool-less for filter/score
- [ ] 3.4 Wire `LlmPipeline.compose()` to use the new compose entry point with a freshly built `ToolBudget`

## 4. History tool

- [ ] 4.1 Implement `HistoryLookupTool` as a Spring AI tool callback wrapping `EpisodeHistoryRepository.search`; bind it via `ToolBudget` with limit 5
- [ ] 4.2 Unit-test `HistoryLookupTool`: returns matches scoped to podcast, returns empty for other podcasts, never returns full script text, respects budget cap

## 5. Prompt nudges

- [ ] 5.1 Add a prompt block to all three composers instructing the LLM to call `searchPastEpisodes` before treating any topic as new and how to react to prior coverage
- [ ] 5.2 Composer unit tests assert the prompt contains the `searchPastEpisodes` instruction

## 6. Caching under tool loops

- [ ] 6.1 Audit `CachingChatModel` cache-key derivation when Spring AI tool-call loops are active
- [ ] 6.2 Add an integration test that runs an identical compose prompt twice with `searchPastEpisodes` registered and asserts: second run is a cache hit and records zero tool invocations
- [ ] 6.3 If the audit reveals the cache key drifts under tool loops, adjust the caching to hash only the user prompt + model and cache the final assistant `ChatResponse`

## 7. Verification

- [ ] 7.1 Run `mvn test` and ensure all tests pass
- [ ] 7.2 Restart the app via `./stop.sh && ./start.sh`
- [ ] 7.3 Seed a fake "speckit" episode (or rely on existing speckit coverage), run a fresh pipeline whose articles touch the topic; confirm logs show `searchPastEpisodes("speckit")` invocations and the resulting script avoids re-introducing the topic as new
- [ ] 7.4 Run `openspec validate add-episode-history-lookback --strict`
- [ ] 7.5 Update `README.md` to document the history-lookback capability
