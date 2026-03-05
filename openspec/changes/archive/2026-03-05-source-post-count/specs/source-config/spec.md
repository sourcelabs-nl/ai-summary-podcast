## MODIFIED Requirements

### Requirement: Article count statistics in source list response
The source list API (`GET /users/{userId}/podcasts/{podcastId}/sources`) SHALL include article count statistics for each source: `articleCount` (total articles from this source), `relevantArticleCount` (articles with relevance score >= podcast's relevance threshold), and `postCount` (total posts from this source).

#### Scenario: Source with articles
- **WHEN** a source has 42 articles, 18 of which have relevance_score >= the podcast's threshold, and 120 posts
- **THEN** the source response SHALL include `articleCount: 42`, `relevantArticleCount: 18`, and `postCount: 120`

#### Scenario: Source with no articles
- **WHEN** a source has no articles in the articles table and no posts in the posts table
- **THEN** the source response SHALL include `articleCount: 0`, `relevantArticleCount: 0`, and `postCount: 0`

#### Scenario: Batch computation
- **WHEN** the source list is fetched for a podcast with multiple sources
- **THEN** article counts and post counts SHALL each be computed in a single batch query, not per-source
