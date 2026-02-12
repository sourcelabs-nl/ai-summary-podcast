## Context

The LLM pipeline (`RelevanceFilter`, `ArticleSummarizer`, `BriefingComposer`) makes ChatClient calls via `ChatClientFactory.createForPodcast()`, which constructs an `OpenAiChatModel` wrapped in a `ChatClient`. When multiple podcasts subscribe to overlapping sources, identical articles get summarized multiple times with the same prompt and model — wasting LLM API credits.

Current call chain:

```
RelevanceFilter  ─┐
ArticleSummarizer ├──▶ ChatClientFactory ──▶ ChatClient ──▶ OpenAiChatModel ──▶ LLM API
BriefingComposer ─┘
```

## Goals / Non-Goals

**Goals:**
- Eliminate redundant LLM API calls for identical prompts sent to the same model
- Persist the cache in SQLite so it survives application restarts
- Make caching transparent — existing pipeline code should not need to change

**Non-Goals:**
- Caching TTS API calls (different pipeline, different concern)
- Cache invalidation strategies beyond simple TTL (e.g., no cache-busting, no manual purge API)
- Shared source architecture (Option A from exploration — deferred)

## Decisions

### Decision 1: Cache at the ChatModel level using a decorator

Wrap `OpenAiChatModel` in a `CachingChatModel` that implements `ChatModel`. The decorator checks the cache before delegating to the real model.

```
ChatClient ──▶ CachingChatModel ──▶ OpenAiChatModel ──▶ LLM API
                    │
                    ▼
               LlmCacheRepository (SQLite)
```

The only change is in `ChatClientFactory` — wrap the model before passing it to `ChatClient.builder()`.

**Alternatives considered:**
- *Spring AI Advisor*: ChatClient advisors can intercept calls, but short-circuiting the response from an advisor requires reconstructing ChatResponse internals. More coupling to Spring AI internals.
- *Explicit LlmCacheService*: A service that callers use instead of ChatClient directly. Simpler but requires modifying all three pipeline components (RelevanceFilter, ArticleSummarizer, BriefingComposer). Violates the "transparent" goal.

**Why decorator:** Single point of change (ChatClientFactory), transparent to all callers, follows the standard decorator pattern. The `ChatModel` interface is stable (`call(Prompt): ChatResponse`).

### Decision 2: Cache key = SHA-256 of (model + full prompt text)

The cache key is `sha256(model + ":" + promptText)` where:
- `model` is the resolved model identifier (e.g., `anthropic/claude-3-haiku`)
- `promptText` is the full user message text including any structured output instructions injected by Spring AI

This means:
- Same article + same model = cache hit (summarization across podcasts)
- Same article + different topic = cache miss (relevance filtering, different prompts)
- Same articles + different podcast style = cache miss (briefing composition, different prompts)

### Decision 3: Store only the text content, not the full ChatResponse

`ChatResponse` contains metadata (token usage, finish reason, etc.) that is not worth caching. Store only the assistant message text content. Reconstruct a minimal `ChatResponse` on cache hits.

This keeps the cache table simple and avoids serialization complexity.

### Decision 4: No TTL by default, optional cleanup

Article summaries are deterministic given the same input — they don't go stale. Default behavior: cache entries live forever. Add a configurable `app.llm-cache.max-age-days` property (default: disabled) that, when set, enables a scheduled cleanup of entries older than N days.

### Decision 5: Cache table schema

```sql
CREATE TABLE llm_cache (
    prompt_hash TEXT NOT NULL,
    model       TEXT NOT NULL,
    response    TEXT NOT NULL,
    created_at  TEXT NOT NULL,
    PRIMARY KEY (prompt_hash, model)
);
```

Composite primary key on `(prompt_hash, model)`. No auto-increment ID needed — the hash is the natural key.

## Risks / Trade-offs

- **Stale cache for identical prompts with different expected outputs** — If the same prompt is sent expecting different results (e.g., temperature > 0 for creative variation), the cache returns the first result. Mitigation: current pipeline uses temperature 0.3, which is near-deterministic. Briefing composition (the creative step) has highly unique prompts per podcast, so cache misses naturally.

- **Cache size growth** — Each entry is a prompt hash (64 chars) + model string + response text (typically < 1KB). Even 100k entries would be ~100MB in SQLite. Mitigation: optional max-age cleanup. SQLite handles this scale easily.

- **ChatResponse reconstruction** — On cache hit, we construct a minimal `ChatResponse` with only the text content. If callers rely on metadata (token counts, finish reason), they get empty/default values. Mitigation: current callers only use `.content()` and `.entity()`, both of which only need the text.