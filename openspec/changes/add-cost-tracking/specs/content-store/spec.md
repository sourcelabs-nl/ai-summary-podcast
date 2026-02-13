## MODIFIED Requirements

### Requirement: Article persistence with deduplication
The system SHALL persist articles in an `articles` SQLite table with columns: `id` (auto-generated), `source_id` (text, foreign key), `title` (text), `body` (text), `url` (text), `published_at` (timestamp, nullable), `content_hash` (text, unique), `relevance_score` (integer, nullable), `is_processed` (boolean, default false), `summary` (text, nullable), `llm_input_tokens` (integer, nullable), `llm_output_tokens` (integer, nullable), and `llm_cost_cents` (integer, nullable). Deduplication SHALL be enforced via a unique constraint on `content_hash`. The `relevance_score` column stores a 0-10 integer score (null means unscored). Relevance is determined at pipeline runtime by comparing the score against a podcast-specific threshold, not stored as a boolean. The `llm_input_tokens` and `llm_output_tokens` columns track the total LLM tokens used for processing the article (scoring + summarization). The `llm_cost_cents` column stores the estimated cost in cents (null if pricing is not configured).

#### Scenario: New article stored successfully
- **WHEN** an article with a unique content hash is saved
- **THEN** the article is persisted with `relevance_score` = null, `summary` = null, `is_processed` = false, and all cost fields = null

#### Scenario: Duplicate article rejected
- **WHEN** an article with an already-existing content hash is saved
- **THEN** the duplicate is silently ignored and the existing record remains unchanged

#### Scenario: Article scored after relevance check
- **WHEN** the LLM relevance scorer assigns a score of 7 to an article
- **THEN** the article's `relevance_score` field is set to 7

#### Scenario: Article marked as processed after briefing generation
- **WHEN** an article's content has been included in a generated briefing
- **THEN** the article's `is_processed` field is set to true

#### Scenario: Article token counts updated after scoring
- **WHEN** an article is scored using 500 input tokens and 50 output tokens
- **THEN** the article's `llm_input_tokens` is set to 500 and `llm_output_tokens` to 50

#### Scenario: Article token counts accumulated after summarization
- **WHEN** an article already has `llm_input_tokens` = 500 and `llm_output_tokens` = 50 from scoring, and summarization uses 800 input tokens and 100 output tokens
- **THEN** the article's `llm_input_tokens` is updated to 1300 and `llm_output_tokens` to 150

### Requirement: Episode persistence
The system SHALL persist episodes in an `episodes` SQLite table with columns: `id` (auto-generated), `podcast_id` (TEXT, FK to podcasts, NOT NULL), `generated_at` (TEXT, NOT NULL), `script_text` (TEXT, NOT NULL), `status` (TEXT, NOT NULL, default `GENERATED`), `audio_file_path` (TEXT, nullable), `duration_seconds` (INTEGER, nullable), `filter_model` (TEXT, nullable), `compose_model` (TEXT, nullable), `llm_input_tokens` (INTEGER, nullable), `llm_output_tokens` (INTEGER, nullable), `llm_cost_cents` (INTEGER, nullable), `tts_characters` (INTEGER, nullable), and `tts_cost_cents` (INTEGER, nullable). The `status` column tracks the episode lifecycle (`PENDING_REVIEW`, `APPROVED`, `GENERATED`, `FAILED`, `DISCARDED`). Episodes with status `PENDING_REVIEW` or `APPROVED` SHALL have null `audio_file_path` and `duration_seconds`. The `llm_input_tokens` and `llm_output_tokens` track tokens used during the composition stage. The `tts_characters` tracks the total character count sent to the TTS API.

#### Scenario: Episode stored after successful generation (no review)
- **WHEN** the TTS pipeline completes and produces an MP3 file for a podcast with `requireReview = false`
- **THEN** an episode record is created with status `GENERATED`, the script text, audio file path, calculated duration, and cost fields populated

#### Scenario: Episode stored as pending review
- **WHEN** the LLM pipeline completes for a podcast with `requireReview = true`
- **THEN** an episode record is created with status `PENDING_REVIEW`, the script text populated, `audio_file_path` and `duration_seconds` set to null, LLM cost fields populated, and TTS cost fields null

#### Scenario: Episode TTS costs populated after audio generation
- **WHEN** the TTS pipeline completes for an approved episode
- **THEN** the episode's `tts_characters` and `tts_cost_cents` are populated alongside `audio_file_path` and `duration_seconds`

#### Scenario: Existing episodes retain null cost fields after migration
- **WHEN** the V10 migration runs on a database with existing episodes
- **THEN** all existing episodes have null values for `llm_input_tokens`, `llm_output_tokens`, `llm_cost_cents`, `tts_characters`, and `tts_cost_cents`
