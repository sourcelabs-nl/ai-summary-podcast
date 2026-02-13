# Capability: LLM Processing

## Purpose

Two-step LLM pipeline for processing articles (combined relevance filtering and summarization) and composing briefing scripts using Spring AI and OpenRouter.

## Requirements

### Requirement: Two-step LLM pipeline
The system SHALL process unprocessed articles through a two-step sequential LLM pipeline via Spring AI's ChatClient connected to OpenRouter: (1) combined relevance filtering and summarization, (2) briefing script composition. The pipeline SHALL be triggered by the `BriefingGenerationScheduler` on a configurable cron schedule.

#### Scenario: Full pipeline produces a briefing script
- **WHEN** the pipeline runs and there are unprocessed, relevant articles
- **THEN** a briefing script is produced containing summaries of all relevant articles composed into a coherent monologue

#### Scenario: Pipeline skipped when no unprocessed articles exist
- **WHEN** the pipeline runs and there are no articles with `is_processed` = false
- **THEN** no LLM calls are made and no briefing is generated

### Requirement: Briefing script composition
The system SHALL compose all individual summaries into a single coherent briefing script. The prompt SHALL include the current date (human-readable format), the podcast name, and the podcast topic as context. The prompt SHALL instruct the LLM to mention the podcast name, topic, and current date in the introduction. Each article in the summary block SHALL include the source domain name (extracted from the article URL). The prompt SHALL instruct the LLM to subtly and sparingly attribute information to its source throughout the script. The prompt SHALL instruct the LLM to use natural spoken language (not written style), include transitions between topics, and target approximately the configured word count (default: 1500 words for ~10 minutes of audio). The prompt SHALL explicitly prohibit bracketed section headers (e.g., `[Opening]`, `[Closing]`, `[Transition]`), stage directions, sound effects, and any other non-spoken text. After receiving the LLM response, the system SHALL sanitize the script by removing any lines consisting solely of bracketed headers (pattern: lines matching `[...]` with no other content). This step SHALL use a more capable model (configurable, e.g., `anthropic/claude-sonnet-4-20250514`). When the podcast's language is not English, the prompt SHALL instruct the LLM to write the entire script in the specified language. The current date in the prompt SHALL be formatted using the locale corresponding to the podcast's language.

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
The system SHALL preserve article summaries when marking articles as processed. After the summarization step produces summaries, the pipeline SHALL use the summarized articles (not the original unsummarized articles) when setting `is_processed` = true, ensuring the summary field is not overwritten with null.

#### Scenario: Summary preserved when article marked as processed
- **WHEN** an article is summarized and then marked as processed
- **THEN** the saved article has both `is_processed` = true and the summary field populated

### Requirement: Model switching per LLM call
The system SHALL resolve a named model for each pipeline stage via the model resolution chain (podcast override → global default → registry lookup). The `ArticleProcessor` SHALL resolve the model for stage `"filter"` and create a `ChatClient` using that model's provider and credentials. The `BriefingComposer` SHALL resolve the model for stage `"compose"` and create a `ChatClient` using that model's provider and credentials. Each stage MAY use a different provider and model. The `ChatClientFactory` SHALL accept a user ID and a `ModelDefinition` (provider + model ID) and create a `ChatClient` configured for that specific provider's base URL and the user's credentials for that provider.

#### Scenario: Article processing uses resolved filter model
- **WHEN** the article processing step runs for a podcast whose resolved filter model is "cheap" (provider: openrouter, model: anthropic/claude-haiku-4.5)
- **THEN** the ChatClient is created using the user's openrouter credentials and the call uses model "anthropic/claude-haiku-4.5"

#### Scenario: Briefing composition uses resolved compose model
- **WHEN** the briefing composition step runs for a podcast whose resolved compose model is "capable" (provider: openrouter, model: anthropic/claude-sonnet-4)
- **THEN** the ChatClient is created using the user's openrouter credentials and the call uses model "anthropic/claude-sonnet-4"

#### Scenario: Different providers per stage
- **WHEN** the filter stage resolves to model "cheap" (provider: openrouter) and the compose stage resolves to model "local" (provider: ollama)
- **THEN** two different ChatClient instances are created — one using the user's openrouter config, one using the user's ollama config

#### Scenario: Pipeline fails if model name not in registry
- **WHEN** the pipeline starts and a podcast's resolved model name is not found in the registry
- **THEN** the pipeline fails immediately with an `IllegalArgumentException` before making any LLM calls
