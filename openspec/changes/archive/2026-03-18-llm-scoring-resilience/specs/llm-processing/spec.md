## MODIFIED Requirements

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
