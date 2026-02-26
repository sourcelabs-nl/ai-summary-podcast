## Context

The project enforces architectural rules (controller hygiene, MockK-only testing, Spring Data JDBC patterns) through CLAUDE.md and a non-invocable `code-review` skill. Additionally, `flyway-migration` and `jackson-migration` skills exist but aren't connected to a review workflow. There is no way to trigger a structured code review on demand.

Claude Code supports **agents** (`.claude/agents/*.md`) that can preload **skills** and run as specialized subagents, and **skills** (`.claude/skills/*/SKILL.md`) that define reusable instruction sets. A skill with `user-invocable: true` appears in the `/` menu for on-demand invocation.

## Goals / Non-Goals

**Goals:**
- Single `/code-review` command that reviews Kotlin code against all project rules
- Agent preloads `code-review`, `flyway-migration`, and `jackson-migration` skills for full context
- Consolidate architecture rules into the skill (single source of truth)
- Cover: controller hygiene, service patterns, Spring Data JDBC, database consistency, MockK, Jackson 3.x, Flyway migrations

**Non-Goals:**
- Automated CI/CD integration (manual invocation only for now)
- Auto-fixing violations (report only)
- Reviewing non-Kotlin files (frontend, config, docs)
- Creating new test frameworks or linting tools

## Decisions

### 1. Skill as the user entry point, agent as the executor

The skill (`code-review`) becomes user-invocable and uses `context: fork` with `agent: code-reviewer` to delegate to the agent. This means:
- User runs `/code-review` → skill forks into the agent
- Agent has `skills: [code-review, flyway-migration, jackson-migration]` preloaded
- Agent reads code, applies rules, reports findings

**Why not agent-only?** Skills are the standard user-invocable entry point in Claude Code. Agents are invoked by Claude through task delegation — they don't appear in the `/` menu. Making the skill user-invocable with `context: fork` and `agent: code-reviewer` gives us both: a clean user entry point and a specialized agent context.

**Why not skill-only?** A skill without an agent runs in the main conversation context. Forking into an agent isolates the review, keeps the main context clean, and lets the agent have its own tool restrictions and preloaded skills.

### 2. Agent preloads three skills

The agent's `skills` frontmatter will list `[code-review, flyway-migration, jackson-migration]`. This injects their full content into the agent's context at startup, so the agent has all review rules available without needing to read files at runtime.

**Alternative considered:** Having the agent dynamically read skill files. Rejected because preloading is simpler and guaranteed to work — the agent always has the full rules available.

### 3. Agent tools restricted to read-only

The agent should only read code — it must not edit files. Frontmatter: `tools: Read, Glob, Grep, Bash`. The agent uses Bash only for running `ls` or `git diff` to identify changed files.

### 4. Review scope: changed files by default, full codebase on request

The skill accepts `$ARGUMENTS` for targeting:
- No arguments → review files changed vs the main branch (`git diff --name-only main...HEAD`)
- Explicit path → review that file/directory
- `--all` → review entire `src/main/kotlin` tree

### 5. Architecture rules move from CLAUDE.md to the skill

The "Architecture Guidelines" section in CLAUDE.md currently inlines the controller/service rules. These move into the `code-review` skill as authoritative review rules. CLAUDE.md keeps a short summary pointing to the skill.

### 6. Structured output format

The agent reports findings grouped by category (Architecture, Spring Data JDBC, Database, Testing, Jackson) with severity levels (violation, warning, note). Each finding includes file path, line reference, rule violated, and suggested fix.

## Risks / Trade-offs

**[Agent context size]** → Preloading three skills adds ~300 lines of context. This is well within limits and acceptable for focused reviews.

**[False positives on complex validation in controllers]** → Some inline validation (e.g., TTS config validation) is acceptable in controllers. The rules should distinguish between input validation (OK in controllers) and business logic (not OK). → Mitigation: Rules explicitly state that input validation is acceptable.

**[Scope creep during review]** → Agent might try to fix code instead of just reporting. → Mitigation: `disallowedTools: Edit, Write, NotebookEdit` in agent frontmatter ensures read-only.
