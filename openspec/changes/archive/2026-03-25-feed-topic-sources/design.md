## Context

The podcast RSS feed's `content:encoded` element currently lists every article from the `episode_articles` table. These articles are the output of the topic dedup filter, which clusters candidate articles by topic and selects up to 3 per cluster. While only the selected articles are persisted, the full list (often 20-30 articles across 5-10 topics) is too noisy for podcast app show notes (e.g., Spotify). The dedup cluster topic label is computed during the pipeline but discarded after composition.

## Goals / Non-Goals

**Goals:**
- Persist the dedup cluster topic label for each episode-article link
- Show only one representative article per topic in `content:encoded`
- Maintain backward compatibility for episodes without topic data

**Non-Goals:**
- Changing which articles are linked to episodes (the dedup filter selection stays the same)
- Modifying the sources HTML file (it continues to show all articles)
- Per-episode image or other feed enhancements (separate concern)

## Decisions

### 1. Add nullable `topic` column to `episode_articles`

Add a `TEXT` column `topic` to the existing `episode_articles` table via Flyway migration. The column is nullable so existing rows (pre-migration) remain valid. The feed generator treats `NULL` topic as "no grouping available" and falls back to showing all articles.

**Alternative considered:** Storing cluster data as JSON on the Episode entity. Rejected because it duplicates article references and makes querying harder.

### 2. Carry topic mapping through PipelineResult

Add a `Map<Long, String>` field `articleTopics` to `PipelineResult` mapping article ID to topic label. The `LlmPipeline` builds this from `DedupResult.clusters` by iterating selected article IDs per cluster and mapping each to the cluster's topic string.

**Alternative considered:** Passing the full `DedupResult` through the pipeline. Rejected as it leaks internal filter concerns into the episode creation layer.

### 3. Update insert to include topic

Change `EpisodeArticleRepository.insertIgnore` to accept an optional `topic` parameter. The `EpisodeService.saveEpisodeArticleLinks` method receives the topic map from `PipelineResult` and passes each article's topic when saving.

### 4. Feed query groups by topic, picks first per group

The `findArticlesByEpisodeIds` query is updated to include the `topic` column. The `FeedGenerator` groups articles by topic and picks the first article per topic (already ordered by relevance score DESC) for `content:encoded`. When topic is `NULL` (old episodes), all articles are included as before.

### 5. Content:encoded shows topic-representative articles only

The `buildHtmlDescription` method receives grouped articles. For each distinct topic, it shows only the first (highest-relevance) article. A link to the full sources page is always included at the bottom.

## Risks / Trade-offs

- **Old episodes show all articles:** Episodes generated before this change have `NULL` topics, so they fall back to the full list. This is acceptable, as new episodes get the improved behavior immediately.
- **Topic labels are LLM-generated:** They may vary in quality or style. This is acceptable since they're only used for grouping, not displayed to users.
- **Recompose path:** The `recompose` pipeline path does not run the dedup filter, so recomposed episodes will have `NULL` topics and fall back to full article lists. This is acceptable as recompose is infrequent.
