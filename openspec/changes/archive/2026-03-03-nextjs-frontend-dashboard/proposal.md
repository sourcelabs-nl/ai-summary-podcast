## Why

The podcast pipeline currently has no UI — all interaction happens via REST API calls. A web frontend would provide a visual dashboard for monitoring podcasts, reviewing episodes, and managing the approval workflow without needing to craft HTTP requests manually.

## What Changes

- Add a Next.js/React frontend in a `frontend/` directory using shadcn/ui components
- Podcast overview page showing all podcasts for a selected user
- Episode list page per podcast with status filtering and approve/discard actions
- Script viewer dialog that renders episode scripts differently based on podcast style (colored speaker labels for dialogue/interview, plain text for monologue styles)
- User picker dropdown to switch between users

## Capabilities

### New Capabilities
- `frontend-dashboard`: Next.js/React/shadcn UI frontend with podcast overview, episode management, script viewing, and approval workflow

### Modified Capabilities

_None — the backend API already supports all needed operations._

## Impact

- **New code**: `frontend/` directory with Next.js project (TypeScript, React, shadcn/ui, Tailwind CSS)
- **Dependencies**: Node.js, npm, Next.js, shadcn/ui, Tailwind CSS
- **APIs consumed**: `GET /users`, `GET /users/{userId}/podcasts`, `GET /users/{userId}/podcasts/{podcastId}/episodes`, `POST .../approve`, `POST .../discard`
- **No backend changes required** — all endpoints already exist
