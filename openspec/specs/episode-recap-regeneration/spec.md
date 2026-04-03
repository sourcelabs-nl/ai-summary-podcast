# Capability: Episode Recap Regeneration

## Purpose

Endpoint to regenerate the recap and show-notes for an existing episode, updating the sources file and static feed.

## Requirements

### Requirement: Regenerate recap endpoint
The system SHALL provide a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/regenerate-recap` endpoint that regenerates the recap and show-notes for an existing episode. The endpoint SHALL call the existing `EpisodeRecapGenerator` with the episode's script text, update the episode's `recap` and `show_notes` fields, regenerate the sources file, and re-export the static feed. The endpoint SHALL return the updated episode.

#### Scenario: Successful recap regeneration
- **WHEN** a POST request is made to `/users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/regenerate-recap` for an episode with a non-empty script
- **THEN** the system generates a new recap from the script text, stores it on the episode, updates show-notes, regenerates the sources file, re-exports the static feed, and returns the updated episode with HTTP 200

#### Scenario: Episode not found
- **WHEN** a POST request is made with a non-existent episode ID
- **THEN** the system returns HTTP 404

#### Scenario: Episode belongs to different podcast
- **WHEN** a POST request is made with an episode ID that belongs to a different podcast
- **THEN** the system returns HTTP 404

#### Scenario: Recap generation failure
- **WHEN** the LLM call for recap generation fails
- **THEN** the system returns HTTP 500 with an error message (the episode is not modified)
