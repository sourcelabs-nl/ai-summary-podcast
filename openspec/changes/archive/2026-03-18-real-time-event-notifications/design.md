## Context

The application has a single SSE use case today: the preview pipeline streams progress events for a specific operation via `GET /users/{userId}/podcasts/{podcastId}/preview`. All other async operations (TTS generation after approval, publishing, scheduled briefing generation) complete silently — the frontend only discovers state changes by manually refetching.

The frontend already uses a React context pattern (`UserProvider` in `layout.tsx`) and the backend already uses `@Async` for TTS generation and coroutine-based schedulers for polling and briefing generation.

## Goals / Non-Goals

**Goals:**
- Real-time UI state synchronization when backend state changes (episode status, publication status)
- Toast notifications for important background events (audio ready, publish complete, new episode from scheduler)
- Single persistent SSE connection per user session with auto-reconnect
- Minimal changes to existing service code (publish one event line after each state transition)

**Non-Goals:**
- Replacing the existing preview SSE stream (it stays as-is for per-operation progress)
- Multi-user broadcast (events are scoped to the user who owns the data)
- Persistent event storage or event replay (events are fire-and-forget to connected clients)
- WebSocket upgrade (SSE is sufficient for server-to-client push)

## Decisions

### 1. Spring ApplicationEventPublisher for internal event bus

**Decision:** Use Spring's built-in `ApplicationEventPublisher` to decouple state transitions from SSE delivery.

**Why:** It's already in the Spring context, requires zero new dependencies, and keeps services unaware of SSE. Services call `publisher.publishEvent(PodcastEvent(...))` after a `save()`. A separate `@EventListener` component handles SSE fan-out.

**Alternative considered:** Direct injection of an SSE broadcaster into services — rejected because it couples domain services to the notification transport.

### 2. Single per-user SSE endpoint

**Decision:** `GET /users/{userId}/events` returns a persistent `text/event-stream`. One connection per user, all podcast events for that user flow through it.

**Why:** The user is mostly on one podcast page, but may switch. A single connection avoids reconnect overhead when navigating. Frontend filters events by `podcastId` to update the relevant page.

**Alternative considered:** Per-podcast SSE streams — rejected because it requires reconnecting when navigating between podcasts and doesn't support cross-podcast toasts (e.g., "New episode for podcast X").

### 3. Event payload: trigger-only with refetch

**Decision:** SSE events carry a minimal payload (`podcastId`, `entityType`, `entityId`, `event`, plus a small `data` map for toast text). The frontend refetches the full entity from the REST API after receiving an event.

**Why:** Avoids duplicating response DTOs in the event payload, keeps events small, and guarantees the frontend always has consistent data from the single source of truth (REST API). The extra API call is negligible (local network, small payloads).

**Alternative considered:** Full entity in event payload — rejected because it duplicates serialization logic and risks stale/inconsistent data shapes.

### 4. SseEmitter with ConcurrentHashMap registry

**Decision:** `SseEventBroadcaster` maintains a `ConcurrentHashMap<String, MutableList<SseEmitter>>` keyed by userId (list to support multiple tabs). The `@EventListener` method iterates connected emitters and sends events. Dead emitters are cleaned up on send failure.

**Why:** Simple, no external dependencies. The app is single-instance, so in-process fan-out is sufficient.

### 5. Frontend: EventProvider context + Sonner toasts

**Decision:** Add an `EventProvider` React context in `layout.tsx` (next to `UserProvider`) that opens an `EventSource` connection. Expose a `useEventStream(podcastId?, callback)` hook for components to subscribe. Use shadcn/ui's Sonner component for toast notifications.

**Why:** Follows the existing provider pattern. `EventSource` is the browser-native SSE client with built-in reconnect. Sonner is the shadcn/ui-recommended toast library.

### 6. Next.js SSE proxy route

**Decision:** Add a Next.js API route at `app/api/users/[userId]/events/route.ts` that proxies the SSE stream from the backend, similar to the existing preview proxy route.

**Why:** The `next.config.ts` rewrite works for regular API calls but may buffer SSE streams. A dedicated proxy route ensures the stream is forwarded without buffering, matching the existing pattern used for the preview SSE endpoint.

### 7. Event catalog

**Decision:** Define a fixed set of event types. Each event has a `toast` flag indicating whether it should trigger a notification.

| Event | Toast | Toast message pattern |
|---|---|---|
| `episode.created` | Yes | "New episode pending review" |
| `episode.approved` | No | — |
| `episode.audio.started` | No | — |
| `episode.generated` | Yes | "Episode #{n} audio ready" |
| `episode.failed` | Yes | "Episode #{n} audio generation failed" |
| `episode.discarded` | No | — |
| `episode.published` | Yes | "Episode #{n} published to {target}" |
| `episode.publish.failed` | Yes | "Episode #{n} publish failed" |

## Risks / Trade-offs

**[Risk] SSE connection dropped silently** → `EventSource` has built-in reconnect. Add a heartbeat (empty comment every 30s) from the backend to detect dead connections faster and keep proxies from closing idle connections.

**[Risk] Multiple tabs open = multiple emitters per user** → The broadcaster supports a list of emitters per userId. Dead emitters are pruned on send failure. Acceptable overhead for a single-user app.

**[Risk] Event published before DB commit completes** → Use `@TransactionalEventListener(phase = AFTER_COMMIT)` instead of `@EventListener` to ensure events are only broadcast after the transaction commits successfully.

**[Trade-off] No event replay** → If the SSE connection is down when an event fires, it's lost. This is acceptable because the frontend refetches on reconnect (initial page load fetches current state). For a single-user app, the window for missed events is small.
