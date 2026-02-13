## Context

The LLM pipeline currently combines relevance filtering and summarization into a single `ArticleProcessor` call per article. This means every article — including irrelevant ones — gets summarized, wasting tokens. The relevance score is a coarse 1-5 stored as a derived boolean (`is_relevant`), and RSS feed bodies may contain raw HTML markup that inflates token usage and breaks word-count logic.

The pipeline currently flows: `ArticleProcessor` (filter+summarize) → `BriefingComposer` (script). We're restructuring this into three distinct stages with configurable thresholds.

## Goals / Non-Goals

**Goals:**
- Split relevance scoring and summarization into separate pipeline stages
- Store a granular 0-10 relevance score; determine relevance at runtime via configurable threshold
- Skip summarization for short articles (body below a configurable word count)
- Clean HTML from RSS feed bodies at ingestion time
- Keep the pipeline resilient — if scoring completes but summarization fails, scored articles are preserved

**Non-Goals:**
- Batch relevance scoring (sending multiple articles in one LLM call) — potential future optimization
- Adding a third model stage name for summarization — reuse the `filter` model for both scoring and summarization since they're both lightweight tasks
- Changing the TTS pipeline or episode generation flow
- Making the summarization word threshold per-podcast (global only for now)

## Decisions

### D1: Reuse `filter` model for both scoring and summarization

**Decision:** Both the relevance scoring and summarization stages use the model resolved for the `filter` stage.

**Rationale:** Summarization of a single article is a lightweight task similar to relevance scoring — it doesn't need the capable model reserved for briefing composition. Adding a third stage name (`summarize`) to the model resolution chain adds config complexity for minimal benefit. Users who want a different model for summarization can be supported later.

**Alternative considered:** Add a `summarize` stage to model resolution. Rejected for now — adds complexity to `StageDefaults`, `ModelResolver`, podcast `llmModels` map, and documentation, all for a stage that performs well with cheap models.

### D2: Two-pass pipeline with intermediate persistence

**Decision:** The pipeline runs in three sequential stages with persistence between each:

1. **Score** — Score all unscored articles, save `relevance_score` immediately per article
2. **Summarize** — Query articles with `relevance_score >= threshold` AND `summary IS NULL` AND `body word count >= min words`, summarize and save
3. **Compose** — Query articles with `relevance_score >= threshold` AND (`summary IS NOT NULL` OR `body word count < min words`) AND `is_processed = false`, compose briefing

**Rationale:** Intermediate persistence means a crashed pipeline doesn't lose work. Scored articles stay scored. Summarized articles stay summarized. The pipeline can resume from wherever it left off on the next run.

**Alternative considered:** In-memory pipeline passing lists between stages. Simpler code, but loses all progress on failure.

### D3: Word count on plain text, computed at pipeline time

**Decision:** Word count is computed at pipeline time using a simple `body.split("\\s+").size` on the already-cleaned body text. Not stored in the database.

**Rationale:** Word count is cheap to compute and only used for the summarization threshold decision. Storing it would require another migration and column for a derived value. If body content is always clean text (after the HTML fix), the split-on-whitespace heuristic is accurate enough.

### D4: `findUnscoredBySourceIds` replaces `findUnfilteredBySourceIds`

**Decision:** Rename the repository query to reflect the new semantics. The query changes from `is_relevant IS NULL` to `relevance_score IS NULL`.

**Rationale:** The concept shifts from "unfiltered" (binary) to "unscored" (numeric). The query is functionally the same (find articles not yet processed by stage 1) but the naming should match.

### D5: HTML stripping in `RssFeedFetcher` using Jsoup

**Decision:** Strip HTML in `RssFeedFetcher` by wrapping the raw `entry.contents` / `entry.description` value with `Jsoup.parse(value).text()`. This is safe to call on already-plain text (returns unchanged).

**Rationale:** Jsoup is already a project dependency (used by `WebsiteFetcher` / `ContentExtractor`). The fix is a single line change in `RssFeedFetcher`. No new dependencies needed.

### D6: Pipeline query strategy for summarization and composition

**Decision:** The `LlmPipeline` orchestrates all three stages using repository queries:

- Stage 1 (score): `SELECT * FROM articles WHERE relevance_score IS NULL AND source_id IN (:sourceIds)`
- Stage 2 (summarize): `SELECT * FROM articles WHERE relevance_score >= :threshold AND summary IS NULL AND source_id IN (:sourceIds)` — then filter in code for word count >= min words
- Stage 3 (compose): `SELECT * FROM articles WHERE relevance_score >= :threshold AND is_processed = 0 AND source_id IN (:sourceIds)`

The composer receives articles that may have `summary` (long articles) or `null` summary (short articles). It uses `article.summary ?: article.body` to build the prompt.

**Rationale:** Keeping word-count filtering in code (not SQL) avoids storing body length and keeps the query simple. The number of articles is small enough that in-memory filtering is fine.

## Risks / Trade-offs

**[More LLM calls for relevant articles]** — Splitting scoring and summarization means two LLM calls for relevant long articles instead of one. → Mitigated by: irrelevant articles no longer waste summarization tokens; net cost depends on relevance ratio. Short relevant articles skip summarization entirely, saving calls.

**[Migration data loss]** — Replacing `is_relevant` with `relevance_score` loses the original boolean. → Mitigated by: migration maps `true` → 5, `false` → 0, `null` → `null`. These values are reasonable defaults that preserve the intent.

**[Word count heuristic inaccuracy]** — `split("\\s+").size` is approximate. → Acceptable for a threshold decision; off-by-a-few-words doesn't matter at the 500-word boundary.

## Migration Plan

1. Add Flyway migration `V9__pipeline_rework.sql`:
   - Add `relevance_score` INTEGER column (nullable) to `articles`
   - Migrate data: `UPDATE articles SET relevance_score = 5 WHERE is_relevant = 1`
   - Migrate data: `UPDATE articles SET relevance_score = 0 WHERE is_relevant = 0`
   - Drop `is_relevant` column (SQLite requires table rebuild — use `ALTER TABLE ... DROP COLUMN` available in SQLite 3.35+)
   - Add `relevance_threshold` INTEGER column (default 5) to `podcasts`
2. Deploy — pipeline picks up new column, existing scored articles maintain their scores
3. No rollback needed for early-stage project; if needed, reverse migration restores `is_relevant` from `relevance_score`
