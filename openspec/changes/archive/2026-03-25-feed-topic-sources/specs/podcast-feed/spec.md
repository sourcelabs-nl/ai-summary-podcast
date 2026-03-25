## MODIFIED Requirements

### Requirement: Episode description uses recap only with sources link
Episode `<description>` in the RSS feed SHALL contain only the show notes or recap text (no sources link, no inline source listings). The sources are available in `content:encoded` and via the sources HTML page link.

#### Scenario: Description with show notes
- **WHEN** the feed is generated for an episode with show notes
- **THEN** the episode `<description>` contains only the show notes text

#### Scenario: Description without show notes but with recap
- **WHEN** the feed is generated for an episode without show notes but with a recap
- **THEN** the episode `<description>` contains only the recap text

#### Scenario: Description without recap or show notes
- **WHEN** the feed is generated for an episode without a recap or show notes
- **THEN** the episode `<description>` falls back to the first 500 characters of the script text + "..."

## ADDED Requirements

### Requirement: Feed content:encoded shows topic-representative sources
Each episode item in the RSS feed SHALL include a `<content:encoded>` element with HTML content. The HTML SHALL contain:

1. The show notes (or recap, or script fallback) formatted as `<p>` paragraphs
2. A "Topics covered:" section listing one representative article per distinct dedup topic, where the topic label is used as the clickable link text (not the article title)
3. A sentence linking to the full sources page: "For the full list of sources that inspired this episode, [view all sources and show notes](url)."
4. A `<hr/>` separator followed by a contact footer: "Tips, comments, or feedback? Mail us at [email](mailto:email)" using the configured `ownerEmail`

Articles SHALL be grouped by their stored `topic` label from the `episode_articles` table, and the first article per topic (ordered by relevance score descending) SHALL be selected as representative. The topic label SHALL be used as the link text, linking to the representative article's URL.

When an episode has no topic data (e.g., pre-migration episodes with `NULL` topic values), all articles SHALL be shown with article titles as link text under a "Sources:" header instead.

When no `ownerEmail` is configured, the contact footer SHALL be omitted.

#### Scenario: Content encoded with topic-grouped sources
- **WHEN** the feed is generated for an episode with 15 articles across 5 distinct topics
- **THEN** the `content:encoded` contains "Topics covered:" with 5 clickable topic names, each linking to the highest-relevance article for that topic

#### Scenario: Content encoded with null topics (legacy episode)
- **WHEN** the feed is generated for an episode where all articles have `NULL` topic values
- **THEN** the `content:encoded` contains "Sources:" with all 15 article titles as links (no grouping applied)

#### Scenario: Content encoded with no articles
- **WHEN** the feed is generated for an episode with no linked articles
- **THEN** the `content:encoded` contains the show notes, the full sources link, and the contact footer, but no topic/sources list

#### Scenario: Content encoded contact footer
- **WHEN** the feed is generated with `ownerEmail` configured as "podcast@example.com"
- **THEN** the `content:encoded` HTML ends with a `mailto:` link to "podcast@example.com"

#### Scenario: Content encoded without ownerEmail
- **WHEN** the feed is generated without `ownerEmail` configured
- **THEN** the `content:encoded` HTML omits the contact footer

### Requirement: Feed article query includes topic
The feed generator's article query SHALL return the `topic` column from `episode_articles` alongside each article's title and URL. The query SHALL order results by relevance score descending (highest first) so that when grouping by topic, the first article per group is the most relevant.

#### Scenario: Query returns topic for articles
- **WHEN** articles are queried for an episode with topic data
- **THEN** each article result includes its `topic` label

#### Scenario: Query returns null topic for legacy articles
- **WHEN** articles are queried for a pre-migration episode
- **THEN** each article result has a `NULL` topic value
