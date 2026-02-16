## Why

Individual content items (tweets, RSS entries) are currently aggregated into articles at poll time, which permanently discards the original items. This prevents reprocessing with improved prompts, loses per-item metadata (URLs, timestamps, authors), and couples poll-cycle boundaries to article boundaries. By storing individual posts first and deferring aggregation to script generation time, we gain data preservation, reprocessability, and more flexible aggregation windows.

## What Changes

- **New `posts` table**: All source pollers write individual items (tweets, RSS entries, scraped pages) to a `posts` table instead of directly to `articles`. Posts are immutable after creation.
- **New `post_articles` join table**: Links posts to the articles they were aggregated into, providing full traceability and enabling reprocessing (a post can be linked to multiple articles over time).
- **Deferred aggregation**: Aggregation moves from poll time to script generation time. For aggregated sources, unprocessed posts within a configurable time window are combined into a single article. For non-aggregated sources, each post maps 1:1 to an article.
- **Two-stage LLM pipeline**: The current three-stage pipeline (score, summarize, compose) is replaced by a two-stage pipeline: (1) score + summarize + filter irrelevant content in a single LLM call per article, (2) compose the briefing script. This reduces LLM calls and lets the model filter irrelevant posts during summarization.
- **Uniform data path**: All source types (RSS, website, Twitter, Reddit, YouTube) write to `posts` first, regardless of whether aggregation is enabled. Smart defaults per source type determine aggregation behavior (same `aggregate` flag logic as today).

## Capabilities

### New Capabilities
- `post-store`: Persistence and retrieval of individual posts, including the `posts` table schema, deduplication, and the `post_articles` join table linking posts to articles.

### Modified Capabilities
- `source-aggregation`: Aggregation moves from poll time to script generation time. Time-windowed grouping replaces per-poll-cycle grouping. The aggregator now reads from the `posts` table and writes to the `articles` table (with join table entries).
- `content-store`: Articles are no longer created during polling. The `articles` table is populated at script generation time from aggregated posts. Posts table added as the primary ingest target.
- `source-polling`: All source pollers write to the `posts` table instead of the `articles` table. The aggregator is no longer called during polling.
- `llm-processing`: Three-stage pipeline (score, summarize, compose) replaced by two-stage pipeline (score+summarize+filter, compose). The first stage scores the article, filters out irrelevant posts, and summarizes relevant content in a single LLM call. Returns structured output with relevance score, summary, and included/excluded post references.

## Impact

- **Database schema**: New `posts` and `post_articles` tables via Flyway migration. Existing `articles` table schema unchanged but population timing changes.
- **Data migration**: Existing articles have no corresponding posts (acceptable — they're already processed). New articles going forward will always have post linkage.
- **Source pollers**: All fetchers (`RssFeedFetcher`, `TwitterFetcher`, `WebsiteFetcher`) change their storage target from `ArticleRepository` to `PostRepository`.
- **SourceAggregator**: Moves from the polling pipeline to the script generation pipeline. Input changes from fetched articles to stored posts.
- **LlmPipeline**: `RelevanceScorer` and `ArticleSummarizer` merge into a single stage. `BriefingComposer` unchanged except it receives articles with richer post metadata.
- **API/RSS feed**: No external-facing changes. Episodes and podcast feeds are unaffected.