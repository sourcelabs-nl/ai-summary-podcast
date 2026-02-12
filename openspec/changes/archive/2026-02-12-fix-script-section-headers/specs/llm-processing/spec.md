## MODIFIED Requirements

### Requirement: Briefing script composition
The system SHALL compose all individual summaries into a single coherent briefing script. The prompt SHALL instruct the LLM to use natural spoken language (not written style), include transitions between topics, and target approximately the configured word count (default: 1500 words for ~10 minutes of audio). The prompt SHALL explicitly prohibit bracketed section headers (e.g., `[Opening]`, `[Closing]`, `[Transition]`), stage directions, sound effects, and any other non-spoken text. After receiving the LLM response, the system SHALL sanitize the script by removing any lines consisting solely of bracketed headers (pattern: lines matching `[...]` with no other content). This step SHALL use a more capable model (configurable, e.g., `anthropic/claude-sonnet-4-20250514`).

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
