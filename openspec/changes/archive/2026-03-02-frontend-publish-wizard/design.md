## Context

The frontend dashboard at `frontend/src/app/podcasts/[podcastId]/page.tsx` currently shows a single episodes list with status filtering, approve/discard actions, and a script viewer. The backend already has full publishing infrastructure: `POST .../episodes/{id}/publish/{target}`, `GET .../episodes/{id}/publications`, and an `EpisodePublication` data model with status tracking. Only SoundCloud is currently implemented as a publisher target.

## Goals / Non-Goals

**Goals:**
- Surface publication data in the frontend dashboard
- Provide a guided wizard for publishing episodes
- Keep the UI simple — show what we know from the API, no backend preview endpoint

**Non-Goals:**
- No backend changes (all required API endpoints already exist)
- No editable fields in the wizard (title/description/tags are computed server-side)
- No support for adding new publisher targets from the UI
- No bulk publishing

## Decisions

### Tabbed layout on podcast detail page
Use shadcn Tabs component to split the podcast detail page into "Episodes" (existing content) and "Publications" (new). The Episodes tab remains the default active tab.

**Why tabs over separate pages:** The two views are closely related (same podcast context) and switching between them should be instant without a page navigation.

### Publish wizard as a multi-step dialog
Use a shadcn Dialog with internal step state (select target → confirm → result) rather than a separate page or a simple confirmation dialog.

**Why a wizard over a single-click action:** Publishing is a significant, somewhat irreversible action. The wizard provides a chance to review before committing, and shows the result clearly.

**Why dialog over page:** Publishing is a quick action, not a complex form. A dialog keeps the user in context.

### Publications tab shows a flat table
A simple table showing: Episode #, Target, Status (badge), External URL (link), Published date. Fetching requires iterating episode publications — we'll fetch all episodes first, then fetch publications for each.

**Alternative considered:** A single endpoint returning all publications for a podcast. This doesn't exist in the backend and we're avoiding backend changes. The N+1 fetch is acceptable since podcast episode counts are small (typically < 50).

### Hardcode SoundCloud as available target
Since SoundCloud is the only publisher, the wizard's target selection step will show SoundCloud as the only option. The wizard structure supports adding more targets later without redesign.

## Risks / Trade-offs

- **N+1 API calls for publications tab**: Fetching publications per-episode means N+1 calls. Acceptable for small episode counts. → If this becomes a problem, add a backend endpoint for podcast-level publications.
- **No preview of computed fields**: Users won't see the exact title/description/tags before publishing. → The "keep it simple" decision was deliberate. Can be added later with a backend preview endpoint.
- **Hardcoded target list**: Adding a new publisher requires a frontend change. → Acceptable for now; a backend endpoint listing available targets could be added later.
