## ADDED Requirements

### Requirement: Code review agent exists and is configured
The system SHALL have a Claude Code agent at `.claude/agents/code-reviewer.md` with the following frontmatter:
- `name: code-reviewer`
- `description` that explains when to use this agent
- `tools: Read, Glob, Grep, Bash` (read-only access)
- `disallowedTools: Edit, Write, NotebookEdit` (prevent modifications)
- `skills: [code-review, flyway-migration, jackson-migration]` (preloaded review context)
- `model: sonnet` (fast, capable enough for review)

The agent's system prompt SHALL instruct it to review Kotlin code against the rules defined in its preloaded skills and report findings in a structured format.

#### Scenario: Agent file exists with correct configuration
- **WHEN** a developer inspects `.claude/agents/code-reviewer.md`
- **THEN** the file contains valid YAML frontmatter with name, description, tools, disallowedTools, skills, and model fields as specified above, followed by a Markdown system prompt

### Requirement: Code review skill is user-invocable
The `code-review` skill at `.claude/skills/code-review/SKILL.md` SHALL have `user-invocable: true` in its frontmatter so developers can trigger it with `/code-review`.

The skill SHALL use `context: fork` and `agent: code-reviewer` to delegate execution to the code review agent.

#### Scenario: Developer triggers code review from CLI
- **WHEN** a developer runs `/code-review` in Claude Code
- **THEN** the skill forks into the `code-reviewer` agent which performs the review

#### Scenario: Developer reviews specific files
- **WHEN** a developer runs `/code-review src/main/kotlin/com/aisummarypodcast/source/`
- **THEN** the agent reviews only files in the specified path

#### Scenario: Developer reviews changed files (default)
- **WHEN** a developer runs `/code-review` with no arguments
- **THEN** the agent identifies files changed vs the main branch and reviews those files

### Requirement: Skill defines controller hygiene rules
The code-review skill SHALL define rules that flag controllers containing business logic. Input validation (checking request parameters, returning 400) is acceptable. Business logic (calculating values, orchestrating multi-step operations, directly calling repositories for mutations) is a violation.

#### Scenario: Controller delegates to service
- **WHEN** a controller method validates input, calls a service method, and maps the result to a response DTO
- **THEN** the review reports no controller hygiene violations

#### Scenario: Controller contains business logic
- **WHEN** a controller method performs multi-step operations, calls repositories directly for mutations, or contains domain calculations
- **THEN** the review flags a violation with the file path, line reference, and suggests moving the logic to a service

### Requirement: Skill defines service layer rules
The code-review skill SHALL define rules that flag duplicate logic across services, missing `@Transactional` on multi-repository operations, and services that bypass other services to access repositories they don't own.

#### Scenario: Service duplicates existing logic
- **WHEN** a service method reimplements logic that already exists in another service
- **THEN** the review flags a warning suggesting reuse of the existing service method

#### Scenario: Multi-repository operation missing @Transactional
- **WHEN** a service method calls save/delete on multiple repositories without `@Transactional`
- **THEN** the review flags a violation recommending `@Transactional`

### Requirement: Skill defines Spring Data JDBC rules
The code-review skill SHALL define rules covering:
- Entity classes must use `@Table` and `@Id` annotations
- Entities with concurrent write risk must use `@Version` for optimistic locking
- Repository interfaces must use `@Modifying` on non-SELECT queries
- Custom queries must use named parameters (`:paramName`), not positional (`?`)
- Foreign key fields must be scalar types (String/Long) referencing the parent entity's ID

#### Scenario: Entity missing @Table annotation
- **WHEN** a data class is used as a Spring Data JDBC entity but lacks `@Table`
- **THEN** the review flags a violation

#### Scenario: Repository query uses positional parameters
- **WHEN** a `@Query` annotation uses `?` instead of `:paramName`
- **THEN** the review flags a warning recommending named parameters

#### Scenario: Non-SELECT query missing @Modifying
- **WHEN** a repository method has a `@Query` with DELETE/UPDATE/INSERT but no `@Modifying`
- **THEN** the review flags a violation

### Requirement: Skill defines database consistency rules
The code-review skill SHALL define rules that check:
- Foreign key columns used in queries should have indexes
- Columns frequently used in WHERE clauses should have indexes
- Entity field names should match their corresponding database column names (snake_case in SQL, camelCase in Kotlin)

#### Scenario: Foreign key column lacks index
- **WHEN** a repository query filters by a foreign key column that has no index in any Flyway migration
- **THEN** the review flags a warning suggesting an index

### Requirement: Skill defines testing rules
The code-review skill SHALL define rules that flag use of Mockito in test files. Only MockK (`io.mockk`) and springmockk (`com.ninja_squad.springmockk`) are permitted.

#### Scenario: Test uses Mockito
- **WHEN** a test file imports from `org.mockito` or uses `@MockBean` (Spring's Mockito annotation)
- **THEN** the review flags a violation recommending MockK equivalents

### Requirement: Skill defines Jackson 3.x rules
The code-review skill SHALL define rules that flag Jackson 2.x patterns in a Spring Boot 4 project. Specifically: imports from `com.fasterxml.jackson.databind` or `com.fasterxml.jackson.core` (except `com.fasterxml.jackson.annotation` which is unchanged in 3.x).

#### Scenario: Code imports Jackson 2.x databind
- **WHEN** a Kotlin file imports from `com.fasterxml.jackson.databind`
- **THEN** the review flags a violation recommending the `tools.jackson.databind` equivalent

#### Scenario: Code uses Jackson 2.x annotations correctly
- **WHEN** a Kotlin file imports from `com.fasterxml.jackson.annotation` (e.g., `@JsonProperty`)
- **THEN** the review reports no violation (these annotations are unchanged in 3.x)

### Requirement: Structured review output format
The agent SHALL report findings grouped by category with severity levels. Each finding SHALL include file path with line reference, rule violated, and suggested fix.

Categories: Architecture, Spring Data JDBC, Database, Testing, Jackson.
Severities: violation (must fix), warning (should fix), note (consider).

#### Scenario: Review produces structured output
- **WHEN** the agent completes a review and finds issues
- **THEN** the output groups findings by category, each with severity, file:line, rule name, and suggestion

#### Scenario: Review finds no issues
- **WHEN** the agent completes a review and finds no issues
- **THEN** the output confirms the reviewed files pass all checks

### Requirement: CLAUDE.md references skill instead of inlining rules
The Architecture Guidelines section in CLAUDE.md SHALL keep a brief summary of the controller/service rules but reference the `code-review` skill as the authoritative source. The detailed rules SHALL live only in the skill.

#### Scenario: CLAUDE.md architecture section is concise
- **WHEN** a developer reads the Architecture Guidelines in CLAUDE.md
- **THEN** they see a short summary of the controller rule and a pointer to `/code-review` for the full rule set
