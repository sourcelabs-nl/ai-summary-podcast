## MODIFIED Requirements

### Requirement: Persistent LLM response cache
The system SHALL maintain a persistent cache of LLM responses in SQLite. The cache table `llm_cache` SHALL have columns: `id` (INTEGER PRIMARY KEY AUTOINCREMENT), `prompt_hash` (TEXT, NOT NULL), `model` (TEXT, NOT NULL), `response` (TEXT), `created_at` (TEXT, ISO-8601), `input_tokens` (INTEGER, nullable), and `output_tokens` (INTEGER, nullable). A unique constraint SHALL exist on `(prompt_hash, model)`. The cache SHALL be created via a Flyway migration (V5), with token columns added in a subsequent migration (V11).

#### Scenario: Cache table exists after migration
- **WHEN** the application starts with the Flyway migrations applied
- **THEN** the `llm_cache` table exists with columns `id`, `prompt_hash`, `model`, `response`, `created_at`, `input_tokens`, and `output_tokens`, with a unique constraint on `(prompt_hash, model)`
