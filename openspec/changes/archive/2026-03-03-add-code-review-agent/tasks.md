## 1. Create the code-review skill

- [x] 1.1 Rewrite `.claude/skills/code-review/SKILL.md` with `user-invocable: true`, `context: fork`, `agent: code-reviewer` in frontmatter, and expanded review rules covering: controller hygiene, service layer, Spring Data JDBC, database consistency, testing (MockK), Jackson 3.x, LLM prompt grounding
- [x] 1.2 Define the structured output format in the skill (categories, severities, file:line references)
- [x] 1.3 Define the review scope logic in the skill ($ARGUMENTS handling: no args → changed files, path → specific files)

## 2. Create the code-reviewer agent

- [x] 2.1 Create `.claude/agents/code-reviewer.md` with frontmatter: name, description, tools (Read, Glob, Grep, Bash), disallowedTools (Edit, Write, NotebookEdit), skills (code-review, flyway-migration, jackson-migration), model (sonnet)
- [x] 2.2 Write the agent system prompt: instructions for reading code, applying preloaded skill rules, and producing structured review output

## 3. Update CLAUDE.md

- [x] 3.1 Replace the Architecture Guidelines section with a concise summary that references the code-review skill as the authoritative source for review rules
