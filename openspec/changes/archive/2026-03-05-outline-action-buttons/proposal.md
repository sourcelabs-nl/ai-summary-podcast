## Why

Action buttons across the frontend used text labels (e.g., "Settings", "Approve", "Details") which were visually heavy. Converting to icon-only buttons with hover tooltips creates a cleaner, more minimal UI consistent with the header gear icon style.

## What Changes

- Convert all action buttons from text+icon to icon-only using `size="icon-lg"` (40px)
- Add `title` attributes to all action buttons for hover alt text
- Buttons keep their existing color variants (orange `default` for actions, `destructive` for delete/discard)
- Header gear icon also gets a `title` attribute

## Capabilities

### New Capabilities

### Modified Capabilities
- `frontend-dashboard`: Convert action buttons to icon-only with title tooltips across episode list, podcast overview, episode detail, sources tab, and publications tab
- `frontend-user-preferences`: Convert Add Provider button to icon-only and add title attributes to settings page action buttons and header gear icon

## Impact

- Frontend only: button size/content changes across multiple components
- No backend changes
