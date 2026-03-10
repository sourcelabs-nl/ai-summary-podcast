## Why

The Sources tab already displays all configured sources for a podcast, but there's no way to export this list. A "Download as Markdown" button lets users save and share their source configuration outside the app.

## What Changes

- Add a download button to the Sources tab that exports the sources list as a `.md` file
- Build the markdown client-side from already-loaded source data (no backend changes)
- Group sources by type with label, URL, and poll interval

## Capabilities

### New Capabilities

### Modified Capabilities
- `frontend-source-management`: Add download/export button to the Sources tab

## Impact

- **Frontend**: `sources-tab.tsx` — add download button and markdown generation helper