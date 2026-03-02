## Context

The frontend dashboard at `frontend/` uses Next.js App Router, shadcn/ui, and Tailwind CSS v4. The podcast detail page at `/podcasts/[podcastId]` currently has two tabs (Episodes, Publications) but no way to view or edit podcast configuration. The backend `PUT /users/{userId}/podcasts/{podcastId}` endpoint already supports full podcast updates. The `Podcast` TypeScript type is missing most fields returned by the API.

## Goals / Non-Goals

**Goals:**
- Provide a dedicated settings page at `/podcasts/[podcastId]/settings` to view and edit all podcast configuration
- Organize settings into logical sub-tabs: General, LLM, TTS, Content, Integrations
- Provide structured key-value row editors for JSON map fields
- Add entry points: gear icon on podcast list cards and settings link on podcast detail header

**Non-Goals:**
- Podcast creation from the UI (out of scope)
- Field-level validation beyond what the backend enforces (rely on API error responses)
- Real-time form validation or preview of changes

## Decisions

### 1. Separate page vs tab on existing page
**Decision**: Separate page at `/podcasts/[podcastId]/settings/page.tsx`

**Rationale**: The podcast detail page is already content-heavy with episodes and publications. A separate page keeps concerns clean and allows a full-width settings form. Consistent with the "entry point from multiple places" pattern.

**Alternatives**: Adding a third tab to the existing page — rejected because settings have a different interaction model (form editing vs data browsing).

### 2. Form state management
**Decision**: Use React `useState` to hold the full form state, initialized from the fetched podcast. One Save button sends the entire object via PUT.

**Rationale**: The PUT endpoint expects the full podcast object. Simple state management avoids introducing form libraries (react-hook-form, formik) for what is a single form. All sub-tabs share the same state object.

### 3. Key-value editor component
**Decision**: Create a reusable `KeyValueEditor` component that renders rows of key/value text inputs with add/remove buttons.

**Rationale**: Six JSON map fields share the same editing pattern. A single component avoids duplication. Each row has: text input for key, text input for value, remove button. Plus an "Add row" button at the bottom.

### 4. Sub-tab implementation
**Decision**: Use shadcn `Tabs` component for the sub-tabs within the settings page.

**Rationale**: Already used on the podcast detail page for Episodes/Publications. Consistent UX pattern.

### 5. Navigation entry points
**Decision**: Gear icon button (using lucide `Settings` icon) on the podcast list cards that stops event propagation (so clicking it doesn't navigate to the podcast detail page). Plus a "Settings" link/button in the podcast detail page header next to the style badge.

## Risks / Trade-offs

- **Large form with many fields** → Mitigated by sub-tabs grouping related fields. Users only see one category at a time.
- **Save sends all fields even if only one changed** → Acceptable since the PUT endpoint is idempotent and expects the full object. No risk of data loss.
- **No optimistic UI updates** → After save, re-fetch the podcast to confirm. Simple and correct.
