## Context

The episode list page currently renders a standalone `<Select>` component (w-48, with mb-4 mt-4 wrapper) above the episodes table for status filtering. This creates vertical space between the tabs and the table and visually disconnects the filter from the Status column it controls.

## Goals / Non-Goals

**Goals:**
- Move the status filter dropdown into the Status column `<TableHead>` cell
- Remove the standalone select and its wrapper div
- Keep existing state management and API-based filtering unchanged

**Non-Goals:**
- Adding sorting to columns
- Changing the filter behavior or API calls
- Adding multi-select filtering

## Decisions

### Use a Select/DropdownMenu inside the TableHead cell

Replace the standalone `<Select>` with a `<DropdownMenu>` (from shadcn/ui) inside the Status `<TableHead>`. The trigger will be the header text "Status" with a ChevronDown icon, styled to look like a regular table header but clickable.

**Rationale:** A `<DropdownMenu>` is lighter than `<Select>` for this use case — it renders a simple menu of items without the form-control styling. It also integrates more naturally into a table header cell. The shadcn `<DropdownMenu>` component supports checkable items via `DropdownMenuCheckboxItem`, which is ideal for showing the active filter.

**Alternative considered:** Keeping `<Select>` but placing it inside the header — rejected because the Select trigger has form-field styling (border, height) that doesn't blend into a table header.

### Show active filter state via check marks

Use `DropdownMenuCheckboxItem` to show which filter is active. The header text stays as "Status" with a filter icon or chevron — no need to change the header text when filtered.

**Rationale:** Keeps the header clean and consistent. The check mark in the dropdown is sufficient to show active state.

## Risks / Trade-offs

- **Discoverability**: A standalone select is more visually obvious as a filter. Mitigation: the ChevronDown icon on the header signals interactivity, and this is a common pattern users recognize from data tables.
- **Header click area**: The entire header cell becomes clickable for the dropdown, which could conflict if sorting is added later. Mitigation: sorting is a non-goal; can be revisited if needed.
