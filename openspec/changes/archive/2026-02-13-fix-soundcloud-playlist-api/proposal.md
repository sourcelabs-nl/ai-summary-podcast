## Why

SoundCloud's playlist API requires track IDs as strings in the JSON body, but the `SoundCloudClient` sends them as numbers. This causes a 422 "Could not parse JSON request body" error on every playlist creation or update. Additionally, the `SoundCloudClient` uses a plain `RestTemplate()` without Spring Boot's message converters, which means it lacks proper Jackson 3 configuration.

## What Changes

- Fix track ID serialization in `createPlaylist` and `addTrackToPlaylist` — send IDs as strings instead of numbers.
- Migrate `SoundCloudClient` from `new RestTemplate()` to `RestTemplateBuilder.build()` for proper Spring Boot 4 auto-configured message converters.

## Capabilities

### New Capabilities

_None — this is a bug fix._

### Modified Capabilities

- `soundcloud-integration`: Fix track ID type in playlist API payloads and use Spring-managed RestTemplate.

## Impact

- **Code**: `SoundCloudClient.kt` — track ID serialization in playlist methods, RestTemplate initialization.
- **Dependencies**: Adds `RestTemplateBuilder` injection (from `spring-boot-restclient`).
- **No breaking changes** — the `SoundCloudClient` public API signatures are unchanged.
