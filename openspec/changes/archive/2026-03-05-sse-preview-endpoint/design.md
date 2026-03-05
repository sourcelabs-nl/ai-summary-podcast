## Context

The preview endpoint (`POST /users/{userId}/podcasts/{podcastId}/preview`) runs the full LLM pipeline synchronously: aggregation, scoring, and composition. This can take minutes depending on article count. The Next.js rewrite proxy has a ~30s timeout that kills the connection before the pipeline completes.

Currently:
- `PodcastController.preview()` calls `PodcastService.previewBriefing()` which calls `LlmPipeline.preview()`
- `LlmPipeline.preview()` runs all three steps sequentially, returning a `PreviewResult`
- The frontend uses a simple `fetch()` POST and waits for the JSON response

## Goals / Non-Goals

**Goals:**
- Eliminate proxy timeout issues for the preview endpoint
- Provide real-time progress feedback to the user during preview generation
- Keep the existing synchronous preview behavior working (the SSE connection stays open until done)

**Non-Goals:**
- Converting the generate/approve endpoints to SSE (can be done later if needed)
- Adding cancellation support for in-progress previews
- Changing the LLM pipeline internals beyond adding progress callbacks

## Decisions

### Decision 1: Use Spring SseEmitter for server-side streaming

**Choice**: `SseEmitter` with a long timeout (5 minutes), running the pipeline on a background thread.

**Alternatives considered**:
- **WebFlux/Reactor Flux**: Would require reactive stack; the project uses Spring MVC — too invasive
- **WebSocket**: Bidirectional not needed; SSE is simpler for server-push
- **Polling**: More complex client logic, more HTTP roundtrips, delayed feedback

**Rationale**: `SseEmitter` integrates naturally with Spring MVC, requires minimal changes, and the pipeline is already a single long-running operation.

### Decision 2: Progress events with stage information

**Choice**: Emit structured progress events at stage transitions:
- `progress` event with `{"stage": "aggregating", "detail": "..."}`
- `progress` event with `{"stage": "scoring", "detail": "Scoring N articles..."}`
- `progress` event with `{"stage": "composing", "detail": "..."}`
- `result` event with the final `{scriptText, style, articleIds}`
- `error` event if the pipeline fails
- `complete` event (empty) to signal end of stream

The pipeline will accept a callback/listener that emits these events.

### Decision 3: Use `fetch()` with ReadableStream on the frontend (not EventSource)

**Choice**: Use `fetch()` with response body streaming and manual SSE parsing.

**Alternatives considered**:
- **EventSource API**: Only supports GET requests, and the endpoint currently validates user/podcast ownership via path params (GET would work here since there's no request body). However, `EventSource` doesn't support custom headers and has limited error handling.

**Rationale**: `fetch()` with readable stream gives full control over headers, error handling, and connection lifecycle. The SSE text format is simple to parse manually. The endpoint can remain GET since it takes no request body.

### Decision 4: Change preview endpoint from POST to GET

**Choice**: Change to `GET /users/{userId}/podcasts/{podcastId}/preview` with `text/event-stream` content type.

**Rationale**: The preview endpoint takes no request body — all context comes from path parameters. GET is semantically correct for SSE and compatible with both `EventSource` and `fetch()`.

### Decision 5: Next.js custom API route for SSE passthrough

**Choice**: Create a Next.js API route at `app/api/users/[userId]/podcasts/[podcastId]/preview/route.ts` that proxies the SSE stream from the backend, bypassing the rewrite proxy.

**Rationale**: Next.js rewrites don't reliably support streaming responses. A custom API route with `fetch()` and `TransformStream` gives full control over timeouts and streaming behavior.

## Risks / Trade-offs

- **[Risk] SseEmitter thread management**: The pipeline runs on a separate thread; if the client disconnects, the thread keeps running.
  → Mitigation: Register `onCompletion`/`onTimeout` handlers on SseEmitter. The pipeline work (scoring) is idempotent and persisted, so orphaned runs cause no harm beyond wasted compute.

- **[Risk] Next.js API route adds a proxy layer**: Slightly more complex than a simple rewrite.
  → Mitigation: Only needed for this one SSE endpoint. All other endpoints continue using the rewrite.

- **[Trade-off] GET with side effects**: The preview endpoint triggers LLM calls (scoring) which mutate article records. GET is not purely safe.
  → Accepted: The mutations are idempotent (re-scoring produces the same result). The alternative (POST with EventSource polyfill) adds unnecessary complexity.
