## 1. Setup

- [x] 1.1 Add the shadcn/ui `dropdown-menu` component (`npx shadcn@latest add dropdown-menu`)

## 2. Implementation

- [x] 2.1 Replace the standalone `<Select>` filter with a `<DropdownMenu>` inside the Status `<TableHead>` cell — use `DropdownMenuCheckboxItem` for each status option plus "All statuses", wired to existing `statusFilter` state and `setStatusFilter` handler
- [x] 2.2 Remove the wrapper `<div className="mb-4 mt-4">` that contained the old select component

## 3. Verification

- [x] 3.1 Visually verify the dropdown opens from the Status header, shows check marks, and filters episodes correctly
