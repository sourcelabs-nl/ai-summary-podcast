## Context

The `InworldTtsProvider` generates TTS audio by splitting scripts into 2000-char chunks and calling the Inworld API sequentially per chunk. A typical episode produces 4-8 chunks, meaning generation time scales linearly. Inworld's rate limit of 100 req/s provides ample headroom for parallelization.

Currently, all three TTS providers (OpenAI, ElevenLabs, Inworld) process chunks sequentially. This change targets Inworld only — other providers can follow the same pattern later if needed.

## Goals / Non-Goals

**Goals:**
- Reduce Inworld TTS generation time by processing chunks concurrently
- Handle rate limiting (HTTP 429) gracefully with retry and backoff
- Maintain correct chunk ordering in the final audio output

**Non-Goals:**
- Parallelizing OpenAI or ElevenLabs providers (separate change if desired)
- Adding shared parallelism infrastructure to `TtsProvider` interface or `TtsPipeline`
- Implementing preemptive rate limiting or request counting
- Changing chunk size (remains 2000)

## Decisions

### 1. Parallelism at provider level, not pipeline level

**Decision:** Implement coroutine-based parallelism inside `InworldTtsProvider`, not in `TtsPipeline` or a shared utility.

**Why:** Each provider has different API shapes, rate limits, and chunking strategies (e.g., ElevenLabs dialogue batches multiple speakers in one call). A shared abstraction would need to accommodate all these differences. Keeping it provider-internal is simpler and avoids over-abstraction.

**Alternative considered:** Lifting parallelism into `TtsPipeline` — rejected because providers aren't uniform enough.

### 2. No explicit concurrency cap

**Decision:** Use `Dispatchers.IO` without a semaphore or thread pool cap.

**Why:** Typical episodes produce 4-8 chunks. Even 8 concurrent requests use 8% of the 100 rps limit. `Dispatchers.IO` has a default 64-thread ceiling which is more than sufficient. Adding a semaphore solves a problem that doesn't exist.

**Alternative considered:** `Semaphore(N)` — rejected as unnecessary complexity for current workload.

### 3. Flatten dialogue chunks for full parallelism

**Decision:** For dialogue scripts, flatten all turn chunks into a single list and fire all in parallel, rather than processing turns sequentially with parallel chunks within each turn.

**Why:** Most dialogue turns are under 2000 chars (single chunk each). Parallelizing only within turns provides little benefit. Full flattening maximizes concurrency and uses the same pattern as monologue — simpler code.

**Alternative considered:** Per-turn parallelism — rejected because most turns don't chunk, so the parallelism opportunity is at the turn level.

### 4. Reactive retry on HTTP 429

**Decision:** Retry individual chunk requests on 429 with exponential backoff (3 attempts, 1s → 2s → 4s).

**Why:** At 4-8 concurrent requests against a 100 rps limit, 429s are extremely unlikely but should be handled gracefully. Reactive retry is simple and self-throttling — repeated 429s naturally serialize requests via backoff delays.

**Alternative considered:** Preemptive rate limiting with sliding window counter — rejected as over-engineering for a scenario that rarely occurs.

### 5. Retry logic in InworldTtsProvider, not InworldApiClient

**Decision:** The retry loop wraps the `apiClient.synthesizeSpeech()` call inside the provider, not inside the API client itself.

**Why:** The API client should remain a thin HTTP wrapper. Retry policy is a concern of the caller (the provider), who understands the context (parallel generation, acceptable delays). The API client needs to signal 429 rather than throw, so it should throw a specific retryable exception (e.g., `InworldRateLimitException`) that the provider catches for retry.

## Risks / Trade-offs

- **[Risk] Inworld API instability under concurrent load** → Mitigated by the 100 rps rate limit being far above our usage. If issues arise, adding a semaphore is trivial.
- **[Risk] Partial failure — some chunks succeed, some fail** → After retry exhaustion, fail the entire generation. Partial audio is worse than no audio. Already-completed chunks are just byte arrays in memory, no cleanup needed.
- **[Trade-off] `suspend fun generate()` changes the call signature** → The `TtsProvider` interface `generate()` would need to become a `suspend` function, or the provider wraps the coroutine scope internally with `runBlocking`. Using `runBlocking` inside the provider avoids changing the interface and affecting other providers.
