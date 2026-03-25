## MODIFIED Requirements

### Requirement: Episode-article join table
The system SHALL maintain an `episode_articles` join table with columns: `id` (auto-generated INTEGER PRIMARY KEY), `episode_id` (INTEGER, NOT NULL, FK to episodes), `article_id` (INTEGER, NOT NULL, FK to articles), `topic` (TEXT, nullable). A unique constraint SHALL exist on `(episode_id, article_id)` to prevent duplicate linkage. The `topic` column stores the dedup cluster topic label that groups related articles. This table enables traceability from episodes back to the articles that contributed to them.

Episode-article links SHALL be saved for ALL episode generation paths, including both the scheduled generation (`BriefingGenerationScheduler`) and manual generation (`PodcastController.generate()`). Both paths SHALL delegate to shared logic in `EpisodeService` to save episode-article links.

#### Scenario: Articles linked to episode after briefing generation
- **WHEN** a briefing is composed from 5 relevant articles across 3 dedup clusters and an episode is created
- **THEN** 5 rows are created in `episode_articles`, each linking one article to the episode with the corresponding cluster topic label

#### Scenario: Articles linked to episode after manual generation
- **WHEN** a manual generation via `POST /users/{userId}/podcasts/{podcastId}/generate` produces an episode from 3 articles
- **THEN** 3 rows are created in `episode_articles`, each linking one article to the episode with the corresponding cluster topic label

#### Scenario: Duplicate linkage prevented
- **WHEN** an `(episode_id, article_id)` combination already exists in `episode_articles`
- **THEN** the duplicate insert is rejected

#### Scenario: Episode with no articles
- **WHEN** an episode is created but the pipeline returned no articles (edge case)
- **THEN** no rows are created in `episode_articles`

#### Scenario: Topic is null for recomposed episodes
- **WHEN** an episode is recomposed (bypassing the dedup filter)
- **THEN** rows are created in `episode_articles` with `topic` set to NULL

### Requirement: Pipeline returns processed article IDs
The `LlmPipeline` SHALL return the list of processed article IDs in `PipelineResult` along with a mapping of article ID to topic label (`articleTopics: Map<Long, String>`). The topic mapping SHALL be built from the `DedupResult.clusters` by iterating each cluster's `selectedArticleIds` and mapping them to the cluster's `topic` string.

#### Scenario: PipelineResult includes article IDs and topics
- **WHEN** the LLM pipeline processes 5 articles across 3 dedup clusters into a briefing
- **THEN** the `PipelineResult` contains the IDs of all 5 processed articles and a topic map with 5 entries mapping each article ID to its cluster topic

#### Scenario: PipelineResult with no articles
- **WHEN** the LLM pipeline finds no relevant articles and returns null
- **THEN** no `PipelineResult` is returned (null)

#### Scenario: Recompose pipeline result has empty topic map
- **WHEN** the recompose pipeline produces a `PipelineResult`
- **THEN** the `articleTopics` map is empty

### Requirement: Shared episode creation logic in EpisodeService
The system SHALL provide a method in `EpisodeService` that encapsulates the post-pipeline episode creation logic: saving the episode, saving episode-article links (with topic metadata from `PipelineResult.articleTopics`), generating a recap, and updating `lastGeneratedAt` on the podcast. Both `BriefingGenerationScheduler` and `PodcastController.generate()` SHALL delegate to this shared method instead of reimplementing the logic independently.

The system SHALL NOT advance `lastGeneratedAt` when the pipeline returns no results (null). Only the successful creation of an episode SHALL update this timestamp. This ensures that articles published before the timestamp remain visible in the upcoming view even when a pipeline run finds no relevant content to compose.

#### Scenario: Scheduler delegates to shared creation logic
- **WHEN** `BriefingGenerationScheduler` completes a pipeline run with a non-null result
- **THEN** it calls the shared `EpisodeService` method which saves the episode, links articles with their topic labels, generates recap, and updates `lastGeneratedAt`

#### Scenario: Manual generate delegates to shared creation logic
- **WHEN** `PodcastController.generate()` completes a pipeline run with a non-null result
- **THEN** it calls the same shared `EpisodeService` method which saves the episode, links articles with their topic labels, generates recap, and updates `lastGeneratedAt`

#### Scenario: Episode recap generated for manually triggered episodes
- **WHEN** a manual generation produces an episode
- **THEN** a recap is generated and stored on the episode, matching the behavior of scheduled generation

#### Scenario: Pipeline returns no results
- **WHEN** `llmPipeline.run()` returns null (no relevant articles to compose)
- **THEN** the system SHALL NOT update `lastGeneratedAt` on the podcast

#### Scenario: Pipeline returns results and episode is created
- **WHEN** `llmPipeline.run()` returns a `PipelineResult` and an episode is successfully created
- **THEN** `lastGeneratedAt` SHALL be updated to the current timestamp
