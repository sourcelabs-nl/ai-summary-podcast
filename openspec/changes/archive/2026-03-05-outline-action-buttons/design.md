## Context

The frontend had action buttons with text labels (e.g., "Settings", "Approve", "Details", "Publish") that were visually heavy. The header gear icon established a minimal icon-only pattern that the user preferred. Converting all action buttons to icon-only with hover tooltips creates a consistent, clean UI.

## Goals / Non-Goals

**Goals:**
- Convert all action buttons to icon-only (`size="icon-lg"`, 40px)
- Add `title` attributes for hover tooltips on all action buttons
- Maintain visual distinction for destructive actions (red) vs normal actions (orange)

**Non-Goals:**
- No changes to dialog submit/cancel buttons (those keep text labels)
- No changes to the button component itself
- No variant changes — colors stay as they were

## Decisions

### Icon-only with title tooltip
All action buttons (Settings, Details, Publish, Approve, Add Source, Edit, Republish, Add Provider) become icon-only with `title` for hover text. Dialog buttons (Save, Cancel, Submit) keep their text labels since dialogs need clear primary actions.

### Button size: icon-lg (40px)
Used `size="icon-lg"` (40px / `size-10`) for comfortable click targets. Smaller sizes (`icon-sm` at 32px, `icon` at 36px) were too small.

### Title text convention
Concise, action-oriented text: "Settings", "Approve episode", "Publish episode", "View details", "Add source", etc.

## Risks / Trade-offs

- [Icon-only less discoverable for new users] → Mitigated by `title` tooltips on hover. Icons are standard (gear, check, x, upload, plus, pencil, trash).
