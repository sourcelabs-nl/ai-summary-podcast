## Why

Even with cross-source content hash dedup and forward-only ingestion (from `content-overlap-safeguards`), a new source can still produce articles that semantically overlap with content already covered in published episodes. For example, two different RSS feeds may cover the same news event with different wording — their content hashes differ, but the topic is the same. Without overlap detection, listeners hear the same story repeated across episodes. The `episode_articles` join table (introduced in `content-overlap-safeguards`) now makes it possible to know exactly which articles went into which episode, enabling comparison of new articles against already-published content.

## What Changes

- **Overlap detection before composition**: Before composing a briefing, compare candidate articles against summaries of articles already included in recent episodes. Use the LLM (cheap filter model) to detect semantic overlap and exclude articles that substantially repeat previously published content.
- **Overlap logging**: Log which articles were excluded due to overlap and which episode they overlapped with, for operator visibility.

## Capabilities

### New Capabilities
- `historical-overlap-detection`: Detection and exclusion of articles that semantically overlap with content already included in recent episodes, using LLM-based comparison.

### Modified Capabilities
- `llm-processing`: The pipeline gains an overlap detection step between scoring and composition that filters out articles overlapping with recent episodes.

## Impact

- **LLM pipeline**: New step between scoring and composition. Additional LLM calls using the filter model (cheap).
- **Cost**: One additional LLM call per pipeline run when there are both candidate articles and recent episode articles. The call uses the cheap filter model and is bounded by a configurable episode lookback window.
- **Database queries**: Queries `episode_articles` + `articles` to fetch summaries of recently published content. Requires `episode-article-tracking` from `content-overlap-safeguards` to be implemented first.
