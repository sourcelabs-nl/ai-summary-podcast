## 1. Backend: Source Labels

- [x] 1.1 Add Flyway migration to add nullable `label` column to `sources` table
- [x] 1.2 Update `Source` entity to include `label` field
- [x] 1.3 Include `label` in source-related API responses

## 2. Backend: Episode Articles API

- [x] 2.1 Add `recap` field to `EpisodeResponse` mapping
- [x] 2.2 Create `EpisodeArticleResponse` DTO with article fields and nested source metadata
- [x] 2.3 Add repository method to fetch articles with source data for an episode
- [x] 2.4 Add `GET /api/users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/articles` endpoint
- [x] 2.5 Add controller test for the articles endpoint

## 3. Frontend: Episode Detail Page

- [x] 3.1 Create episode detail page route at `frontend/src/app/podcasts/[podcastId]/episodes/[episodeId]/page.tsx`
- [x] 3.2 Add API fetch function for single episode and episode articles
- [x] 3.3 Implement episode detail header (ID, date, status badge, duration, recap, action buttons)
- [x] 3.4 Implement tabbed layout with Script, Articles, and Publications tabs

## 4. Frontend: Script Tab

- [x] 4.1 Extract script rendering logic from `script-viewer.tsx` into a reusable `ScriptContent` component
- [x] 4.2 Integrate `ScriptContent` into the Script tab on the detail page

## 5. Frontend: Articles Tab

- [x] 5.1 Add TypeScript types for episode article response (with nested source)
- [x] 5.2 Implement article card component with title, relevance badge (color-coded), truncated/expandable summary, and external link
- [x] 5.3 Implement articles tab with source grouping, collapsible sections, and on-demand loading
- [x] 5.4 Implement source label fallback (derive display name from URL when label is null)

## 6. Frontend: Publications Tab & Navigation

- [x] 6.1 Integrate existing `PublicationsTab` component into the episode detail page
- [x] 6.2 Make episode rows in the episodes table clickable links to the detail page
- [x] 6.3 Remove "Script" button from episode table rows (replaced by row click → detail page)
