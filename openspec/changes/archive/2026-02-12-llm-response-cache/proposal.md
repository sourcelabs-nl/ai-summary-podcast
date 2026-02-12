## Why

When multiple podcasts subscribe to the same sources, each podcast independently runs LLM summarization on identical articles. Since article summarization is topic-agnostic (the prompt is a generic "summarize in 2-3 sentences"), these duplicate calls waste LLM API credits. A persistent cache of LLM responses keyed on prompt hash + model eliminates redundant calls while requiring minimal architectural change.

## What Changes

- Add a persistent `llm_cache` table in SQLite to store LLM responses keyed by `(prompt_hash, model)`
- Add a caching wrapper around `ChatClient` calls that checks the cache before making an LLM request
- Cache hits return the stored response directly, cache misses call the LLM and store the result
- All LLM calls (relevance filtering, summarization, briefing composition) go through the cache, but only summarization will see meaningful cache hits across podcasts (relevance and briefing prompts include podcast-specific context)
- Add a configurable TTL for cache entries (default: no expiry, with optional cleanup of entries older than N days)

## Capabilities

### New Capabilities
- `llm-cache`: Persistent caching layer for LLM responses, keyed on prompt content hash and model identifier, stored in SQLite

### Modified Capabilities
_None. The cache is transparent to the existing LLM processing pipeline — no requirement-level behavior changes._

## Impact

- **New table**: `llm_cache` added via Flyway migration
- **LLM pipeline**: `RelevanceFilter`, `ArticleSummarizer`, and `BriefingComposer` benefit transparently — no code changes in those classes
- **New code**: Cache service + repository, ChatClient wrapper/decorator
- **No API changes**: No REST endpoints affected
- **No dependency changes**: Uses existing Spring Data JDBC + SQLite