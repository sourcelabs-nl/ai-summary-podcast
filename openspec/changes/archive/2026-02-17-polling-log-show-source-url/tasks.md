## 1. Update log messages

- [x] 1.1 Replace `source.id` with `source.url` in all log statements in `SourcePoller.kt`

## 2. Update tests

- [x] 2.1 Update any tests that assert on polling log messages to expect `source.url` instead of `source.id` (no test changes needed — no tests assert on log content)
