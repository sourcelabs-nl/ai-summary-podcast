## MODIFIED Requirements

### Requirement: Conditional summarization stage
The system SHALL summarize relevant articles only when the article body word count is greater than or equal to the global `app.llm.summarization-min-words` threshold (default: 500). Articles below this threshold SHALL NOT be summarized — their original body text will be used directly by the briefing composer. The summarization prompt SHALL request a 2-3 sentence summary capturing the key information. The summarization prompt SHALL instruct the LLM to preserve attribution: if the article attributes information to a specific person, organization, or study, that attribution SHALL be retained in the summary. The summary SHALL be persisted on the article immediately after generation.

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

#### Scenario: Attribution preserved in summary
- **WHEN** an article body states "Researchers at MIT published a study showing..."
- **THEN** the summary retains the attribution, e.g., "MIT researchers found that..." rather than "A study showed..."

### Requirement: Briefing script composition
The system SHALL compose all relevant articles into a single coherent briefing script. For each article, the composer SHALL use the article's `summary` if available, or the article's `body` if no summary exists (short articles). The prompt SHALL include the current date (human-readable format), the podcast name, and the podcast topic as context. The prompt SHALL instruct the LLM to mention the podcast name, topic, and current date in the introduction. Each article in the summary block SHALL include the source domain name (extracted from the article URL) and the author name when available (format: `[domain, by Author]` or `[domain]` when author is null). The prompt SHALL instruct the LLM to naturally attribute information to its source and credit original authors when known — without over-citing. The prompt SHALL instruct the LLM to use natural spoken language (not written style), include transitions between topics, and target approximately the configured word count (default: 1500 words for ~10 minutes of audio). The prompt SHALL explicitly prohibit bracketed section headers (e.g., `[Opening]`, `[Closing]`, `[Transition]`), stage directions, sound effects, and any other non-spoken text. After receiving the LLM response, the system SHALL sanitize the script by removing any lines consisting solely of bracketed headers (pattern: lines matching `[...]` with no other content). This step SHALL use a more capable model (configurable, e.g., `anthropic/claude-sonnet-4-20250514`). When the podcast's language is not English, the prompt SHALL instruct the LLM to write the entire script in the specified language. The current date in the prompt SHALL be formatted using the locale corresponding to the podcast's language.

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

#### Scenario: Source and author attribution included in summary block
- **WHEN** an article from "https://techcrunch.com/2026/02/12/example" with author "John Smith" is included
- **THEN** the summary block entry shows `[techcrunch.com, by John Smith] Article Title`

#### Scenario: Source attribution without author in summary block
- **WHEN** an article from "https://theverge.com/2026/02/12/example" with no author is included
- **THEN** the summary block entry shows `[theverge.com] Article Title`

#### Scenario: Script naturally credits authors
- **WHEN** articles with known authors are composed into a briefing
- **THEN** the script naturally credits authors where appropriate, e.g., "as John Smith reports for TechCrunch"

#### Scenario: Non-English briefing script
- **WHEN** a briefing is composed for a podcast with language `"nl"` (Dutch)
- **THEN** the LLM prompt instructs the model to write the entire script in Dutch, and the script is produced in Dutch

#### Scenario: Date formatted in podcast language
- **WHEN** a briefing is composed for a podcast with language `"nl"` on February 12, 2026
- **THEN** the date in the prompt is formatted as "woensdag 12 februari 2026" using the Dutch locale

#### Scenario: English podcast unchanged
- **WHEN** a briefing is composed for a podcast with language `"en"`
- **THEN** no additional language instruction is added to the prompt and the date is formatted in English
