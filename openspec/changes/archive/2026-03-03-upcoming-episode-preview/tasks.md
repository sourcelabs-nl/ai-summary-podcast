## Tasks

### Backend

- [x] Add `preview()` method to LlmPipeline that scores unscored articles and composes script but does NOT mark articles as processed or create an episode
- [x] Add `previewBriefing()` method to PodcastService that calls the new pipeline preview method
- [x] Add `GET /users/{userId}/podcasts/{podcastId}/upcoming-articles` endpoint to PodcastController returning relevant unprocessed articles with source info
- [x] Add `POST /users/{userId}/podcasts/{podcastId}/preview` endpoint to PodcastController returning dry-run script and articles
- [x] Update `POST /generate` response to always include `episodeId` in the response (needed for frontend navigation)

### Frontend

- [x] Add `UpcomingArticle` and `PreviewResponse` TypeScript types
- [x] Add upcoming articles count fetch to podcast detail page and render "Next Episode" row above episodes table (only when articles > 0)
- [x] Create upcoming content page at `/podcasts/[podcastId]/upcoming` showing articles grouped by source with Preview Script and Generate Episode buttons
- [x] Create script preview page at `/podcasts/[podcastId]/upcoming/preview` showing dry-run script with Generate Episode button
- [x] Handle loading/error states for preview and generate actions (loading spinner on buttons, error messages)
