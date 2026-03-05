## 1. Backend: SSE Preview Endpoint

- [x] 1.1 Add progress callback to `LlmPipeline.preview()` — accept a `(stage: String, detail: Map<String, Any>) -> Unit` callback parameter, invoke it at each pipeline stage (aggregating, scoring, composing) with relevant metadata (article counts)
- [x] 1.2 Change `PodcastController.preview()` from `POST` returning `ResponseEntity` to `GET` returning `SseEmitter` with `text/event-stream` content type and 5-minute timeout
- [x] 1.3 Run the pipeline on a background coroutine, emitting progress events via the SseEmitter, then the result event, then complete
- [x] 1.4 Handle error cases: emit `error` event on pipeline failure, return 404 for invalid user/podcast before creating the SseEmitter
- [x] 1.5 Register `onCompletion`/`onTimeout`/`onError` handlers on the SseEmitter for cleanup logging

## 2. Frontend: Next.js SSE Proxy Route

- [x] 2.1 Create a Next.js API route at `frontend/src/app/api/users/[userId]/podcasts/[podcastId]/preview/route.ts` that proxies the GET request to the backend and streams the SSE response without buffering

## 3. Frontend: SSE Preview Consumer

- [x] 3.1 Replace the `fetch()` POST in `upcoming/page.tsx` with a `fetch()` GET to the SSE endpoint, reading the response body as a readable stream
- [x] 3.2 Parse incoming SSE events (progress, result, error, complete) from the stream
- [x] 3.3 Update loading state to show the current pipeline stage (e.g., "Scoring 5 articles...", "Composing script...")
- [x] 3.4 On `result` event, populate the preview state with scriptText/style/articleIds (or show message if no relevant articles)
- [x] 3.5 On `error` event or connection failure, clear loading state and display error message

## 4. Testing

- [x] 4.1 No PodcastControllerTest exists — no test to update
- [x] 4.2 Verified existing tests pass (EpisodeControllerTest, EpisodeServiceTest) — no changes needed
