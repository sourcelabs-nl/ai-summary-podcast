## Context

Environment variables are currently managed via a `.env` file in the project root, sourced by `start.sh` (lines 19-23). The README instructs users to create this file. The user has direnv installed and already has an untracked `.envrc` file.

## Goals / Non-Goals

**Goals:**
- Recommend direnv as the standard way to manage environment variables
- Remove manual `.env` sourcing from `start.sh`
- Keep backwards compatibility for users who prefer `.env` (direnv can source it)

**Non-Goals:**
- Removing `.env` support entirely — users who don't want direnv can still export vars manually
- Changing which environment variables exist or how the app consumes them

## Decisions

**1. Use `export` directives in `.envrc` (not `dotenv`)**

The `.envrc` file will use explicit `export VAR=value` lines rather than `dotenv` (which sources a `.env` file). This keeps everything in one file and avoids a double-file pattern.

Alternative considered: `dotenv` directive in `.envrc` that sources `.env` — adds indirection without benefit.

**2. Keep `.env` in `.gitignore`**

Even though we're moving to `.envrc`, keep `.env` gitignored for users who may still use it or for tools that create one.

**3. Remove `source .env` from `start.sh`**

With direnv, the environment is already loaded in the shell. The `start.sh` script doesn't need to source anything — it inherits the environment from the calling shell. This simplifies the script.

## Risks / Trade-offs

- [Users without direnv] → README will note that users can alternatively export variables in their shell profile or source a `.env` file manually. The app doesn't care how vars get into the environment.
- [Existing `.env` users] → They'll need to migrate to `.envrc` or export vars another way. This is a minor one-time effort.