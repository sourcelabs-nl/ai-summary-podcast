## ADDED Requirements

### Requirement: Episode sources.md generation
The system SHALL generate a standalone `sources.md` markdown file for each episode during the episode creation pipeline. The file SHALL be stored at `data/episodes/{podcastId}/{slug}-sources.md` where `slug` is derived from the podcast name and episode date (lowercased, non-alphanumeric replaced with hyphens, e.g., `tech-news-2026-03-09`).

The file SHALL contain:
1. A heading with the podcast name
2. The episode date
3. The episode recap (summary)
4. A "Sources" heading followed by a formatted list of articles with title and URL

Example format:
```markdown
# Tech News

**Episode: 2026-03-09**

Java 26 is set to release on March 17th, introducing significant features like HTTP/3 support...

## Sources

- [No Keys, No LLM: Building a Wikidata Definition API](https://foojay.io/today/embabel-spring-boot-wikidata-definition-api/)
- [Java Performance Update: From JDK 21 to JDK 25](https://inside.java/2026/03/08/jfokus-java-performance-update/)
```

#### Scenario: Sources file generated during pipeline
- **WHEN** an episode is created with a recap and 5 linked articles
- **THEN** a `sources.md` file is generated at `data/episodes/{podcastId}/{slug}-sources.md` containing the podcast name, date, recap, and all 5 sources

#### Scenario: Sources file generated without recap
- **WHEN** an episode is created but recap generation fails
- **THEN** the `sources.md` file is generated without the recap section, containing only the podcast name, date, and sources

#### Scenario: Sources file generated without articles
- **WHEN** an episode is created with a recap but no linked articles
- **THEN** the `sources.md` file is generated with the podcast name, date, and recap, but no sources section

#### Scenario: Article titles truncated
- **WHEN** an article title exceeds 100 characters
- **THEN** the title is truncated to 100 characters with "..." appended in the sources.md

### Requirement: Generate sources.md for existing episodes
The system SHALL provide a mechanism (startup task or admin endpoint) to regenerate `sources.md` files for all existing episodes that have linked articles. This ensures existing episodes have sources files available for FTP upload.

#### Scenario: Regenerate for existing episodes
- **WHEN** the regeneration is triggered
- **THEN** all existing GENERATED episodes with linked articles get a `sources.md` file created in their podcast's data directory
