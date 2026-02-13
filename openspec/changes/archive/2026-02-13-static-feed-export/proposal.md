## Why

The podcast feed is currently only available via the dynamic HTTP endpoint (`/users/{userId}/podcasts/{podcastId}/feed.xml`). To support hosting on a static file server (e.g., S3, Nginx, GitHub Pages), the system should also write a `feed.xml` file to the podcast's output directory alongside the MP3 files. This enables self-hosting without running the application server.

## What Changes

- After any event that changes the feed (episode generated, episode approved, episode cleaned up), write a `feed.xml` file to the podcast's episode directory (`data/episodes/{podcastId}/feed.xml`).
- The static feed uses the same RSS 2.0 generation logic as the dynamic endpoint.
- The existing dynamic HTTP endpoint remains unchanged.

## Capabilities

### New Capabilities
- `static-feed-export`: Write a `feed.xml` file to the podcast's output directory whenever the feed content changes, enabling static hosting of the entire podcast directory.

### Modified Capabilities
_(none — the existing `podcast-feed` spec is unaffected; the dynamic endpoint keeps working as-is)_

## Impact

- **Code**: `FeedGenerator` or a new component writes the XML to disk after episode lifecycle events (generation, approval, cleanup).
- **Filesystem**: A new `feed.xml` file appears in each podcast's episode directory (`data/episodes/{podcastId}/feed.xml`).
- **Dependencies**: No new dependencies — reuses ROME and existing `FeedGenerator`.
- **APIs**: No API changes; the dynamic feed endpoint is unchanged.