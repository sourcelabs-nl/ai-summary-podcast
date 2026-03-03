## MODIFIED Requirements

### Requirement: Source configuration via YAML
The system SHALL manage content source definitions in the database via a REST API. Each source entity SHALL have the fields: `id` (unique string), `type` (one of `rss`, `website`, `twitter`, `youtube`), `url`, `pollIntervalMinutes` (default: 30), `enabled` (default: true), `aggregate` (nullable boolean, default: null — enabling hybrid auto-detect/override for article aggregation), `pollDelaySeconds` (nullable integer, default: null — per-source override for delay between polls to the same host), `categoryFilter` (nullable string, default: null — comma-separated list of category terms for RSS pre-ingestion filtering), and `label` (nullable string, default: null — human-readable label for display).

#### Scenario: Source created via API
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/sources` request is received with a valid source definition
- **THEN** the source is persisted in the database with the configured values

#### Scenario: Source with default values
- **WHEN** a source entry omits `pollIntervalMinutes`, `enabled`, `aggregate`, `pollDelaySeconds`, `categoryFilter`, and `label`
- **THEN** the source defaults to a 30-minute poll interval, enabled = true, aggregate = null (auto-detect), pollDelaySeconds = null (use global/host defaults), categoryFilter = null (no category filtering), and label = null

#### Scenario: Disabled source is loaded but not active
- **WHEN** a source has `enabled: false`
- **THEN** the source exists in the database but is excluded from polling

#### Scenario: Source with explicit aggregate override
- **WHEN** a source has `aggregate: true`
- **THEN** articles from this source are always aggregated regardless of type or URL

#### Scenario: Source with explicit poll delay override
- **WHEN** a source has `pollDelaySeconds: 5`
- **THEN** a 5-second delay is applied after polling this source, overriding any host or type default

#### Scenario: RSS source with category filter
- **WHEN** a source has `type: "rss"` and `categoryFilter: "kotlin,AI"`
- **THEN** the source is configured with category filter terms `["kotlin", "AI"]` for pre-ingestion filtering

### Requirement: Topic configuration
The system SHALL store a `topic` string on the `Podcast` entity that defines the user's area of interest. This topic is used as context for LLM relevance filtering.

#### Scenario: Topic stored on podcast
- **WHEN** a podcast is created or updated with `topic: "AI engineering, LLM applications"`
- **THEN** the topic string is available for injection into LLM prompts
