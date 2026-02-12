## Context

API keys are stored in `user_api_keys` with a composite PK of `(user_id, provider)`. The `provider` field is a free-text string (`"openrouter"`, `"openai"`). Callers like `ChatClientFactory` and `TtsService` resolve keys by provider name, but there is no explicit concept of *what the key is for*. Users managing their keys via the API cannot tell which key drives LLM processing vs. TTS generation.

The current data model, API, and resolution logic all need a `category` dimension.

## Goals / Non-Goals

**Goals:**
- Every API key is classified as either `LLM` or `TTS`.
- The list endpoint exposes the category so clients can present keys grouped by purpose.
- Key resolution can look up "the user's LLM key" or "the user's TTS key" by category, not just by provider name.
- Existing data is migrated with correct categories (`openrouter` → `LLM`, `openai` → `TTS`).

**Non-Goals:**
- Supporting additional categories beyond `LLM` and `TTS` (can be added later).
- Changing the encryption scheme or master-key handling.
- Adding provider validation (the provider field remains free-text).

## Decisions

### 1. Category is a required, stored column (not derived)

**Decision:** Add a `category TEXT NOT NULL` column to `user_api_keys` and store it explicitly.

**Alternatives considered:**
- *Derive category from provider at runtime* — simpler schema but requires maintaining a provider→category mapping that could drift. Adding a new LLM provider would require a code change instead of just storing the right category.

**Rationale:** Storing the category makes the model self-describing and allows future providers to be assigned to either category without code changes.

### 2. Composite PK becomes `(user_id, category)`

**Decision:** Change the primary key to `(user_id, category)`.

**Alternatives considered:**
- *Keep PK as `(user_id, provider)`* — simpler but a single provider could never serve two categories (unlikely today, but the model would be inconsistent if category is stored but not part of the key).
- *PK as `(user_id, category)` only* — would limit each user to one provider per category, which is actually the desired constraint today.

**Rationale:** Using `(user_id, category)` as the unique constraint is the cleanest model: each user has exactly one LLM key and one TTS key, regardless of provider. The provider is metadata on the key, not part of the identity. This means the PK changes from `(user_id, provider)` to `(user_id, category)`.

### 3. API URL structure: category replaces provider as path variable

**Decision:** Change the API path from `/users/{userId}/api-keys/{provider}` to `/users/{userId}/api-keys/{category}` where `category` is `LLM` or `TTS`. The provider becomes a field in the request body alongside `apiKey`.

**Alternatives considered:**
- *Add category as a query param or body field while keeping provider in the path* — muddies the URL semantics since provider is no longer the unique identifier.
- *Nested path `/users/{userId}/api-keys/{category}/{provider}`* — over-complicated since we settled on one key per category.

**Rationale:** Since the unique constraint is `(user_id, category)`, the category is the natural path variable. The provider is an attribute of the stored key.

### 4. Key resolution by category

**Decision:** Change `resolveKey(userId, provider)` to `resolveKey(userId, category)`. The method looks up the key by `(user_id, category)`, decrypts it, and returns both the API key and provider. Fallback to environment variables uses the category to determine the env var name (e.g., `LLM` → `OPENROUTER_API_KEY`, `TTS` → `OPENAI_API_KEY`).

**Rationale:** Pipeline services (`ChatClientFactory`, `TtsService`) already know which *category* of key they need. Resolving by category is more natural than resolving by provider.

### 5. Enum for category values

**Decision:** Use a Kotlin `enum class ApiKeyCategory { LLM, TTS }` and validate at the controller level. Invalid categories return HTTP 400.

**Rationale:** An enum gives compile-time safety and self-documents the allowed values.

## Risks / Trade-offs

- **Breaking API change** → This is a breaking change to the REST API (path changes from `/{provider}` to `/{category}`, response shape changes). Acceptable since the project is in early stages with no external consumers.
- **Migration correctness** → Backfilling existing rows requires mapping provider→category. Only `openrouter` and `openai` exist today, so the mapping is straightforward. Unknown providers would fail the migration, but none exist in practice.
- **One key per category** → Users cannot configure multiple LLM providers simultaneously. This is intentional for now and can be revisited later.
- **Env var fallback hardcoded to one provider per category** → The fallback maps `LLM` → `OPENROUTER_API_KEY` and `TTS` → `OPENAI_API_KEY`. If a user stores a key for a different LLM provider, the fallback still checks `OPENROUTER_API_KEY`. This is acceptable since fallback is a convenience, not the primary resolution path.