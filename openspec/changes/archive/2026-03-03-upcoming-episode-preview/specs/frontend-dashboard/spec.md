## ADDED Requirements

### Requirement: Upcoming episode bar on podcast detail page
The podcast detail page SHALL display a highlighted bar below the header description and above the tabs, showing the count of articles ready for the next episode. The bar SHALL link to `/podcasts/{podcastId}/upcoming`.

#### Scenario: Articles available
- **WHEN** the podcast detail page loads and there are relevant unprocessed articles
- **THEN** a bar is displayed below the description: "Next Episode · N articles ready" with a chevron, linking to the upcoming content page

#### Scenario: No articles available
- **WHEN** the podcast detail page loads and there are no relevant unprocessed articles
- **THEN** the upcoming episode bar is not displayed
