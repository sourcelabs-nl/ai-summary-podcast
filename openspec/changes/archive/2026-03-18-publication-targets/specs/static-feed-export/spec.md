## MODIFIED Requirements

### Requirement: Static feed uses configurable base URL
The system SHALL support an optional `app.feed.static-base-url` configuration property. When set, the static `feed.xml` SHALL use this URL as the base for all enclosure URLs instead of `app.feed.base-url`. Additionally, when the podcast has an FTP publication target with a `publicUrl` config value, the static feed SHALL use that `publicUrl` as the base URL, taking precedence over `app.feed.static-base-url`. The priority order SHALL be: FTP target `publicUrl` > `app.feed.static-base-url` > `app.feed.base-url`.

#### Scenario: Static feed with FTP publicUrl
- **WHEN** a podcast has an FTP publication target with `publicUrl = "https://podcast.example.com/shows/tech/"`
- **THEN** enclosure URLs in the static `feed.xml` use `https://podcast.example.com/shows/tech/` as the base, and the image URL and sources.md links also use this base

#### Scenario: Static feed with custom base URL but no FTP target
- **WHEN** `app.feed.static-base-url` is set to `https://cdn.example.com` and the podcast has no FTP target
- **THEN** enclosure URLs in the static `feed.xml` use `https://cdn.example.com/episodes/{podcastId}/{filename}` as the base

#### Scenario: Static feed without custom base URL or FTP target
- **WHEN** `app.feed.static-base-url` is not configured and the podcast has no FTP target
- **THEN** enclosure URLs in the static `feed.xml` use the value of `app.feed.base-url`
