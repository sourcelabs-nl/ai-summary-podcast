## ADDED Requirements

### Requirement: Article count statistics in source list response
The source list API (`GET /users/{userId}/podcasts/{podcastId}/sources`) SHALL include article count statistics for each source: `articleCount` (total articles from this source) and `relevantArticleCount` (articles with relevance score >= podcast's relevance threshold).

#### Scenario: Source with articles
- **WHEN** a source has 42 articles, 18 of which have relevance_score >= the podcast's threshold
- **THEN** the source response SHALL include `articleCount: 42` and `relevantArticleCount: 18`

#### Scenario: Source with no articles
- **WHEN** a source has no articles in the articles table
- **THEN** the source response SHALL include `articleCount: 0` and `relevantArticleCount: 0`

#### Scenario: Batch computation
- **WHEN** the source list is fetched for a podcast with multiple sources
- **THEN** article counts SHALL be computed in a single batch query, not per-source
