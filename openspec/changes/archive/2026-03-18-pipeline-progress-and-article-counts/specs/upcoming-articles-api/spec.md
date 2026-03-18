## MODIFIED Requirements

### Requirement: Upcoming articles response includes effective article count
The `GET /users/{userId}/podcasts/{podcastId}/upcoming-articles` endpoint SHALL return `articleCount` as the effective number of articles after pre-calculating post-to-article aggregation. Unlinked posts from sources where `shouldAggregate()` returns true and with more than one post SHALL be counted as a single article per source.

#### Scenario: Aggregatable source posts counted as one article
- **WHEN** a podcast has 10 already-aggregated articles and 20 unlinked posts from 2 aggregatable twitter sources (12 from source A, 8 from source B)
- **THEN** `articleCount` is 12 (10 existing + 1 for source A + 1 for source B)

#### Scenario: Non-aggregatable source posts counted individually
- **WHEN** a podcast has 5 already-aggregated articles and 3 unlinked posts from a non-aggregatable RSS source
- **THEN** `articleCount` is 8 (5 existing + 3 individual posts)

#### Scenario: Single unlinked post from aggregatable source counted individually
- **WHEN** a podcast has 1 unlinked post from an aggregatable twitter source
- **THEN** `articleCount` counts that post as 1 (aggregation only groups when >1 post)
