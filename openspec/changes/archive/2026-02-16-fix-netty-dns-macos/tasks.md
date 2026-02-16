## 1. Add Dependency

- [x] 1.1 Add `io.netty:netty-resolver-dns-native-macos` dependency to `pom.xml` with classifier `osx-aarch_64` and scope `runtime`

## 2. Verify

- [x] 2.1 Run `./mvnw dependency:resolve` to confirm the dependency resolves correctly
- [x] 2.2 Run the test suite (`./mvnw test`) to ensure no regressions
