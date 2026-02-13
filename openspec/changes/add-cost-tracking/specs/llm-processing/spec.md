## MODIFIED Requirements

### Requirement: Two-step LLM pipeline
The system SHALL process unscored articles through a three-stage sequential LLM pipeline: (1) relevance scoring, (2) conditional summarization, (3) briefing script composition. The pipeline SHALL be triggered by the `BriefingGenerationScheduler` on a configurable cron schedule.

Stage 1 (scoring) SHALL query articles where `relevance_score IS NULL` for the podcast's sources, score each article 0-10, and persist the score immediately. After scoring, the pipeline SHALL extract token usage (input tokens and output tokens) from the LLM response and persist the counts on the article. Stage 2 (summarization) SHALL query articles where `relevance_score >= podcast.relevanceThreshold` AND `summary IS NULL`, skip articles whose body word count is below the global `app.llm.summarization-min-words` threshold, and summarize the remaining articles. After summarization, the pipeline SHALL extract token usage and accumulate the counts on the article (adding to any existing counts from scoring). Stage 3 (composition) SHALL query articles where `relevance_score >= podcast.relevanceThreshold` AND `is_processed = false`, and compose a briefing script. The composition stage SHALL return token usage alongside the script.

Both the scoring and summarization stages SHALL use the model resolved for the `filter` stage. The composition stage SHALL use the model resolved for the `compose` stage.

The `PipelineResult` SHALL include `llmInputTokens` and `llmOutputTokens` from the composition stage, in addition to the existing `script`, `filterModel`, and `composeModel` fields.

#### Scenario: Full pipeline produces a briefing script with token counts
- **WHEN** the pipeline runs and there are unscored articles, some of which are relevant
- **THEN** articles are scored (0-10) with token counts persisted per article, relevant articles are conditionally summarized with token counts accumulated, and a briefing script is produced with composition token counts in the PipelineResult

#### Scenario: Pipeline skipped when no unscored articles exist
- **WHEN** the pipeline runs and there are no articles with `relevance_score IS NULL`
- **THEN** no scoring LLM calls are made but the pipeline SHALL still check for unsummarized and unprocessed relevant articles to compose

#### Scenario: Pipeline resumes after partial completion
- **WHEN** the pipeline previously scored articles but crashed before summarization
- **THEN** on the next run, scored articles are not re-scored; the pipeline proceeds to summarization and composition

#### Scenario: No relevant articles after scoring
- **WHEN** all scored articles have `relevance_score` below the podcast's `relevanceThreshold`
- **THEN** no summarization or composition LLM calls are made and no briefing is generated

#### Scenario: Token usage extracted from scoring response
- **WHEN** the LLM scores an article and the ChatResponse includes usage metadata with 500 prompt tokens and 50 completion tokens
- **THEN** the article's `llmInputTokens` is set to 500 and `llmOutputTokens` to 50

#### Scenario: Token usage accumulated from summarization
- **WHEN** an article already has `llmInputTokens` = 500 and `llmOutputTokens` = 50, and summarization uses 800 input tokens and 100 output tokens
- **THEN** the article's `llmInputTokens` is updated to 1300 and `llmOutputTokens` to 150

#### Scenario: Cache hit reports zero tokens
- **WHEN** an LLM call is served from cache
- **THEN** the token counts for that call are 0 (no API cost was incurred)

#### Scenario: Usage metadata unavailable
- **WHEN** the LLM response does not include usage metadata
- **THEN** the token counts for that call are 0 and a warning is logged
