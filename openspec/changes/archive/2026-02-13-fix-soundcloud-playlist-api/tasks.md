## 1. RestTemplate Configuration

- [x] 1.1 Inject `RestTemplateBuilder` into `SoundCloudClient` and use `.build()` instead of `RestTemplate()`

## 2. Track ID Serialization Fix

- [x] 2.1 Fix `createPlaylist` to serialize track IDs as strings (`it.toString()`) in the JSON body
- [x] 2.2 Fix `addTrackToPlaylist` to serialize track IDs as strings (`it.toString()`) in the JSON body
