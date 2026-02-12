# Capability: Podcast Customization

## Purpose

Per-podcast customization of LLM model, TTS voice/speed, briefing style, target word count, custom instructions, and generation schedule.

## Requirements

### Requirement: LLM model selection per podcast
Each podcast SHALL have an optional `llm_model` field (TEXT, nullable). When set, the LLM pipeline SHALL use this model for relevance filtering, summarization, and script composition. When null, the system SHALL fall back to the global `app.llm.cheapModel` config value.

#### Scenario: Podcast with custom LLM model
- **WHEN** a podcast has `llm_model` set to `"openai/gpt-4o-mini"`
- **THEN** the LLM pipeline uses `"openai/gpt-4o-mini"` for all LLM calls for that podcast

#### Scenario: Podcast with no LLM model set
- **WHEN** a podcast has `llm_model` set to null
- **THEN** the LLM pipeline uses the global `app.llm.cheapModel` config value

### Requirement: TTS voice selection per podcast
Each podcast SHALL have a `tts_voice` field (TEXT, default `"nova"`). The TTS pipeline SHALL use this voice when generating audio for the podcast. Valid values are OpenAI TTS voices: `alloy`, `echo`, `fable`, `onyx`, `nova`, `shimmer`.

#### Scenario: Podcast with custom voice
- **WHEN** a podcast has `tts_voice` set to `"alloy"`
- **THEN** the TTS pipeline generates audio using the "alloy" voice

#### Scenario: Podcast with default voice
- **WHEN** a podcast is created without specifying `tts_voice`
- **THEN** the `tts_voice` defaults to `"nova"`

### Requirement: TTS speed selection per podcast
Each podcast SHALL have a `tts_speed` field (REAL, default `1.0`). The TTS pipeline SHALL use this speed multiplier when generating audio. Valid range is `0.25` to `4.0`.

#### Scenario: Podcast with faster speed
- **WHEN** a podcast has `tts_speed` set to `1.25`
- **THEN** the TTS pipeline generates audio at 1.25x speed

#### Scenario: Podcast with default speed
- **WHEN** a podcast is created without specifying `tts_speed`
- **THEN** the `tts_speed` defaults to `1.0`

### Requirement: Briefing style selection per podcast
Each podcast SHALL have a `style` field (TEXT, default `"news-briefing"`). The briefing composer SHALL adapt its system prompt based on the selected style. Supported styles:
- `news-briefing` — professional news anchor tone, structured with transitions
- `casual` — conversational, relaxed, like chatting with a friend
- `deep-dive` — analytical, in-depth exploration
- `executive-summary` — concise, fact-focused, minimal commentary

#### Scenario: Podcast with casual style
- **WHEN** a podcast has `style` set to `"casual"`
- **THEN** the briefing composer uses a conversational, relaxed tone in its system prompt

#### Scenario: Podcast with default style
- **WHEN** a podcast is created without specifying `style`
- **THEN** the `style` defaults to `"news-briefing"`

### Requirement: Target word count per podcast
Each podcast SHALL have an optional `target_words` field (INTEGER, nullable). When set, the briefing composer SHALL target this word count for the script. When null, the system SHALL fall back to the global `app.briefing.targetWords` config value.

#### Scenario: Podcast with custom target words
- **WHEN** a podcast has `target_words` set to `800`
- **THEN** the briefing composer targets approximately 800 words for the script

#### Scenario: Podcast with no target words set
- **WHEN** a podcast has `target_words` set to null
- **THEN** the briefing composer uses the global `app.briefing.targetWords` value (1500)

### Requirement: Custom instructions per podcast
Each podcast SHALL have an optional `custom_instructions` field (TEXT, nullable). When set, the briefing composer SHALL append these instructions to its prompt, allowing free-form customization of the output.

#### Scenario: Podcast with custom instructions
- **WHEN** a podcast has `custom_instructions` set to `"Focus on practical implications and use Dutch language"`
- **THEN** the briefing composer appends these instructions to the system prompt

#### Scenario: Podcast with no custom instructions
- **WHEN** a podcast has `custom_instructions` set to null
- **THEN** the briefing composer uses only its default prompt without additional instructions

### Requirement: Generation schedule per podcast
Each podcast SHALL have a `cron` field (TEXT, default `"0 0 6 * * *"`). The briefing generation scheduler SHALL use this cron expression to determine when to generate a new episode for the podcast. The podcast SHALL also have a `last_generated_at` field (TEXT, nullable) to track the last generation time.

#### Scenario: Podcast with custom schedule
- **WHEN** a podcast has `cron` set to `"0 0 8 * * MON-FRI"` (weekdays at 08:00)
- **THEN** the scheduler generates episodes for this podcast only on weekdays at 08:00

#### Scenario: Podcast with default schedule
- **WHEN** a podcast is created without specifying `cron`
- **THEN** the `cron` defaults to `"0 0 6 * * *"` (daily at 06:00)

#### Scenario: Scheduler respects per-podcast cron
- **WHEN** the scheduler runs and podcast A has cron `"0 0 6 * * *"` (daily 06:00) and podcast B has cron `"0 0 18 * * *"` (daily 18:00), and it is currently 06:05
- **THEN** the scheduler triggers generation for podcast A but not podcast B

#### Scenario: Scheduler tracks last generation time
- **WHEN** the scheduler successfully generates an episode for a podcast
- **THEN** the podcast's `last_generated_at` is updated to the current timestamp

### Requirement: Customization fields in podcast CRUD
All customization fields SHALL be accepted as optional fields in the podcast create (`POST`) and update (`PUT`) endpoints. The API response for a podcast SHALL include all customization fields with their effective values (stored value or default).

#### Scenario: Create podcast with customization
- **WHEN** a `POST /users/{userId}/podcasts` request includes `name`, `topic`, `ttsVoice: "alloy"`, `style: "casual"`, and `cron: "0 0 8 * * MON-FRI"`
- **THEN** the podcast is created with the specified values and defaults for unspecified fields

#### Scenario: Update podcast customization
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}` request includes `llmModel: "openai/gpt-4o"`
- **THEN** the podcast's `llm_model` is updated and other fields remain unchanged

#### Scenario: Get podcast includes customization
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}` request is received
- **THEN** the response includes all customization fields (llmModel, ttsVoice, ttsSpeed, style, targetWords, cron, customInstructions)
