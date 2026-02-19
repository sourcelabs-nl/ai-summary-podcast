## MODIFIED Requirements

### Requirement: Episode-article join table
The system SHALL maintain an `episode_articles` join table with columns: `id` (auto-generated INTEGER PRIMARY KEY), `episode_id` (INTEGER, NOT NULL, FK to episodes), `article_id` (INTEGER, NOT NULL, FK to articles). A unique constraint SHALL exist on `(episode_id, article_id)` to prevent duplicate linkage. This table enables traceability from episodes back to the articles that contributed to them.

Episode-article links SHALL be saved for ALL episode generation paths, including both the scheduled generation (`BriefingGenerationScheduler`) and manual generation (`PodcastController.generate()`). Both paths SHALL delegate to shared logic in `EpisodeService` to save episode-article links.

#### Scenario: Articles linked to episode after briefing generation
- **WHEN** a briefing is composed from 5 relevant articles and an episode is created
- **THEN** 5 rows are created in `episode_articles`, each linking one article to the episode

#### Scenario: Articles linked to episode after manual generation
- **WHEN** a manual generation via `POST /users/{userId}/podcasts/{podcastId}/generate` produces an episode from 3 articles
- **THEN** 3 rows are created in `episode_articles`, each linking one article to the episode

#### Scenario: Duplicate linkage prevented
- **WHEN** an `(episode_id, article_id)` combination already exists in `episode_articles`
- **THEN** the duplicate insert is rejected

#### Scenario: Episode with no articles
- **WHEN** an episode is created but the pipeline returned no articles (edge case)
- **THEN** no rows are created in `episode_articles`

## ADDED Requirements

### Requirement: Shared episode creation logic in EpisodeService
The system SHALL provide a method in `EpisodeService` that encapsulates the post-pipeline episode creation logic: saving the episode, saving episode-article links, generating a recap, and updating `lastGeneratedAt` on the podcast. Both `BriefingGenerationScheduler` and `PodcastController.generate()` SHALL delegate to this shared method instead of reimplementing the logic independently.

#### Scenario: Scheduler delegates to shared creation logic
- **WHEN** `BriefingGenerationScheduler` completes a pipeline run with a non-null result
- **THEN** it calls the shared `EpisodeService` method which saves the episode, links articles, generates recap, and updates `lastGeneratedAt`

#### Scenario: Manual generate delegates to shared creation logic
- **WHEN** `PodcastController.generate()` completes a pipeline run with a non-null result
- **THEN** it calls the same shared `EpisodeService` method which saves the episode, links articles, generates recap, and updates `lastGeneratedAt`

#### Scenario: Episode recap generated for manually triggered episodes
- **WHEN** a manual generation produces an episode
- **THEN** a recap is generated and stored on the episode, matching the behavior of scheduled generation
