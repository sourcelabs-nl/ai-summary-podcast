## ADDED Requirements

### Requirement: Episode articles endpoint
The system SHALL provide a `GET /api/users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/articles` endpoint that returns all articles linked to the specified episode, including source metadata for each article.

#### Scenario: Fetch articles for episode
- **WHEN** a GET request is made to `/api/users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/articles`
- **THEN** the system returns a JSON array of article objects with fields: `id`, `title`, `url`, `author`, `publishedAt`, `relevanceScore`, `summary`, and a nested `source` object containing `id`, `type`, `url`, and `label`

#### Scenario: Episode with no articles
- **WHEN** the endpoint is called for an episode with no linked articles
- **THEN** the system returns an empty JSON array

#### Scenario: Episode not found
- **WHEN** the endpoint is called with a non-existent episode ID
- **THEN** the system returns HTTP 404

### Requirement: Recap field in episode response
The episode response from `GET /api/users/{userId}/podcasts/{podcastId}/episodes/{episodeId}` SHALL include the `recap` field.

#### Scenario: Episode with recap
- **WHEN** a single episode is fetched and it has a recap
- **THEN** the response includes the `recap` field with the recap text

#### Scenario: Episode without recap
- **WHEN** a single episode is fetched and it has no recap
- **THEN** the response includes the `recap` field as null
