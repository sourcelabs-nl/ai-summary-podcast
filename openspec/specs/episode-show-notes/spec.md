# Capability: Episode Show Notes

## Purpose

Generates and stores show notes for each episode, providing a recap summary for use in RSS feed descriptions and API responses.

## Requirements

### Requirement: Show notes generation
The system SHALL generate show notes for each episode after the episode-article links are saved. Show notes SHALL consist of the episode recap followed by a "Sources:" section listing each linked article's title and URL. Articles SHALL be sorted by relevance score descending. Article titles longer than 100 characters SHALL be truncated with "...".

#### Scenario: Show notes with recap and articles
- **WHEN** an episode is created with a recap and 3 linked articles
- **THEN** the show_notes field contains the recap text, a blank line, "Sources:", and one line per article with "- {title}" followed by "  {url}", sorted by relevance score descending, with titles truncated to 100 characters

#### Scenario: Show notes without recap
- **WHEN** an episode is created but recap generation fails
- **THEN** the show_notes field contains only the "Sources:" section with article links

#### Scenario: Show notes with no linked articles
- **WHEN** an episode is created but has no linked articles (edge case)
- **THEN** the show_notes field contains only the recap text

### Requirement: Show notes storage
The Episode entity SHALL have a `show_notes` TEXT column to store the generated show notes.

#### Scenario: Database migration
- **WHEN** the application starts after the migration
- **THEN** the episodes table has a `show_notes` column that is nullable

### Requirement: Show notes in API response
The episode API response SHALL include a `showNotes` field.

#### Scenario: Episode response includes show notes
- **WHEN** a GET request is made for an episode that has show notes
- **THEN** the response JSON includes a `showNotes` field with the show notes text
