## MODIFIED Requirements

### Requirement: Episode action buttons
The episode detail page header SHALL display action buttons appropriate to the episode status: "Approve" (PENDING_REVIEW only), "Discard" (PENDING_REVIEW only), "Publish" (GENERATED and unpublished only).

#### Scenario: Pending review actions
- **WHEN** the episode has status PENDING_REVIEW
- **THEN** "Approve" and "Discard" buttons are displayed

#### Scenario: Generated episode actions
- **WHEN** the episode has status GENERATED and is not published
- **THEN** a "Publish" button is displayed
