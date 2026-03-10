## ADDED Requirements

### Requirement: Export sources as markdown
The Sources tab SHALL display a "Download" button next to the "Add source" button. Clicking it SHALL generate a markdown file from the loaded sources and trigger a browser download. The file SHALL be named `sources.md`. Sources SHALL be grouped by type (RSS, Website, Twitter, YouTube) with each entry showing the label (or URL if no label), URL, and poll interval. Disabled sources SHALL be included with a "(disabled)" marker.

#### Scenario: Download sources markdown
- **WHEN** user clicks the "Download" button on the Sources tab
- **THEN** a `sources.md` file is downloaded containing all sources grouped by type

#### Scenario: Source entry with label
- **WHEN** a source has label "Hacker News" and URL "https://hnrss.org/frontpage" with 30m interval
- **THEN** the markdown entry reads `- [Hacker News](https://hnrss.org/frontpage) - 30m`

#### Scenario: Source entry without label
- **WHEN** a source has no label and URL "https://example.com/feed"
- **THEN** the markdown entry reads `- https://example.com/feed - 30m`

#### Scenario: Disabled source
- **WHEN** a source is disabled
- **THEN** the entry includes a "(disabled)" marker

#### Scenario: No sources
- **WHEN** the podcast has no sources
- **THEN** the download button is still visible but the generated file contains only the heading with no entries

#### Scenario: Download button placement
- **WHEN** the Sources tab renders
- **THEN** the download button appears next to the "Add source" button using the `Download` lucide icon
