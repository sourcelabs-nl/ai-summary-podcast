# Capability: Source Configuration

## Purpose

Configuration loading for content sources and application settings, using Spring's configuration properties to define sources, topics, and operational parameters.

## Requirements

### Requirement: Source configuration via YAML
The system SHALL load content source definitions from a `sources.yml` file using Spring's `@ConfigurationProperties`. Each source entry SHALL have the fields: `id` (unique string), `type` (one of `rss`, `website`), `url`, `pollIntervalMinutes` (default: 60), and `enabled` (default: true).

#### Scenario: Valid source configuration loaded at startup
- **WHEN** the application starts with a valid `sources.yml` containing two RSS sources and one website source
- **THEN** all three sources are available as `SourceProperties` beans with their configured values

#### Scenario: Source with default values
- **WHEN** a source entry omits `pollIntervalMinutes` and `enabled`
- **THEN** the source defaults to a 60-minute poll interval and enabled = true

#### Scenario: Disabled source is loaded but not active
- **WHEN** a source entry has `enabled: false`
- **THEN** the source is loaded into configuration but excluded from polling

### Requirement: Topic configuration
The system SHALL read a `topic` string from `sources.yml` that defines the user's area of interest. This topic is used as context for LLM relevance filtering.

#### Scenario: Topic loaded from configuration
- **WHEN** the application starts with `sources.yml` containing `topic: "AI engineering, LLM applications"`
- **THEN** the topic string is available for injection into LLM prompts

### Requirement: Application configuration via application.yml
The system SHALL read application settings from `application.yml` including: OpenRouter API key and base URL (via Spring AI properties), OpenAI TTS API key, TTS voice and model, briefing cron schedule, briefing target word count, episode storage directory, episode retention days, and podcast feed metadata (base URL, title).

#### Scenario: API keys from environment variables
- **WHEN** `application.yml` references `${OPENROUTER_API_KEY}` and `${OPENAI_API_KEY}`
- **THEN** the system resolves these from environment variables at startup

#### Scenario: Briefing schedule configuration
- **WHEN** `application.yml` contains `app.briefing.cron: "0 0 6 * * *"`
- **THEN** the briefing generation pipeline runs daily at 6 AM
