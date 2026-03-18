# Capability: LLM Processing

## Purpose

Three-stage LLM pipeline for processing articles (relevance scoring, conditional summarization, briefing script composition) using Spring AI and OpenRouter.

## Requirements

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
The system SHALL process each article through a single LLM call that performs scoring and summarization simultaneously. Articles SHALL be processed concurrently using coroutines with `supervisorScope` for fault isolation — a failure in one article's LLM call SHALL NOT cancel or affect processing of other articles.

The system SHALL limit the number of concurrent LLM requests using a configurable concurrency window (default: 10). All articles are dispatched as coroutines, but at most N coroutines SHALL execute their LLM call simultaneously. When a request completes, the next waiting coroutine SHALL proceed immediately (sliding window, not batch-based).

The system SHALL retry transient LLM failures with exponential backoff before giving up on an article. The maximum number of attempts SHALL be configurable (default: 3, where 1 means no retry). The backoff delay SHALL double on each retry (1s, 2s, 4s, ...). Each retry attempt SHALL be logged at WARN level with the attempt number, article title, and error message. Only after all retry attempts are exhausted SHALL the article be excluded from the result list and logged as an ERROR.

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

#### Scenario: Articles processed with concurrency limit
- **WHEN** 50 articles are submitted for scoring and the concurrency limit is 10
- **THEN** at most 10 LLM calls SHALL be in-flight simultaneously, with new calls starting as previous ones complete

#### Scenario: Concurrency window acts as sliding window
- **WHEN** 20 articles are submitted with concurrency limit 10 and the 3rd article completes before others
- **THEN** the 11th article SHALL start immediately without waiting for the entire first batch of 10 to complete

#### Scenario: Transient failure retried successfully
- **WHEN** an article's LLM call fails on the 1st attempt but succeeds on the 2nd attempt
- **THEN** the article SHALL be scored and included in the result, and a WARN log SHALL record the retry

#### Scenario: All retries exhausted
- **WHEN** an article's LLM call fails on all 3 attempts (default max-retries)
- **THEN** the article SHALL be excluded from the result list and an ERROR SHALL be logged

#### Scenario: Retry uses exponential backoff
- **WHEN** an article's LLM call fails on the 1st attempt
- **THEN** the system SHALL wait 1 second before the 2nd attempt, and 2 seconds before the 3rd attempt

#### Scenario: One article failure does not cancel others
- **WHEN** 3 articles are submitted and the 2nd article's LLM call throws an exception on all retry attempts
- **THEN** the 1st and 3rd articles SHALL still be processed and returned successfully, and the 2nd article SHALL be excluded from the result

#### Scenario: All articles fail gracefully
- **WHEN** 3 articles are submitted and all LLM calls throw exceptions on all retry attempts
- **THEN** an empty list SHALL be returned and all errors SHALL be logged

#### Scenario: Retry logs include attempt details
- **WHEN** an article's LLM call fails on the 1st attempt and succeeds on the 2nd
- **THEN** the WARN log SHALL include the attempt number (1/3), the article title, and the error message

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

### Requirement: Briefing script composition
The system SHALL compose all relevant articles into a single coherent briefing script. The composer SHALL select article content based on a full-body threshold:
- When the number of relevant articles is **below** the podcast's `fullBodyThreshold` (default: 5, configurable per-podcast with global fallback), the composer SHALL use the full `article.body` for all articles.
- When the number of relevant articles is **at or above** the threshold, the composer SHALL use `article.summary` if available, or `article.body` if no summary exists.

The prompt SHALL include the current date (human-readable format), the podcast name, and the podcast topic as context. The prompt SHALL instruct the LLM to mention the podcast name, topic, and current date in the introduction. Each article in the content block SHALL include the source domain name (extracted from the article URL) and the author name when available (format: `[domain, by Author]` or `[domain]` when author is null). The prompt SHALL instruct the LLM to naturally attribute information to its source and credit original authors when known — without over-citing. The prompt SHALL instruct the LLM to use natural spoken language (not written style), include transitions between topics, and target approximately the configured word count (default: 1500 words for ~10 minutes of audio). The prompt SHALL explicitly prohibit bracketed section headers (e.g., `[Opening]`, `[Closing]`, `[Transition]`), stage directions, sound effects, and any other non-spoken text. After receiving the LLM response, the system SHALL sanitize the script by removing any lines consisting solely of bracketed headers (pattern: lines matching `[...]` with no other content). This step SHALL use a more capable model (configurable, e.g., `anthropic/claude-sonnet-4-20250514`). When the podcast's language is not English, the prompt SHALL instruct the LLM to write the entire script in the specified language. The current date in the prompt SHALL be formatted using the locale corresponding to the podcast's language.

