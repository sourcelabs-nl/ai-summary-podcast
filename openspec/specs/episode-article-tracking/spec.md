# Capability: Episode-Article Tracking

## Purpose

Tracks which articles contributed to each episode, providing traceability from episodes back to their source articles.

## Requirements

### Requirement: Episode-article join table
The system SHALL maintain an `episode_articles` join table with columns: `id` (auto-generated INTEGER PRIMARY KEY), `episode_id` (INTEGER, NOT NULL, FK to episodes), `article_id` (INTEGER, NOT NULL, FK to articles). A unique constraint SHALL exist on `(episode_id, article_id)` to prevent duplicate linkage. This table enables traceability from episodes back to the articles that contributed to them.

#### Scenario: Articles linked to episode after briefing generation
- **WHEN** a briefing is composed from 5 relevant articles and an episode is created
- **THEN** 5 rows are created in `episode_articles`, each linking one article to the episode

#### Scenario: Duplicate linkage prevented
- **WHEN** an `(episode_id, article_id)` combination already exists in `episode_articles`
- **THEN** the duplicate insert is rejected

#### Scenario: Episode with no articles
- **WHEN** an episode is created but the pipeline returned no articles (edge case)
- **THEN** no rows are created in `episode_articles`

### Requirement: Episode-article repository
The system SHALL provide a Spring Data JDBC repository for the `EpisodeArticle` entity with methods to save links and query articles by episode.

#### Scenario: Save episode-article links in bulk
- **WHEN** an episode is created from 3 articles
- **THEN** the repository saves 3 `EpisodeArticle` records

#### Scenario: Find articles for an episode
- **WHEN** querying articles for episode ID 42
- **THEN** the repository returns all article IDs linked to episode 42

### Requirement: Pipeline returns processed article IDs
The `LlmPipeline` SHALL return the list of processed article IDs in `PipelineResult` so that callers can record episode-article links after the episode is persisted.

#### Scenario: PipelineResult includes article IDs
- **WHEN** the LLM pipeline processes 5 articles into a briefing
- **THEN** the `PipelineResult` contains the IDs of all 5 processed articles

#### Scenario: PipelineResult with no articles
- **WHEN** the LLM pipeline finds no relevant articles and returns null
- **THEN** no `PipelineResult` is returned (null)
