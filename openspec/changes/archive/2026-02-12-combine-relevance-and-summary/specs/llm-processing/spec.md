## REMOVED Requirements

### Requirement: Relevance filtering
**Reason**: Merged into the new combined "Article processing" requirement.
**Migration**: Use `ArticleProcessor` instead of `RelevanceFilter`.

### Requirement: Per-article summarization
**Reason**: Merged into the new combined "Article processing" requirement.
**Migration**: Use `ArticleProcessor` instead of `ArticleSummarizer`.

## ADDED Requirements

### Requirement: Combined article processing
The system SHALL process each unfiltered article through a single LLM call that performs both relevance scoring and conditional summarization. The prompt SHALL include the podcast topic and the full article body. The LLM SHALL return a structured JSON response containing a relevance score (integer 1-5), a one-sentence justification, and an optional 2-3 sentence summary. The summary SHALL only be included when the relevance score is >= 3. Articles scoring below 3 SHALL be marked as `is_relevant` = false with no summary. Articles scoring 3 or above SHALL be marked as `is_relevant` = true with the summary populated. This step SHALL use the configured cheap model.

#### Scenario: Relevant article scored and summarized in one call
- **WHEN** an article about "new GPT-5 model capabilities" is processed with topic "AI engineering"
- **THEN** the article receives a score of 3 or above, is marked `is_relevant` = true, and has a 2-3 sentence summary populated

#### Scenario: Irrelevant article scored without summary
- **WHEN** an article about "best pizza recipes" is processed with topic "AI engineering"
- **THEN** the article receives a score below 3, is marked `is_relevant` = false, and has no summary

#### Scenario: Summary ignored when score is below threshold
- **WHEN** the LLM returns a score of 2 but includes a summary in the response
- **THEN** the system marks `is_relevant` = false and does not persist the summary

#### Scenario: Structured output parsing
- **WHEN** the LLM returns a combined response
- **THEN** the response is deserialized into an `ArticleProcessingResult` data class with fields `score`, `justification`, and optional `summary`

## MODIFIED Requirements

### Requirement: Three-step LLM pipeline
The system SHALL process unprocessed articles through a two-step sequential LLM pipeline via Spring AI's ChatClient connected to OpenRouter: (1) combined relevance filtering and summarization, (2) briefing script composition. The pipeline SHALL be triggered by the `BriefingGenerationScheduler` on a configurable cron schedule.

#### Scenario: Full pipeline produces a briefing script
- **WHEN** the pipeline runs and there are unprocessed, relevant articles
- **THEN** a briefing script is produced containing summaries of all relevant articles composed into a coherent monologue

#### Scenario: Pipeline skipped when no unprocessed articles exist
- **WHEN** the pipeline runs and there are no articles with `is_processed` = false
- **THEN** no LLM calls are made and no briefing is generated

### Requirement: Model switching per LLM call
The system SHALL use a single ChatClient instance and override the model per call via `OpenAiChatOptions`. The combined article processing step SHALL use the configured cheap model. The briefing composition step SHALL use the configured capable model.

#### Scenario: Article processing uses cheap model
- **WHEN** the article processing step makes an LLM call
- **THEN** the call uses the cheap model configured in application properties

#### Scenario: Briefing composition uses capable model
- **WHEN** the briefing composition step makes an LLM call
- **THEN** the call uses the capable model configured in application properties (the default from Spring AI config)
