## Why

Currently each user can store only one provider config per category (LLM or TTS) because the `user_provider_configs` table uses `(user_id, category)` as its primary key. Users who want to switch between providers (e.g., OpenRouter for daily use and Ollama for testing) must overwrite their existing config each time. Allowing multiple providers per category lets users store all their provider credentials and select which one to use per podcast.

## What Changes

- **BREAKING**: Change the `user_provider_configs` primary key from `(user_id, category)` to `(user_id, category, provider)`, allowing multiple providers per category.
- Change the `PUT /users/{userId}/api-keys/{category}` endpoint to upsert by `(user_id, category, provider)` instead of `(user_id, category)` — storing a second provider no longer replaces the first.
- Change the `DELETE /users/{userId}/api-keys/{category}` endpoint to require a `provider` parameter, deleting a specific provider config rather than the entire category.
- Update `GET /users/{userId}/api-keys` to return multiple entries per category.
- Update `resolveConfig` to pick the provider config for the provider that is actually configured on the podcast (future work; for now, fall back to the first config in the category or the global env var).

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `user-api-keys`: The storage constraint changes from one-provider-per-category to one-config-per-provider-per-category. The API endpoints for set/delete/list are updated accordingly.

## Impact

- **Database**: Migration to change PK from `(user_id, category)` to `(user_id, category, provider)`. Existing data is preserved — each row already has a `provider` value.
- **API**: `DELETE /users/{userId}/api-keys/{category}` now requires a `provider` query/path parameter — this is a **breaking** change for existing clients.
- **Repository**: `save` upsert and `delete` queries need updated conflict/where clauses.
- **Service**: `resolveConfig` needs a strategy for picking among multiple providers in a category (initially: use the first stored config).
- **Tests**: Controller and service tests need updating for the new multi-provider behavior.