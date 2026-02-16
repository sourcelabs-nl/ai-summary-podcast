## Context

The podcast create and update API endpoints accept JSON request bodies that are deserialized into Kotlin data classes (`CreatePodcastRequest`, `UpdatePodcastRequest`). These DTOs use nullable primitive types (`Int?`, `Boolean?`, `Double?`) with default values of `null` to distinguish between "field not provided" and "field explicitly set".

The controller correctly passes deserialized DTO values into a `Podcast` entity and calls `PodcastService.create()` / `PodcastService.update()`. However, the service methods did not include `relevanceThreshold` and `requireReview` in their `Podcast.copy()` calls, silently dropping these fields and always falling back to entity defaults (5 and false respectively).

## Goals / Non-Goals

**Goals:**
- Fix `PodcastService.create()` and `PodcastService.update()` to propagate `relevanceThreshold` and `requireReview`
- Add defensive `@JsonProperty` annotations on nullable primitive DTO fields as a safeguard against Jackson 3 Kotlin module edge cases
- Add test coverage to prevent regression

**Non-Goals:**
- Refactoring the DTO structure or changing the API contract
- Changing other DTOs not affected by this issue

## Decisions

### Decision 1: Add missing fields to PodcastService copy operations

The primary fix: add `relevanceThreshold` and `requireReview` to the `Podcast` constructor call in `PodcastService.create()` and the `existing.copy()` call in `PodcastService.update()`. This is where the values were being silently dropped.

### Decision 2: Add defensive `@JsonProperty` annotations to nullable primitive DTO fields

Add explicit `@JsonProperty` annotations to all nullable primitive-typed fields (`Int?`, `Boolean?`, `Double?`) in both request DTOs. Jackson 3's Kotlin module has known edge cases with nullable boxed primitives; these annotations ensure explicit property binding as a secondary safeguard. Uses `com.fasterxml.jackson.annotation.JsonProperty` (Jackson 3 shares the annotations JAR with Jackson 2).

**Alternatives considered:**
- *Custom `KotlinModule` configuration* — Would require a `@Bean` overriding Spring Boot's auto-configured `ObjectMapper`, risking side effects elsewhere. The `@JsonProperty` approach is surgical.
- *Only fixing the service layer* — Would work, but leaves the DTOs vulnerable to potential Jackson 3 Kotlin module deserialization issues with nullable primitives.

### Decision 3: Test with `@WebMvcTest` using MockMvc

Follow the existing test pattern (see `PodcastControllerLanguageTest`) using `@WebMvcTest` with MockkBean. This tests the full Spring MVC deserialization pipeline (Jackson → controller) without needing a running database. Tests use `slot<Podcast>()` to capture the actual values passed to the service, verifying end-to-end correctness from JSON to response.

## Risks / Trade-offs

- **Low risk**: The service fix is a straightforward addition of two fields to existing copy operations. `@JsonProperty` is a standard Jackson annotation with no behavioral side effects.
- **Maintenance**: If Jackson 3's Kotlin module improves nullable primitive handling, the annotations become redundant but harmless.
