## Why

README.md and CLAUDE.md contain outdated information after the unified-model-config refactor and accumulated drift from earlier changes. CLAUDE.md duplicates project overview content that belongs in README.md, with stale details (mentions Reddit, YouTube, Google Cloud TTS, PostgreSQL, "early stages"). The README .envrc section lists env vars that are now manageable via the UI/API. Both files need cleanup to reflect current state.

## What Changes

- **README.md**: Simplify `.envrc` to only `APP_ENCRYPTION_MASTER_KEY`. Restructure credential sections to lead with UI/API management, env vars as optional fallbacks. Fix remaining stale references.
- **CLAUDE.md**: Remove duplicated project overview, architecture, data model, external dependencies, and source configuration sections. Replace with references to README.md. Fix outdated env var requirements. Keep Claude-specific instructions (coding guidelines, testing, architecture rules, frontend patterns).

## Capabilities

### New Capabilities

_None_

### Modified Capabilities

_None_ (documentation-only change, no spec-level behavior changes)

## Impact

- **Documentation**: README.md, CLAUDE.md
- No code, API, or database changes
