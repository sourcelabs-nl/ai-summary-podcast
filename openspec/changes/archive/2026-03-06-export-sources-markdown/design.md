## Context

The Sources tab in `sources-tab.tsx` already loads all source data from the API into component state. Users want to export this list as a shareable markdown file.

## Goals / Non-Goals

**Goals:**
- Add a download button to export sources as a `.md` file
- Group sources by type for readability
- Include label, URL, and poll interval

**Non-Goals:**
- Backend endpoint for export (data is already available client-side)
- Export of article-level data (only source configuration)
- Custom export format options

## Decisions

### Client-side markdown generation
Generate the markdown string in the browser from the already-loaded `sources` array and trigger a file download via `Blob` + `URL.createObjectURL`. No backend changes needed.

**Alternative**: Backend endpoint returning `text/markdown` — rejected because the data is already loaded in the frontend.

### Group by source type
Sources are grouped under type headings (RSS, Website, Twitter, YouTube) for readability. Each entry shows label (or URL if no label), URL, and poll interval. Disabled sources are included but marked.

### Download button placement
Place a "Download" icon button next to the existing "Add source" button in the top-right action bar. Uses the `Download` icon from lucide-react.

## Risks / Trade-offs

- [Large source lists] → Unlikely to be an issue; source counts are small (tens, not thousands)
- [Disabled sources included] → Users may want only enabled sources, but including all with a marker is more complete and simpler
