## Why

Episode descriptions in the RSS feed and SoundCloud are bare — just truncated script text or a short recap. Listeners have no way to find the original sources discussed in the episode. Adding show notes with the recap and links to source articles makes episodes more useful and discoverable.

## What Changes

- Generate show notes for each episode by assembling the recap text and linked article titles/URLs
- Store show notes on the Episode entity for reuse
- Use show notes as the episode description in the RSS feed (`<description>` tag)
- Use show notes as the SoundCloud track description when publishing
- Expose show notes in the episode API response and frontend episode detail page

## Capabilities

### New Capabilities
- `episode-show-notes`: Show notes generation from recap + linked article source links

### Modified Capabilities
- `podcast-feed`: RSS feed episode `<description>` uses show notes instead of truncated script
- `episode-review`: Episode API response includes show notes field

## Impact

- **Database**: New `show_notes` column on `episodes` table (Flyway migration)
- **Backend**: `EpisodeService.createEpisodeFromPipelineResult()` generates show notes after saving episode-article links; `FeedGenerator` and `SoundCloudPublisher` use show notes for descriptions
- **Frontend**: Episode detail page displays show notes
- **API**: Episode response includes `showNotes` field
