## MODIFIED Requirements

### Requirement: Publish button hidden when fully published
The episode list publish button SHALL only be visible when the episode has NOT been published to all configured publish targets. An episode is considered fully published when it has a publication with status `PUBLISHED` for every target in the TARGETS list.

#### Scenario: Fully published episode hides publish button
- **WHEN** an episode has PUBLISHED publications for all targets (soundcloud and ftp)
- **THEN** the publish (upload) button is not rendered in the episode actions column

#### Scenario: Partially published episode shows publish button
- **WHEN** an episode has PUBLISHED status for soundcloud but no publication for ftp
- **THEN** the publish (upload) button is rendered in the episode actions column
