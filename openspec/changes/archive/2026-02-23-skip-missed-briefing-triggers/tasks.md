## 1. Core Implementation

- [x] 1.1 Add `STALENESS_WINDOW` constant (30 minutes) to `BriefingGenerationScheduler`
- [x] 1.2 Add fast-forward loop in `checkAndGenerate()` to skip stale triggers, logging each skip at WARN level

## 2. Tests

- [x] 2.1 Test that a trigger within the staleness window (e.g. 10 minutes past) still fires
- [x] 2.2 Test that a trigger beyond the staleness window (e.g. 3 hours past) is skipped
- [x] 2.3 Test that multiple missed triggers are all skipped and the scheduler advances to the next future trigger
- [x] 2.4 Test that `lastGeneratedAt` is not modified when triggers are skipped
