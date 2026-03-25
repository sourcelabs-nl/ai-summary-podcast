## Why

The podcast RSS feed currently lists all articles that were input to the script composer in the `content:encoded` element. However, the composer groups articles by topic (via the dedup filter) and discusses topics, not individual articles. This results in noisy show notes (e.g., 28 source links when only 5-8 topics are discussed). Podcast apps like Spotify display these links prominently, so showing only the topic-representative sources gives listeners a cleaner, more useful list.

## What Changes

- Persist the dedup cluster topic for each article in the `episode_articles` join table, so feed generation can determine which articles represent distinct topics
- Update the pipeline to carry topic metadata from the dedup filter result through to episode-article storage
- Update the feed generator to group articles by topic and show only one representative article per topic in `content:encoded`, with a link to the full sources page for the complete list

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `episode-article-tracking`: Add a `topic` column to the `episode_articles` table to store the dedup cluster topic label for each article. Update the save logic to persist this topic alongside the article link.
- `podcast-feed`: Update `content:encoded` in episode items to show only one article per distinct topic (the highest-relevance representative), instead of listing all articles. The full list remains accessible via the sources HTML link.

## Impact

- **Database**: New `topic` column on `episode_articles` table (migration required)
- **Pipeline**: `PipelineResult` must carry article-to-topic mapping; `LlmPipeline` builds this from `DedupResult.clusters`; `EpisodeService.saveEpisodeArticleLinks` persists the topic
- **Feed**: `FeedGenerator` and `EpisodeArticleRepositoryCustom` updated to query and group by topic
- **Existing data**: Episodes generated before this change will have `NULL` topic values; the feed generator should fall back to showing all articles when no topic data exists
