# Capability: Static Feed Export

## Purpose

Export a static `feed.xml` file to each podcast's episode directory, enabling hosting on static file servers without running the application.

## Requirements

### Requirement: Static feed file export
The system SHALL write a `feed.xml` file to the podcast's episode directory (`data/episodes/{podcastId}/feed.xml`) whenever the feed content changes. The file SHALL contain valid RSS 2.0 XML identical to what the dynamic feed endpoint produces. The export SHALL be triggered after each feed-changing event: episode generation, episode approval with TTS completion, and episode cleanup.

#### Scenario: Feed file written after episode generation
- **WHEN** a new episode is generated for a podcast
- **THEN** a `feed.xml` file is written to `data/episodes/{podcastId}/feed.xml` containing all GENERATED episodes for that podcast

#### Scenario: Feed file written after episode approval and TTS
- **WHEN** an episode is approved and TTS generation completes
- **THEN** the `feed.xml` file in the podcast's episode directory is updated to include the newly generated episode

#### Scenario: Feed file written after episode cleanup
- **WHEN** the episode cleanup job removes expired episodes from a podcast
- **THEN** the `feed.xml` file in the podcast's episode directory is updated to exclude the removed episodes

#### Scenario: Feed file for podcast with no remaining episodes
- **WHEN** all episodes for a podcast have been cleaned up
- **THEN** the `feed.xml` file contains valid RSS 2.0 XML with channel metadata but no `<item>` entries

### Requirement: Static feed uses configurable base URL
The system SHALL support an optional `app.feed.static-base-url` configuration property. When set, the static `feed.xml` SHALL use this URL as the base for all enclosure URLs instead of `app.feed.base-url`. When not set, the static feed SHALL use the same `app.feed.base-url` as the dynamic endpoint.

#### Scenario: Static feed with custom base URL
- **WHEN** `app.feed.static-base-url` is set to `https://cdn.example.com`
- **THEN** enclosure URLs in the static `feed.xml` use `https://cdn.example.com/episodes/{podcastId}/{filename}` as the base

#### Scenario: Static feed without custom base URL
- **WHEN** `app.feed.static-base-url` is not configured
- **THEN** enclosure URLs in the static `feed.xml` use the value of `app.feed.base-url`

### Requirement: Export failure does not break the pipeline
The system SHALL NOT fail the episode generation or cleanup pipeline if writing the static `feed.xml` fails. A warning SHALL be logged on export failure.

#### Scenario: Disk write failure during export
- **WHEN** writing the static `feed.xml` fails (e.g., disk full)
- **THEN** a warning is logged and the pipeline continues normally; the dynamic feed endpoint remains unaffected
