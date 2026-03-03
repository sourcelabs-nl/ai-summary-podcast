## Context

The AI Summary Podcast backend exposes a full REST API for managing users, podcasts, episodes, and sources. Currently all interaction is via HTTP calls. A web frontend will provide a visual dashboard for the most common workflows: browsing podcasts, reviewing episodes, and managing the approval pipeline.

The backend runs on `localhost:8085`. The frontend will be a separate Next.js application in a `frontend/` directory within this repo, proxying API calls to the backend.

## Goals / Non-Goals

**Goals:**
- Provide a visual dashboard for podcast and episode management
- Support the episode review workflow (approve/discard) via UI buttons
- Render episode scripts with chat-bubble style formatting (monologue vs multi-speaker)
- Allow switching between users via a picker dropdown
- Consistent orange theme branding throughout the UI

**Non-Goals:**
- User authentication/authorization (MVP uses a simple user picker)
- CRUD for podcasts, sources, or users (read-only for now, except episode actions)
- Real-time updates / WebSocket integration
- Mobile-optimized responsive design (desktop-first MVP)
- Episode generation trigger from the UI
- Audio playback in the browser

## Decisions

### 1. Next.js App Router with client-side data fetching

Use Next.js App Router for routing structure, but fetch data client-side using `fetch`. No SSR/SSG needed since this is a dashboard hitting a local API.

### 2. shadcn/ui + Tailwind CSS v4 with orange theme

Use shadcn/ui for pre-built accessible components (Dialog, Button, Badge, Card, Select, Table). Tailwind CSS v4 with oklch color variables following the official shadcn theming documentation. The orange theme uses oklch values (not HSL) in the `@theme inline` directive for Tailwind v4 compatibility.

### 3. API proxy via Next.js rewrites

Configure `next.config.ts` to proxy `/api/**` requests to `http://localhost:8085/**`. This avoids CORS issues without modifying the backend.

### 4. Script rendering — chat bubble style

Parse `scriptText` client-side based on podcast `style`:
- **Monologue styles** (`news-briefing`, `casual`, `deep-dive`, `executive-summary`): each paragraph in a rounded card bubble with subtle primary-colored border/background
- **Multi-speaker styles** (`dialogue`, `interview`): alternating chat bubbles — first speaker left-aligned (primary/orange tint), second speaker right-aligned (emerald tint), with speaker name labels above each bubble and flattened corners on the speaker's side

The dialog uses `w-[90vw] !max-w-7xl` to maximize readable width.

### 5. Consistent orange branding

All badges use the `default` variant (orange primary color) for consistent styling. This includes podcast style badges and all episode status badges. Buttons use `default` (orange) for primary actions (Approve) and `destructive` (red) for destructive actions (Discard).

### 6. User context via React Context

Store the selected `userId` in a React Context provider. The user picker in the header sets this, and all API calls read from it.

### 7. Routing structure

```
/                              → redirect to /podcasts
/podcasts                      → podcast list (single-column)
/podcasts/[podcastId]          → episode table
```

Script viewing uses a shadcn Dialog (modal overlay), not a separate route.

## Risks / Trade-offs

- **[XML parsing fragility]** → LLM-generated dialogue scripts may have imperfect XML. Use a lenient regex-based parser rather than strict XML parsing. Fall back to monologue rendering on parse failure.
- **[No auth]** → Any user can act as any other user via the picker. Acceptable for a self-hosted MVP. → Future: add auth layer.
- **[API availability]** → Frontend requires the backend to be running. → Show clear error states when API is unreachable.
- **[npm version conflict]** → A stale npm v2 at `~/node_modules/.bin/npm` interferes with `create-next-app` and `shadcn` CLI. Use `/Users/soudmaijer/.nvm/versions/node/v22.16.0/bin/npm` explicitly for installs.
