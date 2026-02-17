# Capability: RSS Category Filter

## Purpose

Optional pre-ingestion filtering of RSS feed entries based on category tags, allowing sources to include only entries matching configured category terms.

## Requirements

### Requirement: RSS entry category filtering
The system SHALL support optional pre-ingestion filtering of RSS feed entries based on category tags. When a source has a `categoryFilter` configured (a comma-separated list of terms), the `RssFeedFetcher` SHALL filter entries by matching the entry's RSS categories against the filter terms. Matching SHALL be case-insensitive and use contains logic (a filter term matches if it is a substring of any category name). An entry passes the filter if at least one of its categories matches at least one filter term.

#### Scenario: Entry matches a category filter term
- **WHEN** an RSS source has `categoryFilter: "kotlin,AI"` and an entry has categories `["Kotlin", "Programming"]`
- **THEN** the entry passes the filter and is included in the fetch results

#### Scenario: Entry does not match any category filter term
- **WHEN** an RSS source has `categoryFilter: "kotlin,AI"` and an entry has categories `["Sports", "Football"]`
- **THEN** the entry is excluded from the fetch results

#### Scenario: Entry has no categories and source has a category filter
- **WHEN** an RSS source has `categoryFilter: "kotlin,AI"` and an entry has no category tags
- **THEN** the entry passes the filter (uncategorized entries are not excluded)

#### Scenario: Source has no category filter configured
- **WHEN** an RSS source has `categoryFilter: null` and entries have various categories
- **THEN** all entries pass through without category filtering

#### Scenario: Case-insensitive contains matching
- **WHEN** an RSS source has `categoryFilter: "tech"` and an entry has category `"Technology"`
- **THEN** the entry passes the filter because `"tech"` is a case-insensitive substring of `"Technology"`
