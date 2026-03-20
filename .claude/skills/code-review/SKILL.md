---
name: code-review
description: Use when reviewing code changes - PR reviews, pre-commit reviews, or automated workflows. Enforces architectural rules for Spring Boot/Kotlin codebases.
user-invocable: true
context: fork
agent: code-reviewer
argument-hint: "[path|--all]"
---

# Code Review

Review Kotlin code with these arguments: $ARGUMENTS

## Task

Run a code review for architectural violations, Spring Data JDBC issues, database inconsistencies, testing anti-patterns, and Jackson 3.x migration issues.

## Scope

Determine what to review based on the arguments above:
- If arguments contain **--all**: Glob for all `**/*.kt` files under `src/main/kotlin`. Do NOT check git diff.
- If arguments contain a **path** (e.g., `src/main/kotlin/`): Glob for all `.kt` files under that path.
- If **no arguments** were provided (empty): Review files changed vs the main branch. Run `git diff --name-only main...HEAD -- '*.kt'`. If on `main` with no changes, tell the user to pass a path or `--all` to review existing code.

For each file in scope, read it fully before applying rules. Also read relevant Flyway migrations (`src/main/resources/db/migration/`) when checking database rules.

## Rules

### 1. Controller Hygiene

Controllers must validate input, delegate to service or domain classes, and map responses. They must not contain business logic.

**Acceptable in controllers:**
- Input validation (null checks, format validation, returning 400)
- Authorization checks (ownership validation, returning 403/404)
- Calling service methods
- Mapping domain objects to response DTOs via extension functions (`.toResponse()`)
- Returning appropriate HTTP status codes (201 for creation, 202 for async, 409 for conflicts)

**Violations to flag:**
- Multi-step orchestration logic (should be in a service)
- Direct repository calls for mutations (save/delete) — reads for simple lookups are acceptable
- Domain calculations or business rule evaluation
- Duplicating logic that already exists in a service

### 2. Service Layer

Services are the single home for business logic. When the same operation can be triggered from multiple entry points (API endpoint, scheduler), the shared logic must live in a single service method — all entry points call that method.

**Violations to flag:**
- Duplicate logic — reimplementing something that already exists in another service method
- Multiple entry points (controller + scheduler) implementing the same operation with duplicated logic instead of sharing a service method
- Missing `@Transactional` on methods that call save/delete on multiple repositories
- Bypassing a service to directly access a repository owned by another service (e.g., calling `articleRepository` from `EpisodeService` instead of going through `ArticleService`, if one exists)

### 3. Type-Safe Constants

Domain values that represent a fixed set of states, categories, or types must be defined as enums, not hardcoded strings.

**Violations to flag:**
- Status values compared or assigned as string literals (e.g., `"ACTIVE"`, `"PENDING"`)
- Any fixed set of values used in `when` expressions or `if` chains that could be an enum
- New string constants introduced for values that already have an enum

### 4. Spring Data JDBC

**Entity rules:**
- Entity classes must be Kotlin `data class`
- Must have `@Table("<table_name>")` annotation
- Must have `@Id` on the primary key field
- Entities with concurrent write risk (shared resources) should use `@Version` for optimistic locking
- Foreign keys should be scalar fields (`String` or `Long`) referencing the parent entity's ID
- No business logic in entity classes

**Repository rules:**
- Repository interfaces extend `CrudRepository<Entity, IdType>`
- `@Query` annotations must use named parameters (`:paramName`), not positional (`?`)
- Non-SELECT queries (`DELETE`, `UPDATE`, `INSERT`) must have `@Modifying`
- No business logic in repositories

### 5. Database Consistency

**Violations to flag:**
- Foreign key columns used in `@Query` WHERE clauses that have no corresponding `CREATE INDEX` in any Flyway migration
- Entity field names that don't correspond to their database column names (Kotlin camelCase should map to SQL snake_case via Spring Data JDBC's default naming strategy)
- New entities without a corresponding Flyway migration

### 6. Testing — MockK Only

This project uses **MockK** (not Mockito) for all Kotlin tests, and `@MockkBean` from `springmockk` (`com.ninja-squad:springmockk`) for Spring integration tests.

**Violations to flag:**
- Imports from `org.mockito`
- Use of `@MockBean` (Spring's Mockito-based annotation) instead of `@MockkBean`
- Use of Mockito syntax (`when(...).thenReturn(...)`) instead of MockK syntax (`every { ... } returns ...`)

### 7. Jackson 3.x

This is a Spring Boot 4 project using Jackson 3.x. The `com.fasterxml.jackson.annotation` package is unchanged in 3.x and is still correct.

**Violations to flag:**
- Imports from `com.fasterxml.jackson.databind` (should be `tools.jackson.databind`)
- Imports from `com.fasterxml.jackson.core` (should be `tools.jackson.core`)
- Use of `JsonSerializer`/`JsonDeserializer` (should be `ValueSerializer`/`ValueDeserializer`)
- Use of `JsonProcessingException` (should be `JacksonException`)

**Not a violation:**
- Imports from `com.fasterxml.jackson.annotation` — these are correct in Jackson 3.x

### 8. Concurrency — Coroutines Only

This project uses Kotlin coroutines for all async/background work. No Java concurrency primitives are allowed.

**Violations to flag:**
- Use of `ExecutorService`, `Executors`, `ThreadPoolExecutor`, or any `java.util.concurrent` thread pool
- Use of `Thread()` or `thread {}` for async work
- Use of `CompletableFuture` for async orchestration (coroutines should be used instead)
- Using `Dispatchers.Default` for I/O-bound work (must use `Dispatchers.IO` for HTTP requests, database calls, file I/O)

**Not a violation:**
- `@Async` on Spring-managed methods (this is the Spring async mechanism)
- `Semaphore` from `kotlinx.coroutines.sync` (coroutine-aware concurrency primitive)

### 9. Code Reuse and Consistency

New code must reuse existing functionality rather than reimplementing it. Similar operations must follow consistent patterns.

**Violations to flag:**
- Reimplementing logic that already exists in a service method (e.g., writing a new cost calculation when `CostEstimator` already does it)
- Copy-pasting blocks of code across functions instead of extracting a shared function (look for 5+ lines that are structurally identical)
- Inconsistent patterns: if function A resolves a model with `modelResolver.resolve()` and function B does the same thing differently (e.g., manual lookup), flag the inconsistency
- Adding a new utility function that duplicates an existing one (e.g., a new date formatter when one exists)

**How to check:**
- When reviewing a new function, search the codebase for similar operations
- When reviewing changes to an existing pattern, check if the same pattern exists elsewhere and was updated consistently
- Flag cases where the same concept is implemented with different approaches in different places

### 10. LLM Prompt Grounding

Composer prompts (briefing, dialogue, interview) must include grounding instructions that constrain LLM output to the provided article content only.

**Violations to flag:**
- A new or modified composer prompt that lacks explicit instructions to only use provided article content
- Removal of existing grounding constraints from prompts
- Prompts that encourage the LLM to add external knowledge or speculation

## Output Format

Group findings by category. For each finding, include:

```
### <Category>

**<severity>** `<file_path>:<line_number>` — <Rule name>
<Description of the issue and suggested fix>
```

Severities:
- **violation** — must fix, breaks architectural rules
- **warning** — should fix, potential issue
- **note** — consider, style or best practice suggestion

If no issues are found, confirm the reviewed files pass all checks.

At the end, provide a summary count: `X violations, Y warnings, Z notes across N files reviewed`.
