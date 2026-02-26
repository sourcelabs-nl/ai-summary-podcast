## Why

The project has architectural rules (no business logic in controllers, MockK-only testing, Spring Data JDBC patterns) documented in CLAUDE.md and a non-invocable `code-review` skill, but there is no dedicated agent that can systematically review Kotlin code against these rules. A code review agent with a user-invocable skill would let developers trigger structured reviews on demand and catch violations like controller logic leaks, missing database indexes, and incorrect Spring Data JDBC patterns before they reach production.

## What Changes

- Create a new Claude Code **agent** at `.claude/agents/code-reviewer.md` that orchestrates code review by reading the codebase and applying review rules. The agent preloads existing skills via `skills: [code-review, flyway-migration, jackson-migration]` so it has full context on project-specific review rules, Flyway migration patterns, and Jackson 3.x conventions
- Create a new **user-invocable skill** at `.claude/skills/code-review/SKILL.md` (replaces the existing non-invocable skill) that contains the actual review rules and checklist
- The skill becomes user-invocable (`user-invocable: true`) so developers can trigger reviews with `/code-review`
- Move architecture-enforcement rules from CLAUDE.md into the skill (controller rules, no-duplication rule) so they live in one authoritative place, and reference them from CLAUDE.md instead of duplicating
- Leverage existing skills during review:
  - **flyway-migration** — validate migration naming, versioning, SQLite dialect correctness, and that migrations match entity definitions
  - **jackson-migration** — flag Jackson 2.x imports/patterns that should be 3.x in this Spring Boot 4 project
- Expand the review rules beyond the current MockK and LLM-prompt checks to cover:
  - **Controller hygiene** — no business logic, validate-delegate-map only
  - **Service layer** — no duplicate logic, proper delegation
  - **Spring Data JDBC patterns** — proper `@Table`/`@Id` annotations, foreign key references, missing indexes on frequently-queried columns, correct use of `@MappedCollection` vs manual queries
  - **Database consistency** — Flyway migrations match entity definitions, no missing indexes for foreign keys or query predicates

## Capabilities

### New Capabilities
- `code-review-agent`: Claude Code agent configuration and user-invocable skill for on-demand Kotlin code review covering architecture, Spring Data JDBC, and database patterns

### Modified Capabilities

_(none — this change adds new files and updates the existing code-review skill in place)_

## Impact

- **New files:** `.claude/agents/code-reviewer.md`, updated `.claude/skills/code-review/SKILL.md`
- **CLAUDE.md:** Architecture Guidelines section will reference the code-review skill instead of inlining the rules (keeps a short summary + pointer)
- **No code changes** — this is a tooling/workflow change only
- **No breaking changes** — existing automated skill invocations (if any) will continue to work since the skill name stays the same