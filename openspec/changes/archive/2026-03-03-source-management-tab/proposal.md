## Why

Sources for a podcast can only be managed via direct API calls — there is no UI. Adding a Sources tab to the podcast detail page lets users view, add, edit, and delete sources without leaving the dashboard.

## What Changes

- Add a `Source` TypeScript interface in the frontend types
- Add a "Sources" tab to the podcast detail page (`/podcasts/{podcastId}`) alongside Episodes and Publications
- Display sources in a table showing label/url, type, poll interval, and enabled status
- Add/edit sources via a dialog with fields: type, url, label, poll interval, enabled
- Delete sources with a confirmation dialog
- Add `label` field to backend `CreateSourceRequest` and `UpdateSourceRequest` so users can name sources from the UI
- Add `label` parameter to `SourceService.create()` and `SourceService.update()`

## Capabilities

### New Capabilities

- `frontend-source-management`: Frontend UI for CRUD management of podcast sources within a tab on the podcast detail page

### Modified Capabilities

- `podcast-sources`: Add `label` field to create and update request DTOs and service methods

## Impact

- **Frontend**: New `Source` type, new `SourcesTab` component, modified podcast detail page (new tab)
- **Backend**: `SourceController.kt` — add `label` to `CreateSourceRequest`/`UpdateSourceRequest`; `SourceService.kt` — add `label` parameter to `create()` and `update()` methods
- No database changes (label column already exists)
- No new external dependencies
