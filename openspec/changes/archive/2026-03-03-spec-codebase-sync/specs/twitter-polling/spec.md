## MODIFIED Requirements

### Requirement: Username-to-user-ID caching in lastSeenId
The system SHALL cache the resolved X user ID in the source's `lastSeenId` field using the format `<userId>:<sinceId>`. On subsequent polls, the system SHALL parse the cached user ID from `lastSeenId` instead of making an additional API call to resolve the username. If the source URL changes (different username), the system SHALL detect the mismatch and re-resolve. The system SHALL cache the userId even on the first poll when no tweets are returned, using the format `<userId>:` (empty sinceId).

#### Scenario: User ID cached after first resolution
- **WHEN** username `techreporter` resolves to user ID `9876543210` and the latest tweet ID is `1111111111`
- **THEN** the source's `lastSeenId` is stored as `9876543210:1111111111`

#### Scenario: User ID cached on first poll with no tweets
- **WHEN** username `techreporter` resolves to user ID `9876543210` and no tweets are returned
- **THEN** the source's `lastSeenId` is stored as `9876543210:` (userId cached, no sinceId)

#### Scenario: Cached user ID reused on subsequent poll
- **WHEN** a Twitter source has `lastSeenId` = `9876543210:1111111111`
- **THEN** the system uses user ID `9876543210` directly and passes `since_id=1111111111` to the API

#### Scenario: No new tweets returns empty list
- **WHEN** the timeline endpoint returns no tweets newer than `since_id`
- **THEN** the fetcher returns an empty list and `lastSeenId` remains unchanged
