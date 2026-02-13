## Context

The podcast pipeline fetches articles from RSS feeds and websites, strips HTML to plain text, scores relevance, optionally summarizes, and composes a briefing script. Attribution is currently limited to domain-level source names (e.g., `[techcrunch.com]`) extracted from article URLs at composition time. Author information available in RSS feed metadata and website meta tags is never captured. The summarization prompt does not instruct the LLM to preserve attribution, so "who said what" can be lost during condensation.

## Goals / Non-Goals

**Goals:**
- Capture article author metadata from RSS feeds and website HTML during ingestion.
- Preserve attribution to people, organizations, and studies during LLM summarization.
- Include author information in the briefing composition context so the LLM can naturally credit original authors.

**Non-Goals:**
- Smarter HTML-to-text conversion (preserving blockquotes, link structure, etc.) — deferred unless A+B+C proves insufficient.
- Extracting authors cited *within* an article body (e.g., "researchers at MIT") — that relies on the LLM interpreting the text, which lever B (prompt improvement) already addresses.
- Changing the Article content hash or deduplication logic.

## Decisions

### Decision 1: Add nullable `author` field to `Article`

Add `author: String?` to the `Article` data class and a corresponding `author TEXT` column to the `articles` table via Flyway migration V12. The field is nullable because not all sources provide author information.

**Alternatives considered:**
- Separate `article_metadata` table — over-engineered for a single field. If we need more metadata fields later, we can add them to `Article` directly.
- Storing author on the `Source` entity — incorrect granularity, as different articles from the same source have different authors.

### Decision 2: Extract author from RSS `SyndEntry.author`

Rome's `SyndEntry` exposes `.author` (a string) and `.authors` (a list of `SyndPerson`). We use `.author` as-is since it's the most commonly populated field. If `.author` is blank, fall back to the first entry in `.authors` (using `SyndPerson.name`). If neither is available, the field remains null.

**Alternatives considered:**
- Parsing all authors into a comma-separated string — adds complexity for marginal gain. Most RSS entries have a single author.

### Decision 3: Extract author from website HTML meta tags

For website-scraped articles, extract the author from the Jsoup `Document` by checking (in order):
1. `<meta name="author" content="...">`
2. `<meta property="article:author" content="...">`

Stop at the first non-blank match. This covers the two most common author meta tag conventions. The extraction is done in `WebsiteFetcher` since it already has access to the Jsoup `Document`.

**Alternatives considered:**
- JSON-LD structured data parsing — would cover more sites but requires JSON parsing of `<script type="application/ld+json">` blocks. Adds complexity. Can be added later if meta tags prove insufficient.
- Byline element extraction (e.g., `.author`, `.byline` CSS classes) — too fragile and site-specific.

### Decision 4: Update summarization prompt to preserve attribution

Add an instruction to the summarization prompt: "If the article attributes information to a specific person, organization, or study, preserve that attribution in your summary."

This is a prompt-only change — no structural changes to the summarization pipeline.

### Decision 5: Include author in composition summary block

Change the summary block format from:
```
1. [techcrunch.com] Article Title
Summary text...
```
to:
```
1. [techcrunch.com, by John Smith] Article Title
Summary text...
```

Only include the "by Author" part when `article.author` is non-null. Update the composition prompt attribution instruction to mention that author names may be available and can be used naturally.

## Risks / Trade-offs

- **RSS author field quality varies** — Some feeds leave `.author` blank, some put email addresses, some use display names. We store whatever is provided. The LLM is good at interpreting noisy author strings naturally. → Mitigation: The field is nullable, and the composition prompt uses "when available" language.
- **Website meta tag coverage** — Not all websites have author meta tags. → Mitigation: This is additive — we still have domain-level attribution as a fallback. JSON-LD extraction can be added later if needed.
- **Prompt changes may affect output style** — Asking the LLM to preserve attribution could make summaries slightly longer or change the briefing tone. → Mitigation: The instructions use soft language ("when available", "naturally credit") to avoid over-citation.

## Migration Plan

1. Add Flyway migration `V12__add_article_author.sql`: `ALTER TABLE articles ADD COLUMN author TEXT;`
2. Existing articles will have `author = NULL` — no backfill needed.
3. New articles ingested after deployment will have author populated when available.
4. Rollback: Drop the `author` column (though nullable columns can simply be ignored).
