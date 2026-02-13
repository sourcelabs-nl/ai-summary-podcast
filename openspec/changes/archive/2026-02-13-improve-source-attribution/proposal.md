## Why

The generated podcast scripts lack proper attribution — listeners don't know who originally reported information or who is being quoted. When HTML is stripped during article ingestion, structural attribution context is lost. Author metadata available in RSS feeds and website meta tags is never captured. The summarization step can further erode attribution by condensing away "who said what." This undermines credibility for a podcast that reports information from external sources.

## What Changes

- Add an `author` field to the `Article` entity and `articles` database table to store author metadata extracted during ingestion.
- Extract author information from RSS feed entries (`SyndEntry.author`) and from website HTML meta tags (`<meta name="author">`, `<meta property="article:author">`).
- Update the summarization LLM prompt to instruct the model to preserve attribution to people, organizations, and studies.
- Enrich the briefing composition context by including author information alongside the source domain, and refine the composition prompt to encourage natural author attribution.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `source-polling`: Extract and store author metadata from RSS feed entries and website meta tags during ingestion.
- `content-store`: Add `author` column (text, nullable) to the `articles` table.
- `llm-processing`: Update summarization prompt to preserve attribution; include author in composition summary block and refine attribution instructions.
- `database-migrations`: Add migration for the new `author` column on the `articles` table.

## Impact

- **Database**: New nullable `author` column on `articles` table (requires Flyway migration).
- **Source polling**: `RssFeedFetcher` and `WebsiteFetcher`/`ContentExtractor` gain author extraction logic.
- **LLM prompts**: Summarization and composition prompts are updated (no API or model changes).
- **Article entity**: New `author` field added to the data class.
- **No breaking changes**: The `author` field is nullable, so existing articles and all APIs remain compatible.
