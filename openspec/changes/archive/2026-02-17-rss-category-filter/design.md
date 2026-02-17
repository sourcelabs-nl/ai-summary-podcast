## Context

RSS feeds often publish across many categories. General news sites may push 50+ entries per poll, only a fraction of which are relevant. The LLM relevance filter handles this downstream, but every entry still gets ingested as a Post and costs tokens to evaluate. Adding a lightweight category pre-filter at the RSS fetcher level reduces noise before it enters the pipeline.

Rome (the RSS parsing library already in use) exposes `entry.categories` as a `List<SyndCategory>`, where each has a `.name` property. This data is currently ignored by `RssFeedFetcher`.

## Goals / Non-Goals

**Goals:**
- Allow users to configure category filter terms per RSS source
- Filter RSS entries before Post creation, reducing ingestion noise and LLM cost
- Keep the implementation minimal — single field, simple matching

**Non-Goals:**
- Exclude-list filtering (only include-list for now)
- Taxonomy URI matching (only category name matching)
- Category filtering for non-RSS source types
- Regex or wildcard pattern matching

## Decisions

### Store category filter as a comma-separated string column
**Rationale:** Consistent with how other optional source fields are stored. A simple TEXT column avoids JSON parsing complexity. The filter list is small (typically 2-5 terms) and only parsed at poll time.
**Alternative considered:** JSON array column — adds parsing overhead and complexity for no real benefit at this scale.

### Case-insensitive contains matching
**Rationale:** RSS category naming is inconsistent across feeds (`"AI"` vs `"artificial-intelligence"` vs `"Artificial Intelligence"`). Contains matching with case-insensitive comparison catches more variants than exact matching. For example, filter term `"tech"` would match categories `"Technology"`, `"tech"`, and `"Tech News"`.
**Alternative considered:** Exact case-insensitive match — too strict, misses common naming variations.

### Entries without categories pass through
**Rationale:** Some feeds don't tag entries with categories at all. Filtering these out would silently drop all content from such feeds. The safe default is to let uncategorized entries pass through, so the filter only applies when both the source has a filter AND the entry has categories.

### Filter applied in RssFeedFetcher, not SourcePoller
**Rationale:** Category data is available on the Rome `SyndEntry` object. Once mapped to a `Post`, the category information is lost. Filtering must happen before the `mapNotNull` transformation. The `categoryFilter` string is passed as a parameter to `fetch()`.

## Risks / Trade-offs

- **Overly broad contains matching** → Could match unintended categories (e.g., `"art"` matches `"Articles"`). Mitigation: users can choose specific enough filter terms. This is acceptable for a v1.
- **Feed changes category scheme** → Filter silently stops matching. Mitigation: entries without matching categories are simply not ingested — same as today's behavior before this feature existed. The LLM relevance filter downstream is the real safety net.
