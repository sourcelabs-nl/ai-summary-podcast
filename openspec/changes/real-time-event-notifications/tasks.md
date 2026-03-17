## 1. Backend Event Infrastructure

- [x] 1.1 Create `PodcastEvent` data class extending `ApplicationEvent` with fields: `podcastId`, `entityType`, `entityId`, `event`, `data`
- [x] 1.2 Create `SseEventBroadcaster` component with `ConcurrentHashMap<String, MutableList<SseEmitter>>` registry, `register`/`remove` methods, and `@TransactionalEventListener(phase = AFTER_COMMIT)` handler that fans out events to connected emitters
- [x] 1.3 Add heartbeat scheduling (`:heartbeat` comment every 30s) to keep SSE connections alive
- [x] 1.4 Create `EventStreamController` with `GET /users/{userId}/events` SSE endpoint that creates emitter, registers with broadcaster, sets up cleanup callbacks

## 2. Backend Event Publishing

- [x] 2.1 Inject `ApplicationEventPublisher` into `EpisodeService` and publish `episode.created` after new episode creation (both `PENDING_REVIEW` and direct `GENERATED`)
- [x] 2.2 Publish `episode.approved` after approval status change in `EpisodeService.approveAndGenerateAudio()`
- [x] 2.3 Publish `episode.audio.started` at the start of `generateAudioAsync()`, `episode.generated` on success, and `episode.failed` on failure
- [x] 2.4 Publish `episode.discarded` after discard status change in `EpisodeService.discardAndResetArticles()`
- [x] 2.5 Inject `ApplicationEventPublisher` into `PublishingService` and publish `episode.published` on success and `episode.publish.failed` on failure
- [x] 2.6 Publish `episode.created` from `BriefingGenerationScheduler` / `PodcastService` when scheduled generation creates a new episode

## 3. Backend Tests

- [x] 3.1 Unit test `SseEventBroadcaster`: event delivery to registered emitters, dead emitter cleanup, no-op when no emitters
- [x] 3.2 Unit test `EventStreamController`: endpoint returns SSE stream, emitter registered/deregistered
- [x] 3.3 Verify existing `EpisodeService` and `PodcastService` tests still pass with event publisher injected (mock the publisher)

## 4. Frontend Event Infrastructure

- [x] 4.1 Add Sonner toast component: install shadcn/ui sonner, mount `<Toaster />` in `layout.tsx`
- [x] 4.2 Create Next.js SSE proxy route at `app/api/users/[userId]/events/route.ts` (non-buffering stream passthrough)
- [x] 4.3 Create `EventProvider` React context with `EventSource` connection management (open on user select, close on change/unmount)
- [x] 4.4 Create `useEventStream(podcastId?, callback)` hook for component-level event subscription with cleanup on unmount

## 5. Frontend Event Handling

- [x] 5.1 Add toast notification logic in `EventProvider`: show toasts for `episode.created`, `episode.generated`, `episode.failed`, `episode.published`, `episode.publish.failed`
- [x] 5.2 Wire `useEventStream` into episode list page to refetch episodes on any episode/publication event for the current podcast
- [x] 5.3 Wire `useEventStream` into episode detail page to refetch episode data on events matching the current episode ID

## 6. Integration Testing

- [x] 6.1 End-to-end manual test: approve episode → verify toast appears when audio generation completes and status updates in-place
- [x] 6.2 End-to-end manual test: wait for scheduled briefing generation → verify toast appears and new episode row appears in list
- [x] 6.3 End-to-end manual test: publish episode → verify toast appears and published badge updates
