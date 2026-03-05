## MODIFIED Requirements

### Requirement: Source list table
The `SourcesTab` component SHALL fetch sources from `GET /api/users/{userId}/podcasts/{podcastId}/sources` and display them in a table with columns: Label (display label, falling back to URL if null), Type (source type badge), Poll Interval (formatted as minutes), Enabled (visual indicator), Articles (total count with relevance percentage), and Actions (edit and delete buttons).

#### Scenario: Display sources
- **WHEN** the Sources tab loads and the podcast has sources
- **THEN** all sources are displayed in a table with the specified columns

#### Scenario: No sources
- **WHEN** the Sources tab loads and the podcast has no sources
- **THEN** an empty state message is displayed

#### Scenario: Source label fallback
- **WHEN** a source has a null label
- **THEN** the URL is displayed in the Label column instead

#### Scenario: Articles column with relevance percentage
- **WHEN** a source has 42 articles and 18 are relevant
- **THEN** the Articles column displays "42 (43% relevant)"

#### Scenario: Articles column with zero articles
- **WHEN** a source has 0 articles
- **THEN** the Articles column displays "0"

#### Scenario: Articles column with zero relevant
- **WHEN** a source has articles but none are relevant
- **THEN** the Articles column displays "42 (0% relevant)"
