## Why

The application logs an ERROR on macOS: `Unable to load io.netty.resolver.dns.macos.MacOSDnsServerAddressStreamProvider, fallback to system defaults. This may result in incorrect DNS resolutions on MacOS.` This is caused by a missing native Netty DNS resolver dependency required by Reactor Netty (pulled in transitively by Spring AI's OpenAI starter). Without it, DNS resolution may silently produce incorrect results on macOS.

## What Changes

- Add `io.netty:netty-resolver-dns-native-macos` dependency to `pom.xml` with the `osx-aarch_64` classifier and `runtime` scope, enabling the native macOS DNS resolver.

## Capabilities

### New Capabilities

_None — this is a dependency fix, not a new capability._

### Modified Capabilities

_None — no spec-level behavior changes._

## Impact

- **Dependencies**: New runtime dependency `io.netty:netty-resolver-dns-native-macos` added to `pom.xml`. Version is managed by Spring Boot's dependency management (BOM), so no explicit version needed.
- **Affected code**: None — no source code changes required.
- **Runtime**: Eliminates the DNS resolution ERROR log on macOS and ensures correct DNS behavior for all Reactor Netty HTTP clients (used by Spring AI's OpenAI integration).