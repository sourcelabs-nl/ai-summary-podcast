## Context

The `user_provider_configs` table currently uses `(user_id, category)` as its primary key, limiting each user to one provider per category (LLM or TTS). The `provider` column exists but is not part of the key. The repository uses `ON CONFLICT (user_id, category) DO UPDATE` for upserts and queries by `(user_id, category)` for lookups and deletes.

The API surface is:
- `PUT /users/{userId}/api-keys/{category}` — set config (upserts by user+category)
- `GET /users/{userId}/api-keys` — list configs
- `DELETE /users/{userId}/api-keys/{category}` — delete config by category

The `resolveConfig` method returns a single `ProviderConfig` per category, with fallback to env vars.

## Goals / Non-Goals

**Goals:**
- Allow multiple provider configs per category per user (one per provider name)
- Update all CRUD endpoints to work with the new composite key
- Preserve existing data during migration
- Keep `resolveConfig` working (pick first stored config for now)

**Non-Goals:**
- Per-podcast provider selection (future work — podcasts don't yet reference a specific provider)
- Provider priority/ordering within a category
- UI for managing multiple providers

## Decisions

### 1. Primary key: `(user_id, category, provider)`

Change the SQLite PK from `(user_id, category)` to `(user_id, category, provider)`. This is the minimal change to allow multiple providers per category while enforcing uniqueness per provider.

**Alternative considered**: Adding a separate `id` column as PK. Rejected — the natural composite key is sufficient and avoids an extra column.

### 2. DELETE endpoint uses path parameter for provider

Change `DELETE /users/{userId}/api-keys/{category}` to `DELETE /users/{userId}/api-keys/{category}/{provider}`. A path parameter is consistent with the resource model (the provider is part of the identity, not a filter).

**Alternative considered**: Query parameter `?provider=openrouter`. Rejected — the provider is part of the resource identity, not a query filter.

### 3. Migration via table recreate

SQLite does not support `ALTER TABLE ... DROP PRIMARY KEY`. The migration will recreate the table with the new PK, copy data, and drop the old table. This is the standard SQLite approach for PK changes.

### 4. resolveConfig picks first stored config

When resolving a provider config for a category, the system will return the first stored config for that category. This is a simple temporary strategy until per-podcast provider selection is implemented.

## Risks / Trade-offs

- **Breaking API change on DELETE** → Existing clients calling `DELETE /users/{userId}/api-keys/{category}` will get a 404 or routing error. Mitigation: this is a pre-release application with no external clients yet.
- **SQLite table recreate during migration** → Brief lock during migration. Mitigation: table is small, migration is fast, and this is a standard SQLite pattern.
- **resolveConfig ambiguity with multiple providers** → If a user stores both `openrouter` and `ollama` for LLM, which one gets used? Mitigation: use the first one found. Document that per-podcast selection is future work.