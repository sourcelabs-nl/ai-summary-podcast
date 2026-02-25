## Why

Inworld TTS chunk generation is sequential — each chunk waits for the previous one to complete before starting. For a typical episode (8000-15000 chars, 4-8 chunks at 2000 chars), this means total generation time scales linearly with chunk count. Parallelizing chunk generation can reduce TTS time by 4-8x. Inworld's rate limit of 100 requests/second makes this safe.

## What Changes

- Parallelize Inworld TTS chunk generation using Kotlin coroutines (`async`/`awaitAll` on `Dispatchers.IO`)
- For monologue: fire all chunks in parallel, reassemble in order
- For dialogue: flatten all turn chunks into a single list, fire all in parallel, reassemble in order
- Add reactive retry with exponential backoff on HTTP 429 responses (3 attempts, 1s/2s/4s delays)
- Update the existing 429 error handling in `InworldApiClient` to support retryable responses instead of immediately throwing

## Capabilities

### New Capabilities

_None — this is an internal optimization of the existing Inworld TTS provider._

### Modified Capabilities

- `inworld-tts`: Chunk generation changes from sequential to parallel; 429 errors are retried instead of thrown immediately

## Impact

- **Code**: `InworldTtsProvider` (parallel coroutines), `InworldApiClient` (retry-on-429 or return retryable result)
- **Dependencies**: Kotlin coroutines (already available via Spring Boot / kotlinx-coroutines)
- **APIs**: No external API changes
- **Risk**: Low — same chunks, same API calls, just concurrent. Retry logic adds resilience.
