---
name: "OPSX: Drift"
description: Detect drift between OpenSpec specs and codebase implementation
category: Workflow
tags: [workflow, drift, verification, experimental]
---

Detect drift between OpenSpec specs and the codebase. Compares each spec's requirements and scenarios against the actual implementation and produces a structured report.

**Input**: Optionally specify spec name(s) after `/opsx:drift` (e.g., `/opsx:drift twitter-polling` or `/opsx:drift twitter-polling,source-config`). If omitted, checks all specs under `openspec/specs/`.

**Steps**

1. **Discover specs**

   If specific spec names were provided, validate they exist under `openspec/specs/`.
   If no names provided, list all directories under `openspec/specs/`.

2. **Batch and verify**

   Group specs into batches of ~10 for parallel processing.

3. **Launch parallel Explore subagents** (Sonnet model, one per batch)

   Each agent receives a list of spec names and instructions to:
   - Read each spec at `openspec/specs/<name>/spec.md`
   - Extract requirements (marked with `### Requirement:`) and scenarios (marked with `#### Scenario:`)
   - For each requirement/scenario, extract concrete expectations (identifiers, paths, values, behaviors)
   - Search the codebase using those identifiers via Grep/Glob/Read
   - Compare what the spec says vs what the code does
   - Note discrepancies with file:line references
   - Return a per-spec verdict with brief notes

   **Verdicts:**
   - **IN SYNC** — implementation matches spec
   - **DRIFT** — concrete discrepancies found (list them)
   - **PARTIAL** — some requirements match, others diverge or can't be verified
   - **UNVERIFIABLE** — requires runtime/integration testing

4. **Compile results** into a structured report:
   - Summary stats: total specs checked, counts per verdict
   - Summary table: `# | Spec | Verdict | Notes`
   - Detailed findings for any DRIFT or PARTIAL specs
   - List of UNVERIFIABLE items

5. **Ask the user** if they want the report saved to `openspec/drift-report.md`

**Output**

```
## Drift Report

_Generated: YYYY-MM-DD_

### Stats
- Total: N specs
- In Sync: X | Drift: Y | Partial: Z | Unverifiable: W

### Summary Table

| # | Spec | Verdict | Notes |
|---|------|---------|-------|
| 1 | spec-name | **IN SYNC** | All requirements match |
| 2 | other-spec | **DRIFT** | Expected X, found Y |

### Detailed Findings

#### other-spec — DRIFT
- **Requirement: Some requirement** — Expected `value_a`, found `value_b` at `src/file.ext:42`
```

**Guardrails**
- Do not hardcode project-specific patterns — the skill is project-agnostic
- Specs contain enough context (identifiers, paths, values) for agents to search the codebase
- When uncertain, prefer PARTIAL over DRIFT to avoid false positives
- Every finding must include a file:line reference where possible
- Keep the summary table concise — details go in the Detailed Findings section
