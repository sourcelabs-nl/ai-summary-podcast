## 1. Header Navigation

- [x] 1.1 Add gear icon button next to user dropdown in header component, linking to `/settings`
- [x] 1.2 Hide gear icon when no user is selected (loading or no users)

## 2. Settings Page - Profile Section

- [x] 2.1 Create `/settings` route and page component with two sections layout
- [x] 2.2 Implement Profile section with name input pre-filled from user context and Save button calling `PUT /users/{userId}`
- [x] 2.3 Refresh user context after successful name update

## 3. Settings Page - API Keys Section

- [x] 3.1 Add API Keys section with security notice (lock icon, encryption message)
- [x] 3.2 Fetch and display provider config list from `GET /users/{userId}/api-keys` with provider name, category badge (default for LLM, outline for TTS), base URL, and action buttons
- [x] 3.3 Implement Remove button calling `DELETE /users/{userId}/api-keys/{category}/{provider}` with list refresh
- [x] 3.4 Show empty state when no providers are configured

## 4. Add/Edit Provider Dialog

- [x] 4.1 Create wizard-style dialog component with category selector, filtered provider dropdown, API key input (password), and base URL field
- [x] 4.2 Implement category-to-provider filtering (LLM: openrouter/openai/ollama, TTS: openai/elevenlabs/inworld) and exclude already-configured providers
- [x] 4.3 Pre-fill base URL from known provider defaults when provider is selected
- [x] 4.4 Make API key optional when ollama is selected
- [x] 4.5 Implement edit mode: category and provider read-only, API key empty, base URL pre-filled with current value
- [x] 4.6 Submit add/edit calls `PUT /users/{userId}/api-keys/{category}` and refreshes the list
