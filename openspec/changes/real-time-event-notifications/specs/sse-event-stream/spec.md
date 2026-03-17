## ADDED Requirements

### Requirement: Domain event model
The system SHALL define a `PodcastEvent` data class with fields: `podcastId` (String), `entityType` (String — "episode" or "publication"), `entityId` (Long), `event` (String — event name from the event catalog), and `data` (Map<String, Any> — additional context such as episode number, error message, target name). `PodcastEvent` SHALL extend Spring's `ApplicationEvent`.

#### Scenario: Episode generated event
- **WHEN** an episode completes TTS generation successfully
- **THEN** a `PodcastEvent` is created with `entityType = "episode"`, `event = "episode.generated"`, and `data` containing the episode number

#### Scenario: Publication failed event
- **WHEN** a publish operation fails
- **THEN** a `PodcastEvent` is created with `entityType = "publication"`, `event = "episode.publish.failed"`, and `data` containing the target name and error message

### Requirement: Event publishing via ApplicationEventPublisher
Services SHALL publish `PodcastEvent` instances via Spring's `ApplicationEventPublisher` after each state transition. Events SHALL only be published after the database transaction commits successfully (using `@TransactionalEventListener(phase = AFTER_COMMIT)`).

#### Scenario: Event published after episode status change
- **WHEN** `EpisodeService` saves an episode with a new status
- **THEN** `applicationEventPublisher.publishEvent(PodcastEvent(...))` is called, and the event listener receives it only after the transaction commits

#### Scenario: Event not published on transaction rollback
- **WHEN** a service method throws an exception and the transaction rolls back
- **THEN** no `PodcastEvent` is delivered to listeners

### Requirement: SSE event broadcaster
The system SHALL provide an `SseEventBroadcaster` Spring component that maintains a `ConcurrentHashMap<String, MutableList<SseEmitter>>` keyed by userId. It SHALL expose `register(userId, emitter)` and `remove(userId, emitter)` methods. It SHALL listen for `PodcastEvent` instances using `@TransactionalEventListener(phase = AFTER_COMMIT)` and send the event as JSON to all emitters registered for the event's owning user. Dead emitters (send failure) SHALL be removed automatically.

#### Scenario: Event broadcast to connected user
- **WHEN** a `PodcastEvent` for podcast owned by user "u1" is published, and user "u1" has one connected SSE emitter
- **THEN** the broadcaster sends the event as a JSON SSE message to that emitter

#### Scenario: Event broadcast with multiple tabs
- **WHEN** user "u1" has 3 connected SSE emitters (3 browser tabs) and a `PodcastEvent` is published
- **THEN** all 3 emitters receive the event

#### Scenario: Dead emitter cleanup
- **WHEN** the broadcaster attempts to send an event to an emitter that has been closed (client disconnected)
- **THEN** the emitter is removed from the registry and no error is propagated

#### Scenario: No connected emitters
- **WHEN** a `PodcastEvent` is published but no emitters are registered for that user
- **THEN** the event is silently discarded

### Requirement: User-to-podcast ownership lookup
The `SseEventBroadcaster` SHALL resolve which userId owns a podcast by looking up the podcast's `userId` field. This is needed to route events to the correct user's emitters.

#### Scenario: Resolve user for podcast event
- **WHEN** a `PodcastEvent` with `podcastId = "abc"` is received by the broadcaster
- **THEN** the broadcaster looks up podcast "abc" to find its owning userId and sends the event to that user's emitters

### Requirement: Persistent SSE endpoint
The system SHALL provide a `GET /users/{userId}/events` endpoint that returns `MediaType.TEXT_EVENT_STREAM_VALUE`. The endpoint SHALL create an `SseEmitter` with no timeout (or a long timeout, e.g., 24 hours), register it with `SseEventBroadcaster`, and set up completion/timeout/error callbacks to deregister the emitter.

#### Scenario: Client connects to event stream
- **WHEN** a `GET /users/u1/events` request is received
- **THEN** the server returns an SSE stream, registers the emitter with the broadcaster, and keeps the connection open

#### Scenario: Client disconnects
- **WHEN** the SSE client closes the connection
- **THEN** the emitter's completion callback fires and the emitter is removed from the broadcaster registry

### Requirement: SSE heartbeat
The SSE endpoint SHALL send a heartbeat comment (`:heartbeat\n\n`) every 30 seconds to keep the connection alive through proxies and detect dead connections.

#### Scenario: Heartbeat sent on idle connection
- **WHEN** no events have been sent for 30 seconds on a connected emitter
- **THEN** a comment-only SSE message (`:heartbeat`) is sent to keep the connection alive

### Requirement: SSE event format
Each SSE event SHALL be sent with `event:` set to the event name (e.g., `episode.generated`) and `data:` set to a JSON object containing `podcastId`, `entityType`, `entityId`, and `data` fields from the `PodcastEvent`.

#### Scenario: SSE message format
- **WHEN** an `episode.generated` event for episode 42 of podcast "abc" is sent
- **THEN** the SSE message is formatted as `event: episode.generated\ndata: {"podcastId":"abc","entityType":"episode","entityId":42,"data":{...}}\n\n`

### Requirement: Next.js SSE proxy route
The frontend SHALL provide a Next.js API route at `app/api/users/[userId]/events/route.ts` that proxies the SSE stream from the backend without buffering, matching the existing pattern used for the preview SSE proxy.

#### Scenario: SSE stream passthrough
- **WHEN** the frontend makes a GET request to `/api/users/{userId}/events`
- **THEN** the Next.js API route forwards the request to the backend at `http://localhost:8085/users/{userId}/events`, streams the response back with `Content-Type: text/event-stream`, and does not buffer
