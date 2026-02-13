## Why

The LLM pipeline combines relevance filtering and summarization into a single LLM call, which wastes tokens summarizing irrelevant articles, prevents using different models per stage, and stores a coarse 1-5 relevance score. RSS feed content is also ingested with raw HTML markup, inflating token usage and undermining word-count-based logic.

## What Changes

- **BREAKING** — Remove `is_relevant` column from the `articles` table; replace with `relevance_score` (INTEGER, nullable, 0-10). Relevance is determined at pipeline runtime by comparing the score against a configurable threshold, not stored as a boolean.
- Split the `ArticleProcessor` into two distinct pipeline stages: (1) relevance scoring (score 0-10 + justification), (2) summarization (only for articles passing the threshold).
- Add conditional summarization: skip summarization for articles whose body is shorter than a globally configurable word count threshold (default 500). The `BriefingComposer` uses the summary when available, or the original body for short articles.
- Add per-podcast `relevance_threshold` field (INTEGER, default 5) to control how aggressive the relevance filter is.
- Add global config `app.llm.summarization-min-words` (INTEGER, default 500) to control the word count threshold below which summarization is skipped.
- Strip HTML/markup from RSS feed article bodies at ingestion time using Jsoup, so `article.body` is always clean plain text regardless of source type.

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `llm-processing`: Split combined filter+summarize into separate relevance scoring (0-10) and conditional summarization stages. BriefingComposer uses summary or body based on availability.
- `content-store`: Replace `is_relevant` (BOOLEAN) with `relevance_score` (INTEGER, nullable, 0-10) on the articles table. Remove `is_relevant` column. Add migration.
- `podcast-customization`: Add `relevance_threshold` field (INTEGER, default 5) to podcast configuration and CRUD endpoints.
- `source-polling`: Strip HTML markup from RSS feed article bodies at ingestion using Jsoup before storing.

## Impact

- **Database migration** — Column rename/type change on `articles` table (`is_relevant` → `relevance_score`). Existing articles with `is_relevant = true` could be migrated to `relevance_score = 5`, `is_relevant = false` to `relevance_score = 0`, null stays null.
- **API** — Podcast create/update endpoints accept new `relevanceThreshold` field. Podcast GET responses include the field.
- **ArticleProcessor** — Refactored into two components (relevance scorer + summarizer) or two methods with distinct prompts.
- **ArticleRepository** — Queries referencing `is_relevant` must be updated to use `relevance_score` with a threshold parameter.
- **Model resolution** — Currently two stages (`filter`, `compose`). May want a third stage name (`summarize`) or reuse `filter` model for summarization.
- **Tests** — All tests referencing `isRelevant` need updating. ArticleProcessor tests need reworking for the split stages.
