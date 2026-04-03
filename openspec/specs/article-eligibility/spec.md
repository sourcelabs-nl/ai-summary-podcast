# Capability: Article Eligibility

## Purpose

Centralized article eligibility decisions for pipeline article selection and episode discard reset logic.

## Requirements

### Requirement: Centralized article eligibility service
The system SHALL provide an `ArticleEligibilityService` that is the single source of truth for article selection and lifecycle management. All pipeline code paths (`run()`, `recompose()`, `preview()`) and episode management operations (`discardAndResetArticles()`) SHALL delegate article eligibility decisions to this service.

#### Scenario: Pipeline run delegates article selection
- **WHEN** `LlmPipeline.run()` needs to find candidate articles for composition
- **THEN** it calls `ArticleEligibilityService` to get eligible articles rather than querying the repository directly

#### Scenario: Pipeline recompose delegates article selection
- **WHEN** `LlmPipeline.recompose()` needs to validate and prepare articles for composition
- **THEN** it delegates to `ArticleEligibilityService` for any eligibility checks

#### Scenario: Discard delegates reset guard
- **WHEN** `EpisodeService.discardAndResetArticles()` needs to determine which articles can be reset
- **THEN** it calls `ArticleEligibilityService.canResetArticle()` for each article

### Requirement: Article age gate
The system SHALL exclude articles whose `published_at` (or `created_at` for posts without a publication date) is before the latest GENERATED episode with at least one PUBLISHED publication's `generated_at` timestamp. This prevents old content from newly added sources from entering the pipeline.

#### Scenario: New source with old content excluded
- **WHEN** a new RSS source is added and its articles have `published_at` dates from last week, and the latest published episode was generated yesterday
- **THEN** none of the old articles are included as candidates for the next episode

#### Scenario: New source with fresh content included
- **WHEN** a new RSS source is added and it has articles published today, after the latest published episode
- **THEN** those articles are included as candidates

#### Scenario: No published episodes -- all articles eligible
- **WHEN** the podcast has no GENERATED episodes with publications (e.g., brand new podcast)
- **THEN** the article age gate does not filter any articles and the existing `maxArticleAgeDays` cutoff applies

#### Scenario: Age gate uses published episode timestamp not lastGeneratedAt
- **WHEN** the podcast's `lastGeneratedAt` was updated by a discarded episode but the latest published episode is older
- **THEN** the age gate uses the published episode's `generated_at`, not the podcast's `lastGeneratedAt`

### Requirement: Discard reset guard
When an episode is discarded, the system SHALL only reset `isProcessed = false` for articles that are NOT linked to any GENERATED episode that has at least one publication with status `PUBLISHED`. Articles linked to published episodes SHALL retain their `isProcessed = true` flag.

#### Scenario: Article only linked to discarded episode -- reset allowed
- **WHEN** an episode is discarded and one of its articles is only linked to this episode (no other episodes)
- **THEN** the article's `isProcessed` is reset to `false`

#### Scenario: Article linked to both discarded and published episode -- reset blocked
- **WHEN** an episode is discarded and one of its articles is also linked to a GENERATED episode that has a PUBLISHED publication
- **THEN** the article's `isProcessed` remains `true`

#### Scenario: Article linked to GENERATED episode without publications -- reset allowed
- **WHEN** an episode is discarded and one of its articles is also linked to a GENERATED episode that has no publications
- **THEN** the article's `isProcessed` is reset to `false` (unpublished GENERATED episodes are not "final")

#### Scenario: Aggregated article linked to published episode -- deletion blocked
- **WHEN** an episode is discarded and one of its aggregated articles (2+ linked posts) is also linked to a GENERATED episode with publications
- **THEN** the aggregated article and its `post_articles` entries are NOT deleted
