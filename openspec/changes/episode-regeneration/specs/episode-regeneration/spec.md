# Capability: Episode Regeneration

## Purpose

Re-compose an existing episode's script using its original linked articles and the podcast's current settings, creating a new episode without affecting the regular generation pipeline.

## Requirements

### Requirement: Regenerate episode script
The system SHALL provide an endpoint to regenerate an episode's script by re-running only the composition stage of the LLM pipeline with the episode's original articles and the podcast's current configuration.

#### Scenario: Successful regeneration
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/regenerate` request is received for an existing episode with linked articles
- **THEN** the system creates a new episode with a freshly composed script, links it to the same articles, and returns HTTP 200 with `{"message": "Episode regenerated", "episodeId": <newId>}`

#### Scenario: Episode has no linked articles
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/regenerate` request is received for an episode with no entries in `episode_articles`
- **THEN** the system returns HTTP 500 with an error indicating no articles were found

#### Scenario: Non-existing episode or podcast
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/regenerate` request is received for an episode or podcast that does not exist, or belongs to a different user/podcast
- **THEN** the system returns HTTP 404

### Requirement: Preserve source episode
The regenerated episode SHALL be created as a new episode. The source episode SHALL remain unmodified.

### Requirement: Inherit source episode timestamp
The regenerated episode's `generatedAt` field SHALL be set to the same value as the source episode's `generatedAt`, not the current time.

### Requirement: No pipeline side effects
Regeneration SHALL NOT update the podcast's `lastGeneratedAt` timestamp. This ensures that the regular generation pipeline continues to find articles based on the original generation window.

### Requirement: Composition-only pipeline
Regeneration SHALL only run the composition stage of the LLM pipeline (script generation). It SHALL NOT re-run article aggregation, scoring, or summarization. The articles' existing scores and summaries are used as-is.
