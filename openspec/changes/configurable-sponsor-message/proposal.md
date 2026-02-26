## Why

The sponsor message is currently hardcoded in the three composer prompts (BriefingComposer, DialogueComposer, InterviewComposer). This means every podcast gets the same sponsor, and changing it requires a code change. Moving it to the podcast configuration makes sponsorship per-podcast and user-manageable via the API.

## What Changes

- Add a `sponsor` JSON field (nullable `Map<String, String>`) to the `Podcast` entity, stored as a JSON column in SQLite
- Add a Flyway migration for the new column
- Update all three composers to conditionally inject sponsor instructions from `podcast.sponsor` instead of hardcoded text
- Expose `sponsor` in podcast create/update/get API endpoints
- When `sponsor` is null, no sponsor lines appear in the prompt

## Capabilities

### New Capabilities

(none — this extends existing capabilities)

### Modified Capabilities

- `podcast-customization`: Add sponsor field as a new per-podcast customization option (JSON map with `name` and `message` keys)
- `llm-processing`: Composers read sponsor from podcast config instead of hardcoded values; omit sponsor instructions when not configured

## Impact

- **Database**: New nullable `sponsor` TEXT column on `podcasts` table (Flyway V10)
- **Entity**: `Podcast` data class gets `sponsor: Map<String, String>? = null`
- **API**: `CreatePodcastRequest`, `UpdatePodcastRequest`, `PodcastResponse` gain `sponsor` field
- **Composers**: `BriefingComposer`, `DialogueComposer`, `InterviewComposer` — sponsor prompt lines become conditional
- **Tests**: Composer tests and controller tests updated
