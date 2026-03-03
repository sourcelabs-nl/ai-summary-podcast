## 1. Backend: Add label to create/update DTOs

- [x] 1.1 Add `label: String? = null` field to `CreateSourceRequest` and `UpdateSourceRequest` in `SourceController.kt`
- [x] 1.2 Pass `label` from request through to `SourceService.create()` and `SourceService.update()` — add `label` parameter to both service methods
- [x] 1.3 Set `label` on the `Source` entity in `create()` and update it in `update()` via the `copy()` call

## 2. Frontend: Types and component setup

- [x] 2.1 Add `Source` interface to `frontend/src/lib/types.ts` with fields: id, podcastId, type, url, pollIntervalMinutes, enabled, label, createdAt
- [x] 2.2 Add shadcn/ui components if not already present: `alert-dialog`, `switch`, `input`, `label`
- [x] 2.3 Create `frontend/src/components/sources-tab.tsx` with a `SourcesTab` component that accepts `userId` and `podcastId` props, fetches sources from the API, and renders a table

## 3. Frontend: Source list table

- [x] 3.1 Implement the sources table with columns: Label (fallback to URL), Type (badge), Poll Interval, Enabled, Actions (edit/delete icon buttons)
- [x] 3.2 Show empty state when no sources exist

## 4. Frontend: Add/Edit source dialog

- [x] 4.1 Create a source form dialog with fields: type (select), url (text), label (text, optional), poll interval (number, default 30), enabled (switch, default true)
- [x] 4.2 Wire "Add Source" button to open the dialog in create mode, submit POST to API, and refresh list
- [x] 4.3 Wire edit button on each row to open the dialog in edit mode with pre-populated values, submit PUT to API, and refresh list

## 5. Frontend: Delete source

- [x] 5.1 Wire delete button on each row to open an AlertDialog with a warning about cascading article deletion, confirm calls DELETE API and refreshes list

## 6. Frontend: Integrate Sources tab

- [x] 6.1 Add "Sources" tab to the podcast detail page alongside Episodes and Publications, rendering the `SourcesTab` component

## 7. Verification

- [x] 7.1 Restart backend and verify add/edit/delete sources works end-to-end in the browser
