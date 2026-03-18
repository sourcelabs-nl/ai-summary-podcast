## Why

After a user triggers an async backend action (e.g. approving an episode for TTS generation, publishing), the frontend has no way to know when the task completes. The user must manually refresh the page to see updated status. Background tasks like scheduled briefing generation also create episodes silently — the user only discovers them on the next page visit. A persistent event stream will keep the UI in sync with backend state and surface important events via toast notifications.

## What Changes

- Add a persistent per-user SSE stream (`GET /users/{userId}/events`) that broadcasts domain events (episode status changes, publication updates, scheduled generation results).
- Introduce a Spring `ApplicationEventPublisher`-based event bus: services publish `PodcastEvent`s after state transitions, and an SSE broadcaster fans them out to connected clients.
- Add a React `EventProvider` context in the frontend layout that maintains an `EventSource` connection with auto-reconnect.
- Frontend pages subscribe to relevant events and refetch data when state changes occur.
- Toast notifications for important background events (audio generation complete/failed, new episode created by scheduler, publish complete/failed).
- The existing preview SSE stream remains unchanged — it serves a different purpose (per-operation progress).

## Capabilities

### New Capabilities
- `sse-event-stream`: Persistent per-user SSE endpoint, event bus, broadcaster, and event catalog for domain events (episode lifecycle, publication status).
- `frontend-event-notifications`: React EventProvider context, event-driven refetch, and toast notifications for real-time UI updates.

### Modified Capabilities
- `episode-review`: Services must publish events after episode status transitions (created, approved, generated, failed, discarded).
- `episode-publishing`: Publishing service must publish events after publication status transitions (published, failed).

## Impact

- **Backend**: New controller (`EventStreamController`), new service (`SseEventBroadcaster`), new domain event class (`PodcastEvent`). Minor additions to `EpisodeService`, `PodcastService`, `PublishingService`, and `BriefingGenerationScheduler` to publish events after state changes.
- **Frontend**: New `EventProvider` context in `layout.tsx`, toast UI component (shadcn/ui Sonner), event subscription hooks in episode list/detail pages.
- **APIs**: New `GET /users/{userId}/events` SSE endpoint. No changes to existing REST endpoints.
- **Dependencies**: shadcn/ui `sonner` toast component (frontend).