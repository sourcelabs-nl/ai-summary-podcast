## MODIFIED Requirements

### Requirement: Two-step LLM pipeline
The system SHALL process articles through a multi-stage sequential LLM pipeline: (1) score, summarize, and filter, (1b) overlap detection against recent episodes, (2) briefing script composition. The pipeline SHALL be triggered by the `BriefingGenerationScheduler` on a configurable cron schedule.

Before running the LLM stages, the pipeline SHALL invoke the `SourceAggregator` to create articles from unlinked posts. For each source belonging to the podcast, the aggregator queries unlinked posts within the configured time window and creates articles (aggregated or 1:1 depending on the source's `aggregate` setting). The resulting articles are persisted to the `articles` table with `post_articles` join entries.

Stage 1 (score+summarize+filter) SHALL process each newly created article with a single LLM call that returns structured JSON containing: `relevanceScore` (integer 0-10), `summary` (text, 2-3 sentences of relevant content), `includedPostIds` (array of post IDs deemed relevant), and `excludedPostIds` (array of post IDs deemed irrelevant). The system SHALL persist `relevance_score` and `summary` on the article immediately. Articles with `relevanceScore` below the podcast's `relevanceThreshold` SHALL be excluded from composition. For non-aggregated articles (single post), `includedPostIds` and `excludedPostIds` MAY be omitted from the response.

Stage 1b (overlap detection) SHALL run after scoring and before composition. It SHALL query articles from the N most recent GENERATED episodes (via the `episode_articles` join table) and compare their summaries against the candidate articles' summaries using a single LLM call with the filter model. Candidate articles flagged as overlapping SHALL be excluded from composition but SHALL still be marked `is_processed = true` (to prevent re-processing in future runs). The overlap detection step SHALL be skipped if there are no recent episodes or no candidate articles after scoring.

Stage 2 (composition) SHALL query articles where `relevance_score >= podcast.relevanceThreshold` AND `is_processed = false` AND not flagged as overlapping, and compose a briefing script. After composition, articles SHALL be marked `is_processed = true`.

Both stages SHALL track token usage. Stage 1 SHALL persist token counts on each article. Stage 1b token usage SHALL be included in the `PipelineResult` totals. Stage 2 SHALL return token counts in the `PipelineResult`.

The `PipelineResult` SHALL include a `processedArticleIds` field containing the IDs of all articles that were marked as processed (including overlap-excluded ones).

Stage 1 and 1b SHALL use the model resolved for the `filter` stage. Stage 2 SHALL use the model resolved for the `compose` stage.

#### Scenario: Full pipeline with overlap detection
- **WHEN** the pipeline runs, 10 articles are scored relevant, and 2 overlap with recent episodes
- **THEN** 8 articles proceed to composition, 2 are excluded but marked as processed, and the briefing script covers only the 8 non-overlapping articles

#### Scenario: Overlap detection skipped for new podcast
- **WHEN** the pipeline runs for a podcast with no GENERATED episodes
- **THEN** Stage 1b is skipped entirely and all relevant articles proceed to composition

#### Scenario: All candidates overlap — no briefing generated
- **WHEN** the pipeline runs, 3 articles are scored relevant, and all 3 overlap with recent episodes
- **THEN** no composition call is made, all 3 articles are marked `is_processed = true`, and no briefing is generated

#### Scenario: Overlap-excluded articles are not reprocessed
- **WHEN** article A was excluded due to overlap in a previous pipeline run
- **THEN** article A has `is_processed = true` and is NOT picked up in subsequent pipeline runs

#### Scenario: Pipeline skipped when no unlinked posts exist
- **WHEN** the pipeline runs and there are no unlinked posts within the time window for any source
- **THEN** no articles are created and the pipeline SHALL still check for existing unprocessed relevant articles to compose

#### Scenario: Pipeline resumes after partial completion
- **WHEN** the pipeline previously created articles and scored some but crashed before composition
- **THEN** on the next run, already-scored articles are not re-scored; new unlinked posts are aggregated; the pipeline proceeds through overlap detection and composition

#### Scenario: No relevant articles after scoring
- **WHEN** all scored articles have `relevanceScore` below the podcast's `relevanceThreshold`
- **THEN** no overlap detection or composition call is made and no briefing is generated

#### Scenario: Irrelevant posts filtered during summarization
- **WHEN** an aggregated article contains 10 posts, 3 of which are irrelevant to the topic
- **THEN** the LLM's summary covers only the 7 relevant posts, and `excludedPostIds` lists the 3 irrelevant post IDs

#### Scenario: PipelineResult contains processed article IDs
- **WHEN** the pipeline composes a briefing from 5 relevant articles (after 2 were excluded for overlap)
- **THEN** the `PipelineResult.processedArticleIds` contains the IDs of all 7 articles (5 composed + 2 overlap-excluded)

#### Scenario: Episode-article links recorded after episode creation
- **WHEN** the `BriefingGenerationScheduler` creates an episode from a `PipelineResult`
- **THEN** the scheduler records one `episode_articles` row for each article ID in `PipelineResult.processedArticleIds`
