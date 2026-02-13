## ADDED Requirements

### Requirement: Relevance threshold per podcast
Each podcast SHALL have a `relevance_threshold` field (INTEGER, NOT NULL, default 5). The LLM pipeline SHALL use this threshold to determine which scored articles are relevant: articles with `relevance_score >= relevance_threshold` are considered relevant. Valid values are 0-10. The field SHALL be accepted in podcast create (`POST`) and update (`PUT`) endpoints and included in GET responses.

#### Scenario: Podcast with custom relevance threshold
- **WHEN** a podcast has `relevance_threshold` set to 7
- **THEN** only articles with `relevance_score` >= 7 are considered relevant for summarization and briefing composition

#### Scenario: Podcast with default relevance threshold
- **WHEN** a podcast is created without specifying `relevance_threshold`
- **THEN** the `relevance_threshold` defaults to 5

#### Scenario: Strict podcast filters aggressively
- **WHEN** a podcast has `relevance_threshold` set to 8 and 10 articles are scored with scores [2, 3, 5, 6, 7, 7, 8, 8, 9, 10]
- **THEN** only the 4 articles with scores 8, 8, 9, 10 are considered relevant

#### Scenario: Broad podcast includes more articles
- **WHEN** a podcast has `relevance_threshold` set to 3 and 10 articles are scored with scores [2, 3, 5, 6, 7, 7, 8, 8, 9, 10]
- **THEN** 9 articles with scores >= 3 are considered relevant

#### Scenario: Relevance threshold included in API response
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}` request is received
- **THEN** the response includes `relevanceThreshold` with its current value

#### Scenario: Relevance threshold updated via API
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}` request includes `relevanceThreshold: 8`
- **THEN** the podcast's `relevance_threshold` is updated to 8