The prompt SHALL include a grounding instruction requiring the LLM to ONLY discuss topics, facts, and claims that are directly present in the provided article summaries. The LLM SHALL NOT introduce information from its own training knowledge. When few articles are provided, the LLM SHALL produce a proportionally shorter script rather than padding with external knowledge.

When the podcast has a non-null `sponsor` field, the prompt SHALL instruct the LLM to include a sponsor message immediately after the introduction using the configured sponsor name and message (e.g., "This podcast is brought to you by {sponsor.name} — {sponsor.message}."). The prompt SHALL also instruct the LLM to mention the sponsor name in the sign-off/outro. When the podcast has no sponsor configured (null), the prompt SHALL NOT include any sponsor-related instructions.

The full-body threshold behavior SHALL be applied to all three composer variants: `BriefingComposer`, `DialogueComposer`, and `InterviewComposer`.

#### Scenario: Few articles triggers full body usage
- **WHEN** 3 relevant articles are passed to the composer and the podcast's `fullBodyThreshold` is 5
- **THEN** the composer uses the full `article.body` for all 3 articles in the prompt

#### Scenario: Many articles uses summaries
- **WHEN** 8 relevant articles are passed to the composer and the podcast's `fullBodyThreshold` is 5
- **THEN** the composer uses `article.summary` (or `article.body` as fallback) for each article

#### Scenario: Threshold at exact boundary uses summaries
- **WHEN** 5 relevant articles are passed to the composer and the podcast's `fullBodyThreshold` is 5
- **THEN** the composer uses `article.summary` (or `article.body` as fallback), since the count is not below the threshold

#### Scenario: Per-podcast threshold override
- **WHEN** a podcast has `fullBodyThreshold` set to 3 and 4 relevant articles are passed
- **THEN** the composer uses summaries, since 4 is not below 3

#### Scenario: Composer uses summary for scored articles
- **WHEN** article count is at or above the threshold and an article has a non-null `summary`
- **THEN** the composer uses the summary in the article block

#### Scenario: Composer uses body for unsummarized articles
- **WHEN** article count is at or above the threshold and an article has a null `summary`
- **THEN** the composer uses the full article body in the article block

#### Scenario: Multiple articles composed into briefing
- **WHEN** 5 relevant articles are passed to the composition step
- **THEN** a single briefing script is produced that covers all 5 topics with spoken-language transitions

#### Scenario: Script does not contain bracketed section headers
- **WHEN** a briefing script is generated
- **THEN** the output SHALL NOT contain any bracketed section headers such as `[Opening]`, `[Transition]`, or `[Closing]`

#### Scenario: Non-English briefing script
- **WHEN** a briefing is composed for a podcast with language `"nl"` (Dutch)
- **THEN** the LLM prompt instructs the model to write the entire script in Dutch

#### Scenario: LLM does not introduce external knowledge
- **WHEN** the composer receives 3 articles about topics A, B, and C
- **THEN** the generated script discusses only topics A, B, and C without introducing additional topics from the LLM's training data

#### Scenario: Short script for few articles
- **WHEN** the composer receives only 2 articles
- **THEN** the generated script is proportionally shorter than when receiving 10 articles, rather than padded with external content

#### Scenario: Sponsor message included when configured
- **WHEN** a podcast has `sponsor: {"name": "source-labs", "message": "experts in agentic software development"}` and any composer generates a script
- **THEN** the prompt instructs the LLM to include "This podcast is brought to you by source-labs — experts in agentic software development" after the introduction

#### Scenario: Sponsor mentioned in sign-off when configured
- **WHEN** a podcast has a non-null sponsor and any composer generates a script
- **THEN** the prompt instructs the LLM to mention the sponsor name in the sign-off/outro

#### Scenario: No sponsor instructions when not configured
- **WHEN** a podcast has `sponsor` set to null and any composer generates a script
- **THEN** the prompt does not include any sponsor-related instructions

#### Scenario: Sponsor name used as-is from configuration
- **WHEN** a podcast has `sponsor: {"name": "Acme Corp", "message": "innovating tomorrow"}`
- **THEN** the prompt uses "Acme Corp" as the sponsor name (no hardcoded name)
