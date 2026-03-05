## Why

The preview endpoint (`POST /preview`) runs the full LLM pipeline (aggregation, scoring, composition) synchronously, which can take minutes for podcasts with many articles. The Next.js proxy has a ~30-second timeout that kills the connection before the pipeline completes, leaving the user with an error even though scoring work was already persisted server-side. SSE provides a long-lived connection that avoids proxy timeouts and enables real-time progress feedback.

## What Changes

- Convert the backend preview endpoint from synchronous HTTP response to Server-Sent Events (SSE) using Spring's `SseEmitter`
- Emit progress events as the pipeline moves through stages (scoring, composing)
- Emit the final result (script text + article IDs) as an SSE event
- Update the frontend to consume the SSE stream via `EventSource` or `fetch` with readable stream
- Replace the Next.js rewrite proxy for the preview endpoint with a custom API route that supports streaming

## Capabilities

### New Capabilities
- `sse-preview`: SSE-based preview endpoint with progress events and streamed result delivery

### Modified Capabilities
- `frontend-upcoming-episode`: Script preview now uses SSE stream instead of a synchronous POST, showing real-time progress stages

## Impact

- **Backend**: `PodcastController.preview()` changes return type; `LlmPipeline.preview()` needs a callback or emitter mechanism for progress reporting
- **Frontend**: `upcoming/page.tsx` preview fetch logic replaced with SSE consumption; loading state becomes stage-aware
- **Next.js config**: May need a custom API route for the preview path to support SSE streaming through the proxy
