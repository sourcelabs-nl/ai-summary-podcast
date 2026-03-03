## Purpose

Defines the backend API endpoints for fetching upcoming articles and generating a dry-run script preview.

## Requirements

### Requirement: Upcoming articles endpoint
The system SHALL provide a `GET /users/{userId}/podcasts/{podcastId}/upcoming-articles` endpoint that returns all articles that would be included in the next episode: relevant (score >= threshold), unprocessed, belonging to the podcast's sources.

#### Scenario: Articles available
- **WHEN** a GET request is made and there are relevant unprocessed articles for the podcast's sources
- **THEN** the response SHALL return a list of articles with: id, title, url, author, publishedAt, relevanceScore, summary, body, and source (id, type, url, label)

#### Scenario: No articles available
- **WHEN** a GET request is made and there are no relevant unprocessed articles
- **THEN** the response SHALL return an empty list

#### Scenario: Podcast not found
- **WHEN** the podcast does not exist or does not belong to the user
- **THEN** the response SHALL return 404

### Requirement: Preview script endpoint
The system SHALL provide a `POST /users/{userId}/podcasts/{podcastId}/preview` endpoint that performs a dry-run: scores any unscored articles, composes a script from relevant unprocessed articles, but does NOT create an episode and does NOT mark articles as processed.

#### Scenario: Successful preview
- **WHEN** a POST request is made and there are relevant articles
- **THEN** the response SHALL return the composed scriptText, the podcast style, and the list of articles used

#### Scenario: No articles for preview
- **WHEN** a POST request is made but there are no relevant unprocessed articles
- **THEN** the response SHALL return 200 with an appropriate message indicating no content is available

#### Scenario: Scoring side effect
- **WHEN** a preview is requested and there are unscored articles
- **THEN** those articles SHALL be scored and their scores persisted, but articles SHALL NOT be marked as processed
