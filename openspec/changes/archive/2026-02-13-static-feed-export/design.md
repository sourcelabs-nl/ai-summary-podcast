## Context

The podcast feed is currently generated on-the-fly by `FeedGenerator.generate()` and served via `FeedController` at `/users/{userId}/podcasts/{podcastId}/feed.xml`. MP3 files are stored in `data/episodes/{podcastId}/` and served via Spring's static resource handler. There is no persistent `feed.xml` file on disk.

To support static hosting (e.g., syncing the `data/episodes/{podcastId}/` directory to S3 or serving it via Nginx), a `feed.xml` file needs to be written to disk whenever the feed content changes.

## Goals / Non-Goals

**Goals:**
- Write a `feed.xml` file to `data/episodes/{podcastId}/` whenever the feed content changes
- Reuse the existing `FeedGenerator` for XML generation
- Ensure the static feed stays in sync with the dynamic endpoint

**Non-Goals:**
- Replacing the dynamic HTTP feed endpoint (it stays as-is)
- Uploading/syncing files to external hosting (out of scope — users handle this themselves)
- Generating an index page or any other static assets beyond `feed.xml`

## Decisions

### 1. Introduce a `StaticFeedExporter` component

Create a new `StaticFeedExporter` component that calls `FeedGenerator.generate()` and writes the result to `data/episodes/{podcastId}/feed.xml`.

**Why not extend `FeedGenerator`?** `FeedGenerator` is a pure function (podcast + user → XML string). Writing to disk is a side effect that belongs in a separate component. This keeps `FeedGenerator` testable and reusable.

### 2. Trigger export after feed-changing events

Call `StaticFeedExporter` after each event that changes feed content:
- Episode generated (in `TtsPipeline`)
- Episode approved and TTS completed (in `EpisodeService`)
- Episode cleanup (in `EpisodeCleanup`)

**Why event-based rather than scheduled?** The feed only changes when episodes change. A scheduled job would either lag behind or run unnecessarily. Calling the exporter directly after each mutation is simple and keeps the file in sync.

### 3. Feed URLs use a configurable `staticBaseUrl`

The static feed needs enclosure URLs pointing to the static host, not the app server. Add an optional `app.feed.static-base-url` property. When set, `StaticFeedExporter` passes this to `FeedGenerator` (or performs a string replacement) so enclosure URLs use the static host. When not set, the static feed uses the same `app.feed.base-url` as the dynamic feed.

**Alternative considered**: Always use relative URLs in the static feed. Rejected because RSS 2.0 requires absolute URLs in `<enclosure>` tags.

## Risks / Trade-offs

- **Race condition on concurrent writes** → Low risk. Episode generation is sequential per podcast. If two events fire near-simultaneously, the last write wins, which is the correct state. No locking needed.
- **Stale feed if export fails** → The static file could become out of date if a write fails (e.g., disk full). Mitigation: log a warning on failure but don't fail the pipeline. The dynamic endpoint remains the source of truth.
- **Disk usage** → Negligible. A `feed.xml` file is typically a few KB.