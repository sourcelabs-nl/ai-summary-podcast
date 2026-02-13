## MODIFIED Requirements

### Requirement: SoundCloud playlist creation
The `SoundCloudClient` SHALL provide a `createPlaylist` method that creates a new public playlist on SoundCloud via `POST https://api.soundcloud.com/playlists` with the user's access token. The request SHALL include a JSON body with the playlist `title` and an initial list of track IDs serialized as strings. The method SHALL return the created playlist's ID.

#### Scenario: Track IDs sent as strings
- **WHEN** `createPlaylist` is called with track ID 2266108838
- **THEN** the JSON body contains `"tracks": [{"id": "2266108838"}]` with the ID as a string value

### Requirement: SoundCloud add track to playlist
The `SoundCloudClient` SHALL provide an `addTrackToPlaylist` method that adds a track to an existing SoundCloud playlist via `PUT https://api.soundcloud.com/playlists/{playlistId}` with the user's access token. The request SHALL include the track IDs serialized as strings. The method SHALL return the updated playlist response.

#### Scenario: Track IDs sent as strings
- **WHEN** `addTrackToPlaylist` is called with track ID 6789
- **THEN** the JSON body contains `"tracks": [{"id": "6789"}]` with the ID as a string value

### Requirement: SoundCloudClient uses Spring-managed RestTemplate
The `SoundCloudClient` SHALL use a `RestTemplate` built from Spring Boot's `RestTemplateBuilder` instead of a plain `RestTemplate()` constructor. This ensures proper auto-configured message converters (Jackson 3) are available.
