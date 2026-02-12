## 1. Database Migration

- [x] 1.1 Create Flyway migration to recreate `user_provider_configs` with PK `(user_id, category, provider)` — copy data, drop old table

## 2. Repository

- [x] 2.1 Update `UserProviderConfigRepository.save` upsert to use `ON CONFLICT (user_id, category, provider)`
- [x] 2.2 Update `UserProviderConfigRepository.deleteByUserIdAndCategory` to `deleteByUserIdAndCategoryAndProvider` — add provider parameter to the WHERE clause
- [x] 2.3 Update `findByUserIdAndCategory` to return `List<UserProviderConfig>` instead of a single result

## 3. Service

- [x] 3.1 Update `UserProviderConfigService.deleteConfig` to accept a `provider` parameter
- [x] 3.2 Update `UserProviderConfigService.resolveConfig` to handle multiple configs per category (pick first)

## 4. Controller

- [x] 4.1 Change `DELETE /users/{userId}/api-keys/{category}` to `DELETE /users/{userId}/api-keys/{category}/{provider}`
- [x] 4.2 Update controller delete method to pass provider to service

## 5. Tests

- [x] 5.1 Update `UserProviderConfigControllerTest` for new delete path and multi-provider scenarios
- [x] 5.2 Update `UserProviderConfigServiceTest` for multi-provider storage, delete-by-provider, and resolve-first behavior
