## Why

When a new source is added to a podcast, it may fetch historical content that overlaps with articles already processed in previous episodes. Additionally, different sources within the same podcast can produce identical content (e.g., a Twitter source and a Nitter RSS mirror of the same account). The current deduplication is scoped per-source (`source_id + content_hash`), so cross-source duplicates slip through and get summarized twice. There is also no episode-article link, making it impossible to know which articles contributed to which episode.

## What Changes

- **Cross-source content hash dedup**: Add a secondary dedup check at the article level that looks across all sources within the same podcast. If an article with the same `content_hash` already exists for any source in the podcast, skip it.
- **New sources look forward only**: When a source is first added (never polled), only ingest content published after the source's `created_at` timestamp. This prevents historical content from flooding in and overlapping with already-published episodes.
- **Episode-article join table**: Introduce an `episode_articles` join table to track which articles were included in each episode. This provides traceability and lays the groundwork for future historical overlap detection.

## Capabilities

### New Capabilities
- `episode-article-tracking`: Join table and repository support for linking episodes to the articles that contributed to them.

### Modified Capabilities
- `content-store`: Add the `episode_articles` join table schema and migration.
- `source-polling`: New sources (never polled) only ingest content published after the source's creation timestamp.
- `post-store`: Add cross-source content hash dedup check within the same podcast's sources.
- `llm-processing`: After composing a briefing, record the article-episode links in the `episode_articles` table.

## Impact

- **Database**: New `episode_articles` table (Flyway migration). New `created_at` column on `sources` table.
- **Source polling**: `SourcePoller` filters posts by `source.createdAt` on first poll.
- **Post dedup**: `PostRepository` gains a cross-source lookup method; `SourcePoller` adds a secondary dedup check.
- **LLM pipeline**: `LlmPipeline` or `EpisodeService` records article-episode links after episode creation.
- **Existing data**: Existing sources get `created_at` set to a past date (migration default) so they continue to ingest all content as before.