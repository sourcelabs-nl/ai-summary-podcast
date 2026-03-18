## Context

Episode descriptions in the RSS feed use `scriptText.take(500)` and SoundCloud uses `recap ?: scriptText.take(500)`. Neither includes links to the source articles that were discussed. The `episode_articles` join table already links episodes to the relevant articles used in the script, each with a `title` and `url`.

## Goals / Non-Goals

**Goals:**
- Generate show notes (recap + source links) when an episode is created
- Store show notes on the Episode entity for consistent reuse
- Use show notes in RSS feed and SoundCloud descriptions

**Non-Goals:**
- LLM-generated show notes (assemble from existing data, no new LLM calls)
- Customizable show notes format/template
- Show notes for existing episodes (only new episodes going forward)

## Decisions

### Decision 1: Store show notes as a text column on episodes

**Choice**: Add `show_notes TEXT` column to the episodes table, populated after episode-article links are saved.

**Alternatives considered**:
- Compute on the fly: Simple but duplicates logic across FeedGenerator, SoundCloudPublisher, and API responses.

**Rationale**: Single source of truth, computed once at episode creation time. Consistent across all consumers.

### Decision 2: Plain text format with line breaks

**Choice**: Show notes are plain text — recap paragraph followed by a "Sources:" section with title + URL per line.

**Alternatives considered**:
- HTML: More complex, and SoundCloud/RSS description fields have varying HTML support.
- Markdown: Neither RSS readers nor SoundCloud render Markdown.

**Rationale**: Plain text works everywhere — RSS `<description>`, SoundCloud, API responses, and is trivially renderable in the frontend.

### Decision 3: Generate show notes in EpisodeService after saving article links

**Choice**: Build show notes in `EpisodeService.createEpisodeFromPipelineResult()` after `saveEpisodeArticleLinks()`, since we need the article links to resolve titles/URLs.

**Rationale**: The article links are available at that point, and the recap is generated right after. Show notes assembly is the final step: recap + article links → show_notes → save.

## Risks / Trade-offs

- **[Trade-off] Show notes are static**: If articles are edited after episode creation, show notes won't update. Acceptable since articles are immutable after processing.
- **[Risk] Aggregated articles have generic titles**: Twitter/Nitter aggregated articles have titles like "Posts from @user — Mar 5, 2026" and `url` points to the source feed, not individual tweets. This is acceptable — it still links to the source.
