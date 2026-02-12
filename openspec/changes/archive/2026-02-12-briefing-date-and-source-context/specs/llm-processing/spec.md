## MODIFIED Requirements

### Requirement: Briefing script composition
The system SHALL compose all individual summaries into a single coherent briefing script. The prompt SHALL include the current date (human-readable format), the podcast name, and the podcast topic as context. The prompt SHALL instruct the LLM to mention the podcast name, topic, and current date in the introduction. Each article in the summary block SHALL include the source domain name (extracted from the article URL). The prompt SHALL instruct the LLM to subtly and sparingly attribute information to its source throughout the script. The prompt SHALL instruct the LLM to use natural spoken language (not written style), include transitions between topics, and target approximately the configured word count (default: 1500 words for ~10 minutes of audio). The prompt SHALL explicitly prohibit bracketed section headers (e.g., `[Opening]`, `[Closing]`, `[Transition]`), stage directions, sound effects, and any other non-spoken text. After receiving the LLM response, the system SHALL sanitize the script by removing any lines consisting solely of bracketed headers (pattern: lines matching `[...]` with no other content). This step SHALL use a more capable model (configurable, e.g., `anthropic/claude-sonnet-4-20250514`).

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
