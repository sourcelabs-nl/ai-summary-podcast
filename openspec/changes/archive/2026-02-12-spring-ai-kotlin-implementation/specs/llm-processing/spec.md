## ADDED Requirements

### Requirement: Three-step LLM pipeline
The system SHALL process unprocessed articles through a three-step sequential LLM pipeline via Spring AI's ChatClient connected to OpenRouter: (1) relevance filtering, (2) per-article summarization, (3) briefing script composition. The pipeline SHALL be triggered by the `BriefingGenerationScheduler` on a configurable cron schedule.

#### Scenario: Full pipeline produces a briefing script
- **WHEN** the pipeline runs and there are unprocessed, relevant articles
- **THEN** a briefing script is produced containing summaries of all relevant articles composed into a coherent monologue

#### Scenario: Pipeline skipped when no unprocessed articles exist
- **WHEN** the pipeline runs and there are no articles with `is_processed` = false
- **THEN** no LLM calls are made and no briefing is generated

### Requirement: Relevance filtering
The system SHALL send each unprocessed article's title and first 500 words, along with the configured topic, to the LLM. The LLM SHALL return a structured response with a relevance score (1-5) and a one-sentence justification. Articles scoring below 3 SHALL be marked as `is_relevant` = false. Articles scoring 3 or above SHALL be marked as `is_relevant` = true. This step SHALL use a cheap, fast model (configurable, e.g., `anthropic/claude-3-haiku`). Articles MAY be batched into a single LLM call.

#### Scenario: Article scored as relevant
- **WHEN** an article about "new GPT-5 model capabilities" is filtered with topic "AI engineering"
- **THEN** the article receives a score of 3 or above and is marked `is_relevant` = true

#### Scenario: Article scored as irrelevant
- **WHEN** an article about "best pizza recipes" is filtered with topic "AI engineering"
- **THEN** the article receives a score below 3 and is marked `is_relevant` = false

#### Scenario: Structured output parsing
- **WHEN** the LLM returns a relevance response
- **THEN** the response is deserialized into a `RelevanceResult` data class using Spring AI's entity mapping

### Requirement: Per-article summarization
The system SHALL generate a 2-3 sentence summary for each article marked as relevant and not yet processed. Each summary SHALL capture the key information from the article.

#### Scenario: Relevant article summarized
- **WHEN** an article with `is_relevant` = true is passed to the summarization step
- **THEN** a 2-3 sentence summary is generated and associated with the article

#### Scenario: Irrelevant articles skipped
- **WHEN** articles with `is_relevant` = false exist
- **THEN** they are not sent to the summarization step

### Requirement: Briefing script composition
The system SHALL compose all individual summaries into a single coherent briefing script. The prompt SHALL instruct the LLM to use natural spoken language (not written style), include transitions between topics, and target approximately the configured word count (default: 1500 words for ~10 minutes of audio). This step SHALL use a more capable model (configurable, e.g., `anthropic/claude-sonnet-4-20250514`).

#### Scenario: Multiple summaries composed into briefing
- **WHEN** 5 article summaries are passed to the composition step
- **THEN** a single briefing script is produced that covers all 5 topics with spoken-language transitions

#### Scenario: Single summary composed into briefing
- **WHEN** only 1 article summary is available
- **THEN** a briefing script is still produced covering that single topic

### Requirement: Model switching per LLM call
The system SHALL use a single ChatClient instance and override the model per call via `OpenAiChatOptions`. The relevance filtering and summarization steps SHALL use the configured cheap model. The briefing composition step SHALL use the configured capable model.

#### Scenario: Relevance filter uses cheap model
- **WHEN** the relevance filter step makes an LLM call
- **THEN** the call uses the cheap model configured in application properties

#### Scenario: Briefing composition uses capable model
- **WHEN** the briefing composition step makes an LLM call
- **THEN** the call uses the capable model configured in application properties (the default from Spring AI config)
