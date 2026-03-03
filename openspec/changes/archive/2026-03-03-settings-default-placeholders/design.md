## Context

The podcast settings page has several nullable number fields (targetWords, maxLlmCostCents, fullBodyThreshold, maxArticleAgeDays) and a key-value editor (llmModels) that appear empty when no override is set. The system uses defaults from `application.yaml` at runtime, but users have no visibility into these values.

## Goals / Non-Goals

**Goals:**
- Show system default values as placeholder text in empty nullable fields
- Make it clear these are system defaults, not user-set values

**Non-Goals:**
- No backend API for fetching defaults — hardcode placeholder values in the frontend
- No visual changes to fields that already have explicit values

## Decisions

**Hardcoded placeholders over API endpoint**: The defaults rarely change and there are only ~5 fields. A backend endpoint would be cleaner but over-engineered for this. If defaults change frequently in the future, a `/config/defaults` endpoint could replace the hardcoded values.

**Placeholder format**: Use `{value} (system default)` pattern (e.g., `1500 (system default)`). This is standard HTML placeholder behavior — grey text that disappears on input.

**LLM Models key-value editor**: Show placeholder text below the editor listing the default model assignments: "System defaults: filter = openai/gpt-4o-mini, compose = anthropic/claude-sonnet-4.6". This is clearer than ghost rows for a key-value editor.

## Risks / Trade-offs

- [Drift] Hardcoded placeholders can become stale if `application.yaml` defaults change → Low risk since defaults change infrequently; add a comment in the code referencing the source
- [UX] Placeholders disappear when the field is focused → Acceptable; the format is intuitive enough that users remember the value
