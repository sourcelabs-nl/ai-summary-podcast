## 1. Types and Data

- [x] 1.1 Add `EpisodePublication` interface to `frontend/src/lib/types.ts`

## 2. Publish Wizard Dialog

- [x] 2.1 Create `PublishWizard` component (`frontend/src/components/publish-wizard.tsx`) with three-step dialog: target selection, confirmation, and result
- [x] 2.2 Wire up publish API call (`POST .../episodes/{id}/publish/{target}`) with loading state and error handling (including 409 conflict)

## 3. Publications Tab

- [x] 3.1 Create `PublicationsTab` component (`frontend/src/components/publications-tab.tsx`) that fetches and displays all publications for a podcast's episodes in a table
- [x] 3.2 Handle empty state and publication status badges (default variant)

## 4. Podcast Detail Page Integration

- [x] 4.1 Refactor `/podcasts/[podcastId]/page.tsx` to use shadcn Tabs with "Episodes" (default) and "Publications" tabs
- [x] 4.2 Add "Publish" button to episodes table for GENERATED episodes that opens the PublishWizard
- [x] 4.3 Refresh episodes and publications data when wizard closes
