---
name: openspec-drift-check
description: Detect and report drift between OpenSpec specs and codebase implementation. Use when the user wants to check if specs are still in sync with the code.
license: MIT
compatibility: Requires openspec CLI and specs under openspec/specs/.
metadata:
  author: openspec
  version: "1.0"
  generatedBy: "1.1.1"
---

Detect drift between OpenSpec specs and the codebase. Compares each spec's requirements and scenarios against the actual implementation and produces a structured report.

**Input**: Optionally specify spec name(s) (comma-separated). If omitted, checks all specs under `openspec/specs/`.

**Steps**

1. **Discover specs**

   If specific spec names were provided as arguments, validate each exists under `openspec/specs/<name>/spec.md`.

   If no names provided, list all directories under `openspec/specs/`:
   ```bash
   ls openspec/specs/
   ```

   Collect the full list of spec names to check.

2. **Batch specs for parallel processing**

   Group the spec names into batches of approximately 10 specs each. For small sets (≤10), use a single batch.

3. **Launch parallel Explore subagents**

   Launch one **Explore** subagent per batch using the **Agent tool** with `subagent_type: "Explore"` and `model: "sonnet"`. Send all agent calls in a **single message** for true parallelism.

   Each agent receives this prompt template (fill in the batch-specific spec names):

   ```
   You are checking OpenSpec specs against the codebase for drift. For each spec listed below, follow this process:

   1. Read the spec file at `openspec/specs/<name>/spec.md`
   2. Extract all requirements (sections starting with `### Requirement:`) and scenarios (sections starting with `#### Scenario:`)
   3. For each requirement and scenario:
      - Identify concrete expectations: identifiers, file paths, values, behaviors, status codes, column names, field names, UI elements, config properties, etc.
      - Search the codebase using Grep, Glob, and Read to find the corresponding implementation
      - Compare what the spec says vs what the code actually does
      - Note any discrepancies with file:line references
   4. Produce a verdict for each spec:
      - **IN SYNC** — all requirements and scenarios match the implementation
      - **DRIFT** — concrete discrepancies found (list each one)
      - **PARTIAL** — some requirements match but others diverge or cannot be verified from code alone
      - **UNVERIFIABLE** — the spec describes behavior that requires runtime or integration testing (e.g., OAuth flows, audio output, external API responses)

   When uncertain, prefer PARTIAL over DRIFT to avoid false positives.

   Return your findings in this exact format for each spec:

   ### <spec-name>
   **Verdict:** IN SYNC | DRIFT | PARTIAL | UNVERIFIABLE
   **Notes:** <one-line summary>
   **Details:** (only for DRIFT or PARTIAL)
   - <requirement/scenario name>: <what spec says> vs <what code does> (`file:line`)

   Specs to check: <comma-separated list of spec names in this batch>
   ```

4. **Compile results**

   After all agents complete, merge their findings into a single report:

   **Summary stats:**
   - Total specs checked
   - Count per verdict (IN SYNC, DRIFT, PARTIAL, UNVERIFIABLE)

   **Summary table:**
   ```
   | # | Spec | Verdict | Notes |
   |---|------|---------|-------|
   | 1 | spec-name | **IN SYNC** | All requirements match |
   ```

   **Detailed findings** (only for DRIFT and PARTIAL specs):
   - Group by spec name
   - List each discrepancy with file:line references

5. **Present report to user**

   Display the full report directly. Then ask if they want it saved to `openspec/drift-report.md`.

**Output Format**

```
## Drift Report

_Generated: YYYY-MM-DD_

### Stats
- **Total:** N specs
- **In Sync:** X | **Drift:** Y | **Partial:** Z | **Unverifiable:** W

### Summary Table

| # | Spec | Verdict | Notes |
|---|------|---------|-------|
| 1 | spec-name | **IN SYNC** | All requirements match |
| 2 | other-spec | **DRIFT** | Expected X, found Y |

### Detailed Findings

#### other-spec — DRIFT
- **Requirement: Some requirement** — Expected `value_a`, found `value_b` at `src/file.ext:42`
- **Scenario: Edge case** — Spec says return 404, code returns 400 at `src/controller.ext:78`

#### another-spec — PARTIAL
- **Requirement: Feature X** — Implementation found but response shape differs at `src/service.ext:120`
- **Scenario: Empty input** — Could not verify from code alone (requires runtime test)
```

**Guardrails**

- **Project-agnostic** — Do not hardcode language, framework, or project-specific search patterns. The specs themselves contain enough context (identifiers, paths, values) for agents to know what to search for.
- **False positive avoidance** — When uncertain, prefer PARTIAL over DRIFT, UNVERIFIABLE over PARTIAL.
- **Actionable findings** — Every discrepancy must include a file:line reference where possible.
- **Concise summary** — Keep the summary table brief. Details go in the Detailed Findings section.
- **No side effects** — This skill is read-only. It never modifies code or specs. The only optional write is the drift report file.
