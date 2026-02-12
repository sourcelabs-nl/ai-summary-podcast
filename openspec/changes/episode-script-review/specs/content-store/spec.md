## MODIFIED Requirements

### Requirement: Episode persistence
The system SHALL persist episodes in an `episodes` SQLite table with columns: `id` (auto-generated), `podcast_id` (TEXT, FK to podcasts, NOT NULL), `generated_at` (TEXT, NOT NULL), `script_text` (TEXT, NOT NULL), `status` (TEXT, NOT NULL, default `GENERATED`), `audio_file_path` (TEXT, nullable), and `duration_seconds` (INTEGER, nullable). The `status` column tracks the episode lifecycle (`PENDING_REVIEW`, `APPROVED`, `GENERATED`, `FAILED`, `DISCARDED`). Episodes with status `PENDING_REVIEW` or `APPROVED` SHALL have null `audio_file_path` and `duration_seconds`.

#### Scenario: Episode stored after successful generation (no review)
- **WHEN** the TTS pipeline completes and produces an MP3 file for a podcast with `requireReview = false`
- **THEN** an episode record is created with status `GENERATED`, the script text, audio file path, and calculated duration

#### Scenario: Episode stored as pending review
- **WHEN** the LLM pipeline completes for a podcast with `requireReview = true`
- **THEN** an episode record is created with status `PENDING_REVIEW`, the script text populated, and `audio_file_path` and `duration_seconds` set to null

#### Scenario: Episode status updated after TTS completion
- **WHEN** the async TTS pipeline completes for an approved episode
- **THEN** the episode's `status` is updated to `GENERATED` and `audio_file_path` and `duration_seconds` are populated

#### Scenario: Existing episodes retain GENERATED status after migration
- **WHEN** the V5 migration runs on a database with existing episodes
- **THEN** all existing episodes receive status `GENERATED` and their `audio_file_path` and `duration_seconds` values are preserved
