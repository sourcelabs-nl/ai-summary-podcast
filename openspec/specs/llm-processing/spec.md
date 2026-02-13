# Capability: LLM Processing

## Purpose

Three-stage LLM pipeline for processing articles (relevance scoring, conditional summarization, briefing script composition) using Spring AI and OpenRouter.

## Requirements

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

### Requirement: Briefing script composition
The system SHALL compose all relevant articles into a single coherent briefing script. For each article, the composer SHALL use the article's `summary` if available, or the article's `body` if no summary exists (short articles). The prompt SHALL include the current date (human-readable format), the podcast name, and the podcast topic as context. The prompt SHALL instruct the LLM to mention the podcast name, topic, and current date in the introduction. Each article in the summary block SHALL include the source domain name (extracted from the article URL). The prompt SHALL instruct the LLM to subtly and sparingly attribute information to its source throughout the script. The prompt SHALL instruct the LLM to use natural spoken language (not written style), include transitions between topics, and target approximately the configured word count (default: 1500 words for ~10 minutes of audio). The prompt SHALL explicitly prohibit bracketed section headers (e.g., `[Opening]`, `[Closing]`, `[Transition]`), stage directions, sound effects, and any other non-spoken text. After receiving the LLM response, the system SHALL sanitize the script by removing any lines consisting solely of bracketed headers (pattern: lines matching `[...]` with no other content). This step SHALL use a more capable model (configurable, e.g., `anthropic/claude-sonnet-4-20250514`). When the podcast's language is not English, the prompt SHALL instruct the LLM to write the entire script in the specified language. The current date in the prompt SHALL be formatted using the locale corresponding to the podcast's language.

#### Scenario: Composer uses summary for long articles
- **WHEN** an article has a non-null `summary`
- **THEN** the composer uses the summary in the article block

#### Scenario: Composer uses body for short articles
- **WHEN** an article has a null `summary` (short article that skipped summarization)
- **THEN** the composer uses the full article body in the article block

#### Scenario: Mixed short and long articles in briefing
- **WHEN** 3 relevant articles are composed, where 2 have summaries and 1 has only a body
- **THEN** the composer uses summaries for the first two and body for the third, producing a coherent script

#### Scenario: Multiple summaries composed into briefing
- **WHEN** 5 article summaries are passed to the composition step
- **THEN** a single briefing script is produced that covers all 5 topics with spoken-language transitions

#### Scenario: Single summary composed into briefing
- **WHEN** only 1 article summary is available
- **THEN** a briefing script is still produced covering that single topic

#### Scenario: Script does not contain bracketed section headers
- **WHEN** a briefing script is generated
- **THEN** the output SHALL NOT contain any bracketed section headers such as `[Opening]`, `[Transition]`, or `[Closing]`

#### Scenario: Post-generation sanitization removes residual headers
- **WHEN** the LLM returns a script containing lines like `[Opening]` or `[Closing]`
- **THEN** the system removes those lines before returning the final script

#### Scenario: Introduction mentions current date
- **WHEN** a briefing script is generated on February 12, 2026
- **THEN** the introduction mentions the date, e.g., "Wednesday, February 12th, 2026"

#### Scenario: Introduction mentions podcast name and topic
- **WHEN** a briefing is composed for podcast "AI Weekly" with topic "artificial intelligence"
- **THEN** the introduction mentions the podcast name and topic

#### Scenario: Source attribution included subtly
- **WHEN** articles from techcrunch.com and theverge.com are composed into a briefing
- **THEN** the script subtly references the source names, e.g., "according to TechCrunch" or "as reported by The Verge"

#### Scenario: Article summary block includes source domain
- **WHEN** an article with URL "https://techcrunch.com/2026/02/12/example" is included
- **THEN** the summary block entry for that article includes "techcrunch.com" as the source

#### Scenario: Non-English briefing script
- **WHEN** a briefing is composed for a podcast with language `"nl"` (Dutch)
- **THEN** the LLM prompt instructs the model to write the entire script in Dutch, and the script is produced in Dutch

#### Scenario: Date formatted in podcast language
- **WHEN** a briefing is composed for a podcast with language `"nl"` on February 12, 2026
- **THEN** the date in the prompt is formatted as "woensdag 12 februari 2026" using the Dutch locale

#### Scenario: English podcast unchanged
- **WHEN** a briefing is composed for a podcast with language `"en"`
- **THEN** no additional language instruction is added to the prompt and the date is formatted in English

