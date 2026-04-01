package com.aisummarypodcast.source

import com.aisummarypodcast.store.Source

internal fun Source.toResponse() = SourceResponse(
    id = id, podcastId = podcastId, type = type.value, url = url,
    pollIntervalMinutes = pollIntervalMinutes, enabled = enabled, aggregate = aggregate,
    maxFailures = maxFailures, maxBackoffHours = maxBackoffHours, pollDelaySeconds = pollDelaySeconds,
    categoryFilter = categoryFilter, label = label, createdAt = createdAt, lastPolled = lastPolled, lastSeenId = lastSeenId,
    consecutiveFailures = consecutiveFailures, lastFailureType = lastFailureType,
    disabledReason = disabledReason
)
