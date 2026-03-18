## MODIFIED Requirements

### Requirement: Upcoming articles endpoint
The system SHALL provide a `GET /users/{userId}/podcasts/{podcastId}/upcoming-articles` endpoint that returns all articles and unlinked posts collected since the last episode generation, regardless of relevance score.

#### Scenario: Articles and posts available since last episode
- **WHEN** a GET request is made and there are articles or unlinked posts published/created since `podcast.lastGeneratedAt`
- **THEN** the response SHALL return a combined list of articles and posts with: id, title, url, author, publishedAt, relevanceScore (null for unscored/posts), summary (null for posts), body, and source (id, type, url, label), sorted by relevanceScore descending (nulls last)

#### Scenario: No content since last episode
- **WHEN** a GET request is made and there are no articles or posts since `podcast.lastGeneratedAt`
- **THEN** the response SHALL return an empty list

#### Scenario: No previous episode
- **WHEN** a GET request is made and `podcast.lastGeneratedAt` is null
- **THEN** the system SHALL fall back to returning content from the last `maxArticleAgeDays` days (default 7)

#### Scenario: Podcast not found
- **WHEN** the podcast does not exist or does not belong to the user
- **THEN** the response SHALL return 404

#### Scenario: Service layer delegation
- **WHEN** the endpoint is called
- **THEN** the controller SHALL delegate to a service method and SHALL NOT access repositories directly
