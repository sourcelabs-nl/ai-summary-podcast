## Why

The frontend dashboard has no visibility into episode publications. Users must use the API directly to publish episodes and cannot see what has been published, where, or the status. Adding publication visibility and a guided publish wizard to the dashboard completes the end-to-end workflow in the UI.

## What Changes

- Add a "Publications" tab to the podcast detail page (`/podcasts/{podcastId}`) alongside the existing Episodes tab, showing all publication records for the podcast's episodes
- Add a "Publish" button to the episodes table for episodes in `GENERATED` status that opens a wizard dialog
- The wizard dialog guides the user through: selecting a target provider, confirming the publish action, and showing the result (success with external link, or failure with error)
- Add TypeScript types for publication data (`EpisodePublication`)

## Capabilities

### New Capabilities
- `frontend-publish-wizard`: Frontend wizard dialog and publications tab for publishing episodes and viewing publication status

### Modified Capabilities
- `frontend-dashboard`: Add Publications tab to podcast detail page, add Publish button to episodes table

## Impact

- Frontend only — no backend changes required
- Modified files: `frontend/src/app/podcasts/[podcastId]/page.tsx`, `frontend/src/lib/types.ts`
- New components for the publish wizard dialog and publications tab
- Uses existing backend API endpoints: `POST .../publish/{target}`, `GET .../publications`
