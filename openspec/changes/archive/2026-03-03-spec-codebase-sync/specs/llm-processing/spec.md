## MODIFIED Requirements

### Requirement: Two-step LLM pipeline
The system SHALL process articles through a two-stage sequential LLM pipeline: (1) score, summarize, and filter, (2) briefing script composition. The pipeline SHALL be triggered by the `BriefingGenerationScheduler` on a configurable cron schedule.

Before running the LLM stages, the pipeline SHALL invoke the `SourceAggregator` to create articles from unlinked posts. For each source belonging to the podcast, the aggregator queries unlinked posts within the configured time window and creates articles (aggregated or 1:1 depending on the source's `aggregate` setting). The resulting articles are persisted to the `articles` table with `post_articles` join entries.

Stage 1 (score+summarize+filter) SHALL process each newly created article with a single LLM call that returns structured JSON containing: `relevanceScore` (integer 0-10) and `summary` (text — a concise summary focusing only on content relevant to the podcast's topic). The system SHALL persist `relevance_score` and `summary` on the article immediately. Articles with `relevanceScore` below the podcast's `relevanceThreshold` SHALL be excluded from composition.

Between Stage 1 and Stage 2, the pipeline SHALL fetch the most recent episode for the podcast (any status). If a previous episode exists and has a non-null `recap` field, the pipeline SHALL pass the recap to the composer as continuity context.

Stage 2 (composition) SHALL query articles where `relevance_score >= podcast.relevanceThreshold` AND `is_processed = false`, and compose a briefing script. The composer SHALL receive the previous episode recap (if available) to enable continuity references. After composition, articles SHALL be marked `is_processed = true`.

Both stages SHALL track token usage. Stage 1 SHALL persist token counts on each article. Stage 2 SHALL return token counts in the `PipelineResult`.

The `PipelineResult` SHALL include a `processedArticleIds` field containing the IDs of all articles that were marked as processed. This enables the caller (`BriefingGenerationScheduler`) to record episode-article links after the episode is persisted.

Stage 1 SHALL use the model resolved for the `filter` stage. Stage 2 SHALL use the model resolved for the `compose` stage.

#### Scenario: Full pipeline produces a briefing script
- **WHEN** the pipeline runs and there are unlinked posts for the podcast's sources
- **THEN** posts are aggregated into articles, each article is scored and summarized in one LLM call, and relevant articles are composed into a briefing script

#### Scenario: Pipeline skipped when no unlinked posts exist
- **WHEN** the pipeline runs and there are no unlinked posts within the time window for any source
- **THEN** no articles are created and the pipeline SHALL still check for existing unprocessed relevant articles to compose

#### Scenario: Pipeline resumes after partial completion
- **WHEN** the pipeline previously created articles and scored some but crashed before composition
- **THEN** on the next run, already-scored articles are not re-scored; new unlinked posts are aggregated; the pipeline proceeds to composition

#### Scenario: No relevant articles after scoring
- **WHEN** all scored articles have `relevanceScore` below the podcast's `relevanceThreshold`
- **THEN** no composition LLM call is made and no briefing is generated

#### Scenario: Summary focuses on relevant content
- **WHEN** an aggregated article contains posts about AI and posts about celebrity gossip, scored against topic "artificial intelligence"
- **THEN** the LLM's summary covers only the AI-relevant content, omitting irrelevant posts

#### Scenario: PipelineResult contains processed article IDs
- **WHEN** the pipeline composes a briefing from 5 relevant articles
- **THEN** the `PipelineResult.processedArticleIds` contains the IDs of all 5 articles

#### Scenario: Episode-article links recorded after episode creation
- **WHEN** the `BriefingGenerationScheduler` creates an episode from a `PipelineResult`
- **THEN** the scheduler records one `episode_articles` row for each article ID in `PipelineResult.processedArticleIds`

#### Scenario: Previous episode recap passed to composer
- **WHEN** the pipeline runs and the podcast has a most recent episode with a non-null recap
- **THEN** the pipeline passes the recap to the composer as continuity context

#### Scenario: No recap when previous episode has null recap
- **WHEN** the pipeline runs and the most recent episode has a null recap (old episode)
- **THEN** the composer receives null for the recap parameter

#### Scenario: No recap when no previous episode exists
- **WHEN** the pipeline runs for a podcast with no episodes
- **THEN** the composer receives null for the recap parameter

### Requirement: Score, summarize, and filter stage
The system SHALL process each article through a single LLM call that performs scoring and summarization simultaneously. Articles SHALL be processed concurrently using coroutines with `supervisorScope` for fault isolation — a failure in one article's LLM call SHALL NOT cancel or affect processing of other articles. Failed articles SHALL be excluded from the result list and logged as errors.

The LLM prompt SHALL include the podcast's topic, the full article content, and instructions to: (1) assign a relevance score of 0-10, (2) summarize the relevant content only. The prompt SHALL request a JSON response with the structure: `{ "relevanceScore": <int>, "summary": "<text>" }`. The prompt SHALL instruct the LLM to preserve attribution in the summary and focus only on content relevant to the podcast's topic. The system SHALL persist the `relevanceScore` and `summary` on the article immediately after the call. Token usage SHALL be extracted and persisted on the article.

The prompt SHALL scale the requested summary length based on the word count of the article body:
- Articles with fewer than 500 words: 2-3 sentences.
- Articles with 500-1500 words: 4-6 sentences.
- Articles with more than 1500 words: a full paragraph covering key points, context, and attribution.

The prompt SHALL use content-type-aware framing:
- For aggregated articles (title starts with "Posts from"): The prompt SHALL state that the content consists of multiple social media posts and SHALL name the author (from `article.author`) when available.
- For non-aggregated articles: The prompt SHALL use neutral framing ("Content title" / "Content") instead of "Article title" / "Article text".

The prompt SHALL include the author name (from `article.author`) as context when available, regardless of content type.

The prompt SHALL instruct the LLM to write summaries as direct factual statements about what happened, not meta-descriptions of the content. The prompt SHALL include an explicit negative example: say "Anthropic launched X" not "The article discusses Anthropic launching X".

#### Scenario: Articles processed concurrently
- **WHEN** 5 articles are submitted for scoring and summarization
- **THEN** all 5 LLM calls SHALL be dispatched concurrently rather than sequentially

#### Scenario: One article failure does not cancel others
- **WHEN** 3 articles are submitted and the 2nd article's LLM call throws an exception
- **THEN** the 1st and 3rd articles SHALL still be processed and returned successfully, and the 2nd article SHALL be excluded from the result

#### Scenario: All articles fail gracefully
- **WHEN** 3 articles are submitted and all LLM calls throw exceptions
- **THEN** an empty list SHALL be returned and all errors SHALL be logged

#### Scenario: Short article summarized in 2-3 sentences
- **WHEN** an article with a body of 300 words is scored and summarized
- **THEN** the summary contains 2-3 sentences

#### Scenario: Medium article summarized in 4-6 sentences
- **WHEN** an article with a body of 1000 words is scored and summarized
- **THEN** the summary contains 4-6 sentences with additional context beyond the current 2-3 sentence default

#### Scenario: Long article summarized in a full paragraph
- **WHEN** an article with a body of 2500 words is scored and summarized
- **THEN** the summary is a full paragraph covering key points, context, and attribution

#### Scenario: Fully relevant article
- **WHEN** all content in an article is relevant to the topic
- **THEN** the response has a high `relevanceScore` and a comprehensive summary

#### Scenario: Fully irrelevant article
- **WHEN** no content in an article is relevant to the topic
- **THEN** the response has `relevanceScore` 0-2 and an empty or minimal summary

#### Scenario: Non-aggregated article scored and summarized
- **WHEN** an article from a non-aggregated source (1:1 post mapping) is processed
- **THEN** the response contains `relevanceScore` and `summary`

#### Scenario: Attribution preserved in summary
- **WHEN** a post body states "Researchers at MIT published a study showing..."
- **THEN** the summary retains the attribution, e.g., "MIT researchers found that..."

#### Scenario: Token usage persisted
- **WHEN** the LLM call uses 500 input tokens and 80 output tokens
- **THEN** the article's `llmInputTokens` is set to 500 and `llmOutputTokens` to 80

#### Scenario: Aggregated article prompt includes post context
- **WHEN** an aggregated article with title "Posts from @rauchg — Feb 15, 2026" and author "@rauchg" is scored
- **THEN** the LLM prompt states the content consists of multiple social media posts by @rauchg

#### Scenario: Non-aggregated article uses neutral framing
- **WHEN** a single article titled "New AI Breakthrough" is scored
- **THEN** the LLM prompt uses "Content title" and "Content" labels, not "Article title" and "Article text"

#### Scenario: Summary uses direct factual statements
- **WHEN** an article about Anthropic's funding round is summarized
- **THEN** the summary states facts directly (e.g., "Anthropic closed a $30B Series G round") rather than meta-describing (e.g., "The article discusses Anthropic's funding round")

#### Scenario: Author context included in prompt
- **WHEN** an article has `author` = "@simonw"
- **THEN** the LLM prompt includes "@simonw" as the content author for attribution context
