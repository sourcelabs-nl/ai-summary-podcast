## Context

The `SoundCloudClient` creates playlists and adds tracks to playlists via `POST /playlists` and `PUT /playlists/{id}`. The JSON body includes a `tracks` array where each element has an `id` field. The SoundCloud API expects this ID as a string (`"id": "123"`) but the code sends it as a number (`"id": 123`), causing a 422 error.

The `SoundCloudClient` also instantiates `RestTemplate()` directly instead of using Spring Boot's `RestTemplateBuilder`, missing auto-configured message converters (Jackson 3 in Spring Boot 4).

## Goals / Non-Goals

**Goals:**
- Fix playlist creation and track-to-playlist operations.
- Use Spring Boot's managed RestTemplate with proper message converters.

**Non-Goals:**
- Changing the SoundCloudClient public API.
- Modifying other SoundCloud API calls (token exchange, track upload work correctly).

## Decisions

### Send track IDs as strings in playlist API payloads

**Decision:** Convert `Long` track IDs to `String` via `it.toString()` in the map entries sent to the playlist endpoints.

**Rationale:** SoundCloud's API spec defines `tracks[].id` as type `string`. Sending a number causes a 422.

### Use RestTemplateBuilder instead of plain RestTemplate

**Decision:** Inject `RestTemplateBuilder` (from `org.springframework.boot.restclient`) and call `.build()` to create the `RestTemplate` instance.

**Rationale:** Spring Boot 4 auto-configures `RestTemplateBuilder` with Jackson 3 message converters and other defaults. A plain `RestTemplate()` may lack these.
