## Context

The podcast detail page has a tabbed layout with Episodes and Publications tabs. Sources for a podcast are managed exclusively via the REST API — no frontend UI exists. The backend already has full CRUD at `/users/{userId}/podcasts/{podcastId}/sources`, but the `label` field is missing from the create/update DTOs.

## Goals / Non-Goals

**Goals:**
- Add a Sources tab to the podcast detail page for basic CRUD
- Add `label` to backend create/update request DTOs and service methods
- Follow existing frontend patterns (PublicationsTab component, Dialog for forms)

**Non-Goals:**
- Source health/status display (consecutiveFailures, disabledReason)
- Advanced fields (aggregate, maxFailures, maxBackoffHours, pollDelaySeconds, categoryFilter)
- Drag-and-drop reordering or bulk operations
- Inline editing — use dialog-based add/edit

## Decisions

### Extract SourcesTab as a separate component

Follow the `PublicationsTab` pattern: a self-contained component that receives `userId` and `podcastId` as props and manages its own data fetching. This keeps the podcast detail page clean.

**Alternative considered:** Inline everything in the page — rejected for consistency and readability.

### Use Dialog for add/edit forms

Both add and edit use the same dialog component with a form containing: type (select), url (text input), label (text input), poll interval (number input), enabled (switch/checkbox). Edit mode pre-populates fields.

**Alternative considered:** Inline row editing — rejected as too complex for the number of fields.

### Delete with confirmation via AlertDialog

Deleting a source cascades to all its articles. Use an AlertDialog to warn the user before proceeding.

## Risks / Trade-offs

- [Cascade delete is destructive] → AlertDialog with clear warning about article deletion
- [No validation on URL format in frontend] → Rely on backend validation; keep frontend simple for now
