## Context

The `SourcePollingScheduler` currently iterates over all enabled sources sequentially in a `for` loop within a `@Scheduled(fixedDelay = 60_000)` method. When multiple sources share the same host (e.g. several Nitter RSS feeds), they fire back-to-back within milliseconds, triggering rate limits on free/community-run instances. On startup or after downtime, all sources become due simultaneously, amplifying the burst.

Current flow:
```
@Scheduled pollSources()
  for (source in allEnabledSources):
    if (isDue(source)): poll(source)   ← sequential, no spacing
```

The `Source` entity has per-source overrides for `maxFailures` and `maxBackoffHours`, but no mechanism for request spacing.

## Goals / Non-Goals

**Goals:**
- Prevent rate limit violations on free/community-run services (primarily Nitter) by spacing requests to the same host
- Poll different hosts in parallel so slow/rate-limited hosts don't block others
- Provide sensible defaults with per-source override capability
- Distribute initial poll load on startup via jitter

**Non-Goals:**
- Sophisticated queue-based rate limiting with token buckets or sliding windows
- Async/reactive rewrite of the fetchers themselves — they remain blocking calls wrapped in coroutines
- Rate limiting for paid APIs (Twitter, OpenRouter) — these are handled by their own API tiers

## Decisions

### Decision 1: Kotlin coroutines with `supervisorScope` for parallel host polling

Poll host groups in parallel using `kotlinx.coroutines`. Each host group runs as an `async` child under `supervisorScope`, so one host group failing does not cancel siblings.

**Why coroutines over `ExecutorService`**: Spring 6.1+ natively supports `@Scheduled suspend fun`, making coroutines a first-class citizen. Coroutines are lighter weight, and structured concurrency with `supervisorScope` gives us exactly the failure isolation needed. The scheduler entry point becomes a `suspend fun` — no `runBlocking` bridge needed.

**Alternative considered**: `ExecutorService` with `invokeAll()` — works but requires manual thread pool management and doesn't integrate as cleanly with Spring's scheduling.

### Decision 2: Host grouping by URL host extraction

Group due sources by extracting the host from `source.url` using `java.net.URI(url).host`. Sources sharing the same host (e.g. `nitter.net/user1/rss` and `nitter.net/user2/rss`) end up in the same group and are polled sequentially with delays.

Sources with unparseable URLs fall into a `null`-host group and are polled without delay.

### Decision 3: Three-layer delay resolution

Resolve the delay between polls for a source using this chain:

```
source.pollDelaySeconds       ← explicit per-source override (new DB column)
  ↓ null?
host-overrides[host]          ← config: app.source.host-overrides.<host>
  ↓ no match?
poll-delay-seconds[type]      ← config: app.source.poll-delay-seconds.<type>
  ↓ no match?
0                             ← no delay
```

**Why three layers**: Per-source gives full control for edge cases. Host overrides handle the common case (all nitter.net sources need spacing) without touching each source. Type defaults provide a baseline (e.g. website scraping should always be polite).

**Alternative considered**: Hardcoded auto-detection (like `shouldAggregate()` checks for "nitter" in URL) — less flexible, doesn't handle self-hosted instances with custom domains.

### Decision 4: Startup jitter via random offset

When `lastPolled` is null (first poll ever), assign a random delay of `random(0..pollIntervalMinutes)` minutes. This spreads initial polls across the first interval window instead of all firing on the first tick.

Implementation: store the app start time and compare against it, OR simply set `lastPolled` to `now - random(0..pollIntervalMinutes)` on first encounter so the normal due-check logic handles it naturally.

**Preferred approach**: Set `lastPolled` to a synthetic value on first encounter. This avoids needing to track app start time and works naturally with the existing due-check logic. The source gets updated in the DB, so subsequent restarts don't re-jitter.

### Decision 5: Configuration structure in `AppProperties`

Extend `SourceProperties` with:

```kotlin
data class SourceProperties(
    val maxArticleAgeDays: Int = 7,
    val maxFailures: Int = 15,
    val maxBackoffHours: Int = 24,
    val pollDelaySeconds: Map<String, Int> = emptyMap(),   // keyed by source type
    val hostOverrides: Map<String, HostOverride> = emptyMap()  // keyed by host
)

data class HostOverride(
    val pollDelaySeconds: Int = 0
)
```

YAML example:
```yaml
app:
  source:
    poll-delay-seconds:
      rss: 0
      website: 2
      twitter: 0
    host-overrides:
      nitter.net:
        poll-delay-seconds: 3
```

### Decision 6: `kotlinx-coroutines-core` and `kotlinx-coroutines-reactor` dependencies

Add `kotlinx-coroutines-core` for structured concurrency primitives and `kotlinx-coroutines-reactor` for Spring's coroutine scheduling support. Use `Dispatchers.IO` for the poll operations since the fetchers perform blocking HTTP calls.

## Risks / Trade-offs

**[Risk] Coroutine exception leaking past `supervisorScope`** → Each `async` block wraps its content in try-catch. The existing `PollFailure` handling already catches and classifies exceptions within `SourcePoller.poll()`, so coroutine-level exceptions should be rare (only truly unexpected errors like OOM).

**[Risk] Host extraction fails for malformed URLs** → Fall back to `null` host group, which runs without delay. Acceptable — malformed URLs will fail at fetch time anyway.

**[Trade-off] `kotlinx.coroutines.delay()` in sequential polling blocks the coroutine but not the thread** → This is desirable. The delay is cooperative, freeing the thread for other host groups. However, if a delay is long (e.g. 30s between sources), the total time for that host group increases linearly. With 10 nitter sources at 3s each, that's 30s — well within the 60s scheduler tick.

**[Trade-off] Startup jitter means some sources don't poll immediately on first boot** → Acceptable trade-off for preventing burst. Users who want immediate polling can manually trigger a poll via the API.
