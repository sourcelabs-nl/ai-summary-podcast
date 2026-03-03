## Context

The frontend currently shows episodes in a flat table with actions opening modals (script viewer) or inline tabs (publications). There is no way to see which articles were used to generate an episode. The backend already tracks episode-article links in the `episode_articles` junction table, but no API exposes this data. Sources lack a human-readable label — they only have a UUID `id` and a `url`.

Episodes can contain anywhere from 2 to 100+ articles, ruling out inline expansion or side panels.

## Goals / Non-Goals

**Goals:**
- Provide a dedicated episode detail page consolidating script, articles, and publications
- Expose episode-article data through a new API endpoint
- Add human-readable labels to sources for better display
- Replace the script viewer modal with the detail page's Script tab

**Non-Goals:**
- Article-level actions (exclude/re-include articles, regenerate)
- Full-text article body display (only title, summary, metadata)
- Source label auto-generation from URLs (manual label for now)
- Search or filtering within the articles list

## Decisions

### 1. Page route: `/podcasts/[podcastId]/episodes/[episodeId]`

Follows the existing Next.js App Router pattern. The podcast detail page at `/podcasts/[podcastId]` already exists; this adds a nested dynamic segment.

**Alternative considered:** Query parameter (`?episode=42`) — rejected because it doesn't give a clean shareable URL and complicates routing.

### 2. Tabbed layout with three tabs: Script, Articles, Publications

Consolidates all episode-related views in one place. Script tab replaces the modal dialog. Publications tab moves from the podcast-level page to here (it's always per-episode anyway). Articles tab is new.

**Alternative considered:** Single scrollable page with sections — rejected because 100+ articles would make the page very long and bury the script/publications sections.

### 3. Articles loaded on demand (lazy tab)

The Articles tab fetches data only when activated. With potentially 100+ articles per episode, eager loading would slow down the initial page render for no benefit if the user only wants to see the script.

### 4. Articles grouped by source, collapsible, sorted by relevance

Grouping by source gives structure to large article lists. Collapsible sections prevent overwhelming the user. Within each group, sorting by relevance score (descending) surfaces the most important articles first.

### 5. New endpoint: `GET /api/users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/articles`

Returns articles with nested source metadata. Keeps the existing episode endpoint unchanged (no breaking change). The response includes source `id`, `type`, `url`, and `label` for grouping.

**Alternative considered:** Embedding articles in the episode response — rejected because it would bloat the episode list endpoint and force eager loading.

### 6. Source label: nullable `label` column on `sources` table

A simple nullable TEXT column. When `label` is NULL, the frontend derives a display name from the URL (e.g., extract domain or path). This avoids a mandatory backfill migration while still allowing clean labels.

### 7. Episode rows become clickable links

The entire episode row in the episodes table becomes a `<Link>` to the detail page. The existing action buttons (Approve, Discard, Publish) remain as click-through actions on the table — they don't navigate. The "Script" button is removed since clicking the row now takes you to the detail page where the script is the default tab.

## Risks / Trade-offs

- **Large article lists (100+ items)**: Collapsible groups mitigate this, but very large episodes may still feel heavy. → Pagination could be added later if needed.
- **Source labels require manual entry**: No auto-generation from URLs. → Acceptable for now; sources are configured manually anyway. The frontend falls back to URL-derived display names.
- **Removing script modal changes existing UX**: Users currently click "Script" for a quick look. → The detail page loads fast and script is the default tab, so the extra navigation is minimal.
