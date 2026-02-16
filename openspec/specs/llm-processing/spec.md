# Capability: LLM Processing

## Purpose

Three-stage LLM pipeline for processing articles (relevance scoring, conditional summarization, briefing script composition) using Spring AI and OpenRouter.

## MODIFIED Requirements

### Requirement: Two-step LLM pipeline
The system SHALL process articles through a two-stage sequential LLM pipeline: (1) score, summarize, and filter, (2) briefing script composition. The pipeline SHALL be triggered by the `BriefingGenerationScheduler` on a configurable cron schedule.

Before running the LLM stages, the pipeline SHALL invoke the `SourceAggregator` to create articles from unlinked posts. For each source belonging to the podcast, the aggregator queries unlinked posts within the configured time window and creates articles (aggregated or 1:1 depending on the source's `aggregate` setting). The resulting articles are persisted to the `articles` table with `post_articles` join entries.

Stage 1 (score+summarize+filter) SHALL process each newly created article with a single LLM call that returns structured JSON containing: `relevanceScore` (integer 0-10), `summary` (text, 2-3 sentences of relevant content), `includedPostIds` (array of post IDs deemed relevant), and `excludedPostIds` (array of post IDs deemed irrelevant). The system SHALL persist `relevance_score` and `summary` on the article immediately. Articles with `relevanceScore` below the podcast's `relevanceThreshold` SHALL be excluded from composition. For non-aggregated articles (single post), `includedPostIds` and `excludedPostIds` MAY be omitted from the response.

Stage 2 (composition) SHALL query articles where `relevance_score >= podcast.relevanceThreshold` AND `is_processed = false`, and compose a briefing script. After composition, articles SHALL be marked `is_processed = true`.

Both stages SHALL track token usage. Stage 1 SHALL persist token counts on each article. Stage 2 SHALL return token counts in the `PipelineResult`.

Stage 1 SHALL use the model resolved for the `filter` stage. Stage 2 SHALL use the model resolved for the `compose` stage.

#### Scenario: Full pipeline produces a briefing script
- **WHEN** the pipeline runs and there are unlinked posts for the podcast's sources
- **THEN** posts are aggregated into articles, each article is scored+summarized+filtered in one LLM call, and relevant articles are composed into a briefing script

#### Scenario: Pipeline skipped when no unlinked posts exist
- **WHEN** the pipeline runs and there are no unlinked posts within the time window for any source
- **THEN** no articles are created and the pipeline SHALL still check for existing unprocessed relevant articles to compose

#### Scenario: Pipeline resumes after partial completion
- **WHEN** the pipeline previously created articles and scored some but crashed before composition
- **THEN** on the next run, already-scored articles are not re-scored; new unlinked posts are aggregated; the pipeline proceeds to composition

#### Scenario: No relevant articles after scoring
- **WHEN** all scored articles have `relevanceScore` below the podcast's `relevanceThreshold`
- **THEN** no composition LLM call is made and no briefing is generated

#### Scenario: Irrelevant posts filtered during summarization
- **WHEN** an aggregated article contains 10 posts, 3 of which are irrelevant to the topic
- **THEN** the LLM's summary covers only the 7 relevant posts, and `excludedPostIds` lists the 3 irrelevant post IDs

### Requirement: Score, summarize, and filter stage
The system SHALL process each article through a single LLM call that performs scoring, summarization, and content filtering simultaneously. The LLM prompt SHALL include the podcast's topic, the full article content, and instructions to: (1) assign a relevance score of 0-10, (2) summarize the relevant content in 2-3 sentences, (3) identify which posts are relevant and which are not. The prompt SHALL request a JSON response with the structure: `{ "relevanceScore": <int>, "summary": "<text>", "includedPostIds": [<ids>], "excludedPostIds": [<ids>] }`. The prompt SHALL instruct the LLM to preserve attribution in the summary. The system SHALL persist the `relevanceScore` and `summary` on the article immediately after the call. Token usage SHALL be extracted and persisted on the article.

The prompt SHALL use content-type-aware framing:
- For aggregated articles (title starts with "Posts from"): The prompt SHALL state that the content consists of multiple social media posts and SHALL name the author (from `article.author`) when available.
- For non-aggregated articles: The prompt SHALL use neutral framing ("Content title" / "Content") instead of "Article title" / "Article text".

The prompt SHALL include the author name (from `article.author`) as context when available, regardless of content type.

The prompt SHALL instruct the LLM to write summaries as direct factual statements about what happened, not meta-descriptions of the content. The prompt SHALL include an explicit negative example: say "Anthropic launched X" not "The article discusses Anthropic launching X".

#### Scenario: Article with mixed relevant and irrelevant posts
- **WHEN** an aggregated article contains posts about AI and posts about celebrity gossip, scored against topic "artificial intelligence"
- **THEN** the response has `relevanceScore` 6-8, a summary covering only the AI posts, and `excludedPostIds` listing the gossip posts

#### Scenario: Fully relevant article
- **WHEN** all posts in an article are relevant to the topic
- **THEN** the response has a high `relevanceScore`, a summary covering all content, and empty `excludedPostIds`

#### Scenario: Fully irrelevant article
- **WHEN** no posts in an article are relevant to the topic
- **THEN** the response has `relevanceScore` 0-2 and an empty or minimal summary

#### Scenario: Non-aggregated article scored and summarized
- **WHEN** an article from a non-aggregated source (1:1 post mapping) is processed
- **THEN** the response contains `relevanceScore` and `summary` without `includedPostIds`/`excludedPostIds` (optional for single-post articles)

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
The system SHALL compose all relevant articles into a single coherent briefing script. For each article, the composer SHALL use the article's `summary` if available, or the article's `body` if no summary exists (short articles). The prompt SHALL include the current date (human-readable format), the podcast name, and the podcast topic as context. The prompt SHALL instruct the LLM to mention the podcast name, topic, and current date in the introduction. Each article in the summary block SHALL include the source domain name (extracted from the article URL) and the author name when available (format: `[domain, by Author]` or `[domain]` when author is null). The prompt SHALL instruct the LLM to naturally attribute information to its source and credit original authors when known — without over-citing. The prompt SHALL instruct the LLM to use natural spoken language (not written style), include transitions between topics, and target approximately the configured word count (default: 1500 words for ~10 minutes of audio). The prompt SHALL explicitly prohibit bracketed section headers (e.g., `[Opening]`, `[Closing]`, `[Transition]`), stage directions, sound effects, and any other non-spoken text. After receiving the LLM response, the system SHALL sanitize the script by removing any lines consisting solely of bracketed headers (pattern: lines matching `[...]` with no other content). This step SHALL use a more capable model (configurable, e.g., `anthropic/claude-sonnet-4-20250514`). When the podcast's language is not English, the prompt SHALL instruct the LLM to write the entire script in the specified language. The current date in the prompt SHALL be formatted using the locale corresponding to the podcast's language.

#### Scenario: Composer uses summary for scored articles
- **WHEN** an article has a non-null `summary` from the score+summarize stage
- **THEN** the composer uses the summary in the article block

#### Scenario: Composer uses body for unsummarized articles
- **WHEN** an article has a null `summary`
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

## REMOVED Requirements

### Requirement: Relevance scoring stage
**Reason**: Replaced by the combined score+summarize+filter stage. Relevance scoring is now performed as part of a single LLM call that also summarizes and filters content, reducing the number of LLM calls per article from 2 to 1.
**Migration**: The `RelevanceScorer` component is replaced by the new score+summarize+filter stage in the LLM pipeline.

### Requirement: Conditional summarization stage
**Reason**: Replaced by the combined score+summarize+filter stage. Summarization is now performed in the same LLM call as scoring, with the model filtering out irrelevant content during summarization.
**Migration**: The `ArticleSummarizer` component is replaced by the new score+summarize+filter stage in the LLM pipeline.
