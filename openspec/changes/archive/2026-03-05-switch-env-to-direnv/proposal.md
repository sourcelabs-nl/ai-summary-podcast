## Why

The project currently instructs users to create a `.env` file in the project root and sources it in `start.sh`. While functional, direnv (`.envrc`) is a better fit: it auto-loads/unloads environment variables when entering/leaving the project directory, keeps secrets scoped to the project (unlike `~/.zshenv`), and removes the need for manual sourcing logic in scripts.

## What Changes

- Remove the `source .env` block from `start.sh` — direnv handles environment loading
- Update `README.md` setup section to recommend `.envrc` with direnv instead of `.env`
- Update `.gitignore` to include `.envrc` (keep `.env` for backwards compatibility)
- Update `CLAUDE.md` to reflect the new env var documentation

## Capabilities

### New Capabilities

None — this is a documentation and configuration change, not a new capability.

### Modified Capabilities

None — no spec-level behavior changes.

## Impact

- `start.sh` — removal of `.env` sourcing block
- `README.md` — setup instructions rewritten for direnv
- `.gitignore` — add `.envrc`
- `CLAUDE.md` — update environment variable references