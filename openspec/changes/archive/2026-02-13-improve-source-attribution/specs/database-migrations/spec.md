## ADDED Requirements

### Requirement: V12 migration adds author column to articles
The system SHALL include a migration file `V12__add_article_author.sql` that adds an `author` column (TEXT, nullable) to the `articles` table. This column stores the article author's name as extracted from RSS feed metadata or website HTML meta tags.

#### Scenario: V12 migration adds author column
- **WHEN** Flyway applies `V12__add_article_author.sql`
- **THEN** the `articles` table has an `author` column of type TEXT, nullable

#### Scenario: Existing articles have null author after migration
- **WHEN** V12 is applied to a database with existing articles
- **THEN** those articles have `author = NULL`
