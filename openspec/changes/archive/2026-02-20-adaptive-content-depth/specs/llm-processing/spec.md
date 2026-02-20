## MODIFIED Requirements

### Requirement: Score, summarize, and filter stage
The system SHALL process each article through a single LLM call that performs scoring, summarization, and content filtering simultaneously. The LLM prompt SHALL include the podcast's topic, the full article content, and instructions to: (1) assign a relevance score of 0-10, (2) summarize the relevant content, (3) identify which posts are relevant and which are not. The prompt SHALL request a JSON response with the structure: `{ "relevanceScore": <int>, "summary": "<text>", "includedPostIds": [<ids>], "excludedPostIds": [<ids>] }`. The prompt SHALL instruct the LLM to preserve attribution in the summary. The system SHALL persist the `relevanceScore` and `summary` on the article immediately after the call. Token usage SHALL be extracted and persisted on the article.

The prompt SHALL scale the requested summary length based on the word count of the article body:
- Articles with fewer than 500 words: 2-3 sentences.
- Articles with 500-1500 words: 4-6 sentences.
- Articles with more than 1500 words: a full paragraph covering key points, context, and attribution.

The prompt SHALL use content-type-aware framing:
- For aggregated articles (title starts with "Posts from"): The prompt SHALL state that the content consists of multiple social media posts and SHALL name the author (from `article.author`) when available.
- For non-aggregated articles: The prompt SHALL use neutral framing ("Content title" / "Content") instead of "Article title" / "Article text".

The prompt SHALL include the author name (from `article.author`) as context when available, regardless of content type.

The prompt SHALL instruct the LLM to write summaries as direct factual statements about what happened, not meta-descriptions of the content. The prompt SHALL include an explicit negative example: say "Anthropic launched X" not "The article discusses Anthropic launching X".

#### Scenario: Short article summarized in 2-3 sentences
- **WHEN** an article with a body of 300 words is scored and summarized
- **THEN** the summary contains 2-3 sentences

#### Scenario: Medium article summarized in 4-6 sentences
- **WHEN** an article with a body of 1000 words is scored and summarized
- **THEN** the summary contains 4-6 sentences with additional context beyond the current 2-3 sentence default

#### Scenario: Long article summarized in a full paragraph
- **WHEN** an article with a body of 2500 words is scored and summarized
- **THEN** the summary is a full paragraph covering key points, context, and attribution

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
The system SHALL compose all relevant articles into a single coherent briefing script. The composer SHALL select article content based on a full-body threshold:
- When the number of relevant articles is **below** the podcast's `fullBodyThreshold` (default: 5, configurable per-podcast with global fallback), the composer SHALL use the full `article.body` for all articles.
- When the number of relevant articles is **at or above** the threshold, the composer SHALL use `article.summary` if available, or `article.body` if no summary exists.

The prompt SHALL include the current date (human-readable format), the podcast name, and the podcast topic as context. The prompt SHALL instruct the LLM to mention the podcast name, topic, and current date in the introduction. Each article in the content block SHALL include the source domain name (extracted from the article URL) and the author name when available (format: `[domain, by Author]` or `[domain]` when author is null). The prompt SHALL instruct the LLM to naturally attribute information to its source and credit original authors when known — without over-citing. The prompt SHALL instruct the LLM to use natural spoken language (not written style), include transitions between topics, and target approximately the configured word count (default: 1500 words for ~10 minutes of audio). The prompt SHALL explicitly prohibit bracketed section headers (e.g., `[Opening]`, `[Closing]`, `[Transition]`), stage directions, sound effects, and any other non-spoken text. After receiving the LLM response, the system SHALL sanitize the script by removing any lines consisting solely of bracketed headers (pattern: lines matching `[...]` with no other content). This step SHALL use a more capable model (configurable, e.g., `anthropic/claude-sonnet-4-20250514`). When the podcast's language is not English, the prompt SHALL instruct the LLM to write the entire script in the specified language. The current date in the prompt SHALL be formatted using the locale corresponding to the podcast's language.

The prompt SHALL include a grounding instruction requiring the LLM to ONLY discuss topics, facts, and claims that are directly present in the provided article summaries. The LLM SHALL NOT introduce information from its own training knowledge. When few articles are provided, the LLM SHALL produce a proportionally shorter script rather than padding with external knowledge.

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