### Requirement: Summary preservation during processing
The system SHALL preserve article summaries when marking articles as processed. After the composition step, the pipeline SHALL mark relevant articles with `is_processed` = true, ensuring the `summary` and `relevance_score` fields are not overwritten.

#### Scenario: Summary preserved when article marked as processed
- **WHEN** an article is summarized and then marked as processed
- **THEN** the saved article has `is_processed` = true, the summary field populated, and `relevance_score` preserved

### Requirement: Model switching per LLM call
The system SHALL resolve a named model for each pipeline stage via the model resolution chain (podcast override → global default → registry lookup). The scoring and summarization stages SHALL both resolve the model for stage `"filter"` and create a `ChatClient` using that model's provider and credentials. The `BriefingComposer` SHALL resolve the model for stage `"compose"` and create a `ChatClient` using that model's provider and credentials. Each stage MAY use a different provider and model. The `ChatClientFactory` SHALL accept a user ID and a `ModelDefinition` (provider + model ID) and create a `ChatClient` configured for that specific provider's base URL and the user's credentials for that provider.

#### Scenario: Scoring and summarization use resolved filter model
- **WHEN** the scoring and summarization steps run for a podcast whose resolved filter model is "cheap" (provider: openrouter, model: anthropic/claude-haiku-4.5)
- **THEN** the ChatClient is created using the user's openrouter credentials and both stages use model "anthropic/claude-haiku-4.5"

#### Scenario: Briefing composition uses resolved compose model
- **WHEN** the briefing composition step runs for a podcast whose resolved compose model is "capable" (provider: openrouter, model: anthropic/claude-sonnet-4)
- **THEN** the ChatClient is created using the user's openrouter credentials and the call uses model "anthropic/claude-sonnet-4"

#### Scenario: Different providers per stage
- **WHEN** the filter stage resolves to model "cheap" (provider: openrouter) and the compose stage resolves to model "local" (provider: ollama)
- **THEN** two different ChatClient instances are created — one using the user's openrouter config, one using the user's ollama config

#### Scenario: Pipeline fails if model name not in registry
- **WHEN** the pipeline starts and a podcast's resolved model name is not found in the registry
- **THEN** the pipeline fails immediately with an `IllegalArgumentException` before making any LLM calls

### Requirement: Relevance scoring stage
The system SHALL score each unscored article on a scale of 0-10 for relevance to the podcast's topic. The LLM prompt SHALL request a JSON response containing `score` (integer 0-10) and `justification` (one sentence explaining the score). The system SHALL persist the `relevance_score` on the article immediately after scoring. Articles are considered relevant if their score is greater than or equal to the podcast's `relevanceThreshold`.

#### Scenario: Article scored as highly relevant
- **WHEN** an article about "new GPT-5 release" is scored against topic "artificial intelligence"
- **THEN** the article receives a relevance_score of 8-10 and the score is persisted

#### Scenario: Article scored as irrelevant
- **WHEN** an article about "celebrity gossip" is scored against topic "artificial intelligence"
- **THEN** the article receives a relevance_score of 0-2 and the score is persisted

#### Scenario: Score persisted immediately
- **WHEN** an article is scored
- **THEN** the `relevance_score` is saved to the database before the next article is scored

### Requirement: Conditional summarization stage
The system SHALL summarize relevant articles only when the article body word count is greater than or equal to the global `app.llm.summarization-min-words` threshold (default: 500). Articles below this threshold SHALL NOT be summarized — their original body text will be used directly by the briefing composer. The summarization prompt SHALL request a 2-3 sentence summary capturing the key information. The summary SHALL be persisted on the article immediately after generation.

#### Scenario: Long article is summarized
- **WHEN** a relevant article has a body of 1200 words and the summarization threshold is 500
- **THEN** the article is summarized into 2-3 sentences and the summary is persisted

#### Scenario: Short article skips summarization
- **WHEN** a relevant article has a body of 300 words and the summarization threshold is 500
- **THEN** the article is NOT summarized and its `summary` field remains null

#### Scenario: Article at exact threshold is summarized
- **WHEN** a relevant article has a body of exactly 500 words and the summarization threshold is 500
- **THEN** the article is summarized

#### Scenario: Custom global threshold respected
- **WHEN** `app.llm.summarization-min-words` is configured to 800
- **THEN** only articles with 800 or more words are summarized
