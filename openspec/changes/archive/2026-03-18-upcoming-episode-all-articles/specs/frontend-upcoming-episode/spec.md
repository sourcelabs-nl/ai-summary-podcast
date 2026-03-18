## MODIFIED Requirements

### Requirement: Upcoming content page with tabbed layout
The system SHALL provide a page at `/podcasts/[podcastId]/upcoming` that uses a tabbed layout with Articles and Script tabs, matching the episode detail page pattern.

#### Scenario: Page header
- **WHEN** user navigates to the upcoming content page
- **THEN** the page displays a back link "Back to Episodes", a header "Upcoming Episode" with article count, source count, and next scheduled generation time (parsed from the podcast's cron expression), and a "Generate Episode" button

#### Scenario: Next generation schedule
- **WHEN** the podcast has a cron expression configured
- **THEN** the header subtitle shows the next generation date and time (e.g., "Will be generated Wed, Mar 4 at 06:00 AM")

#### Scenario: No articles
- **WHEN** the page loads and there are no upcoming articles or posts
- **THEN** the Articles tab displays a message indicating no content has been collected yet

## ADDED Requirements

### Requirement: Always-visible upcoming episode link
The podcast detail page SHALL always display the "Next Episode" navigation link, regardless of whether articles are available.

#### Scenario: Articles available
- **WHEN** the podcast detail page loads and there are upcoming articles
- **THEN** the "Next Episode" link SHALL display with the article count (e.g., "Next Episode · 5 articles ready")

#### Scenario: No articles available
- **WHEN** the podcast detail page loads and there are no upcoming articles
- **THEN** the "Next Episode" link SHALL display with the text "Next Episode · no articles yet"
