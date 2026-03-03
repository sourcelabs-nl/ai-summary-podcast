## Why

The episode status filter is currently a standalone `<Select>` component above the episodes table, taking up vertical space and visually disconnected from the Status column it filters. Moving it into the table header makes the UI more compact and the filter-column relationship immediately obvious.

## What Changes

- Remove the standalone status filter `<Select>` component above the episodes table
- Embed a dropdown filter directly in the Status `<TableHead>` cell
- Keep the same state management (`statusFilter` state) and API filtering logic (`?status=` query param)
- Show a dropdown indicator (chevron) in the Status header to signal it's interactive

## Capabilities

### New Capabilities

_None — this is a UI layout change within the existing frontend dashboard._

### Modified Capabilities

- `frontend-dashboard`: The episode list status filter moves from a standalone select into the table header column

## Impact

- **Frontend only**: `frontend/src/app/podcasts/[podcastId]/page.tsx` — layout change in the episodes tab
- No backend changes
- No API changes
- No new dependencies