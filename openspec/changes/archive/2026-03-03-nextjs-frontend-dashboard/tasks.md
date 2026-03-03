## 1. Project Setup

- [x] 1.1 Initialize Next.js project in `frontend/` with TypeScript, Tailwind CSS, and App Router
- [x] 1.2 Install and initialize shadcn/ui with required components (Card, Button, Badge, Dialog, Select, Table)
- [x] 1.3 Configure API proxy in `next.config.js` to rewrite `/api/**` to `http://localhost:8080/**`

## 2. Layout and User Context

- [x] 2.1 Create UserContext provider to store selected userId
- [x] 2.2 Create app layout with header containing user picker dropdown (fetches from `GET /api/users`)
- [x] 2.3 Add empty state for when no users are available

## 3. Podcast Overview Page

- [x] 3.1 Create `/podcasts` page that fetches and displays podcast cards (name, style badge, topic)
- [x] 3.2 Add click navigation from podcast card to `/podcasts/[podcastId]`
- [x] 3.3 Add empty state for when no podcasts exist

## 4. Episode List Page

- [x] 4.1 Create `/podcasts/[podcastId]` page that fetches and displays episodes in a table (ID, date, status badge)
- [x] 4.2 Add status filter dropdown using `?status=` query param
- [x] 4.3 Add Approve button for PENDING_REVIEW episodes (calls `POST .../approve`, refreshes list)
- [x] 4.4 Add Discard button for PENDING_REVIEW episodes (calls `POST .../discard`, refreshes list)

## 5. Script Viewer Dialog

- [x] 5.1 Add "View Script" button on each episode row that opens a shadcn Dialog
- [x] 5.2 Implement monologue script renderer (plain text with paragraph breaks) for news-briefing, casual, deep-dive, executive-summary styles
- [x] 5.3 Implement multi-speaker script renderer (parse XML tags, color-coded labeled blocks) for dialogue and interview styles, with fallback to plain text on parse failure
