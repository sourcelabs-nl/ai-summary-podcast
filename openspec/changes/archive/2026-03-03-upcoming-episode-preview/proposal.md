## Why

Users have no visibility into what content has been collected for the next episode. They can only see articles after an episode is generated. This makes it hard to know if there's enough content, decide whether to trigger manual generation, or preview what the script would look like before committing.

## What Changes

- Add a `GET /podcasts/{podcastId}/upcoming-articles` endpoint that returns relevant unprocessed articles for a podcast
- Add a `POST /podcasts/{podcastId}/preview` endpoint that performs a dry-run: scores unscored articles, composes a script, but does NOT create an episode or mark articles as processed. Returns the script and article list.
- Add a compact "Next Episode" row at the top of the Episodes tab showing the upcoming article count, linking to a dedicated upcoming content page
- Add a new frontend route `/podcasts/{podcastId}/upcoming` showing articles grouped by source with Preview Script and Generate Episode actions
- Preview navigates to a temporary preview page showing the script in chat-bubble style with a Generate Episode button
- Generate triggers the existing full pipeline and navigates to the resulting episode detail page

## Capabilities

### New Capabilities

- `upcoming-articles-api`: Backend endpoints for fetching upcoming articles and dry-run script preview
- `frontend-upcoming-episode`: Frontend pages for upcoming content preview and script dry-run

### Modified Capabilities

- `frontend-dashboard`: Add "Next Episode" row to the Episodes tab

## Impact

- Backend: new endpoints on PodcastController, new preview method on LlmPipeline/PodcastService
- Frontend: new route `/podcasts/{podcastId}/upcoming`, preview page, upcoming row component
- No database changes
