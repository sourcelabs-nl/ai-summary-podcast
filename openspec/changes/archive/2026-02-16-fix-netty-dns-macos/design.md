## Context

The application uses Spring AI's OpenAI starter (`spring-ai-starter-model-openai`), which transitively depends on Reactor Netty for HTTP communication. On macOS, Netty attempts to load a native DNS resolver (`MacOSDnsServerAddressStreamProvider`) but fails because the `io.netty:netty-resolver-dns-native-macos` artifact is not on the classpath. This causes an ERROR log and a fallback to system defaults, which may produce incorrect DNS resolutions.

## Goals / Non-Goals

**Goals:**
- Eliminate the Netty DNS resolver ERROR log on macOS
- Ensure correct DNS resolution for all Reactor Netty HTTP clients

**Non-Goals:**
- Changing HTTP client implementations or switching away from Reactor Netty
- Adding Linux or Windows native resolvers (not needed — the error only occurs on macOS dev machines)

## Decisions

### Add netty-resolver-dns-native-macos with classifier

Add the dependency to `pom.xml` with:
- **Classifier**: `osx-aarch_64` — targets Apple Silicon (the development platform)
- **Scope**: `runtime` — only needed at runtime, not compile time
- **No explicit version** — Spring Boot's BOM manages the Netty version, keeping it aligned with all other Netty modules

**Alternative considered**: Using the `osx-x86_64` classifier or no classifier. Since the target dev machine is Apple Silicon, `osx-aarch_64` is correct. The dependency has no effect on non-macOS platforms (it simply won't load).

## Risks / Trade-offs

- **[Risk] Dependency only benefits macOS/aarch64** → Acceptable trade-off. The dependency is small (~100KB), has no effect on other platforms, and resolves a real error.
- **[Risk] Future architecture change (e.g., Intel Mac)** → Would need the `osx-x86_64` classifier instead. Low probability given industry direction.