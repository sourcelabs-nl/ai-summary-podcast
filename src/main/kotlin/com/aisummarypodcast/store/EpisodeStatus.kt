package com.aisummarypodcast.store

enum class EpisodeStatus {
    GENERATING,
    GENERATING_AUDIO,
    GENERATED,
    PENDING_REVIEW,
    APPROVED,
    FAILED,
    DISCARDED
}
