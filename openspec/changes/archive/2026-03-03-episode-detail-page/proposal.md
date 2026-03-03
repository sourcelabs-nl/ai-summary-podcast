## Why

The frontend currently has no way to see which articles contributed to an episode. The backend tracks episode-article links in the `episode_articles` table, but this data is invisible to users. Additionally, the script viewer, publications, and episode metadata are scattered across modals and tabs — there's no single place to inspect an episode in depth. Episodes can contain 5-100+ articles, making a dedicated detail page essential.

## What Changes

- Add a dedicated **episode detail page** at `/podcasts/[podcastId]/episodes/[episodeId]` with tabs for Script, Articles, and Publications
- Add a new **backend endpoint** to fetch articles linked to an episode, including their source information
- Add a `label` **field to the `sources` table** so sources have human-readable display names
- Replace the current script viewer modal — the Script tab on the detail page takes over
- Move episode-specific publications display from the podcast page to the episode detail page
- Episode rows in the episodes table become clickable, navigating to the detail page

## Capabilities

### New Capabilities
- `episode-detail-page`: Frontend episode detail page with tabbed layout (Script, Articles, Publications) and drill-down from episodes table
- `episode-articles-api`: Backend endpoint returning articles for an episode with source metadata
- `source-labels`: Human-readable label field on sources for display in the UI

### Modified Capabilities
- `frontend-dashboard`: Episode rows become clickable links to the detail page; script viewer modal is removed in favor of the detail page's Script tab; publications tab moves to episode detail

## Impact

- **Backend**: New REST endpoint, new Flyway migration for `sources.label` column, updated source entity/repository
- **Frontend**: New page route, new components (article cards, tabbed layout), modified episodes table (clickable rows), removal of script viewer dialog usage from episodes table
- **API contract**: New `GET .../episodes/{episodeId}/articles` endpoint; source responses gain a `label` field
