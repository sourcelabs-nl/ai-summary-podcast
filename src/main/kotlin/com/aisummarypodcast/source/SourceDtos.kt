package com.aisummarypodcast.source

data class CreateSourceRequest(
    val type: String,
    val url: String,
    val pollIntervalMinutes: Int = 30,
    val enabled: Boolean = true,
    val aggregate: Boolean? = null,
    val maxFailures: Int? = null,
    val maxBackoffHours: Int? = null,
    val pollDelaySeconds: Int? = null,
    val categoryFilter: String? = null,
    val label: String? = null
)

data class UpdateSourceRequest(
    val type: String,
    val url: String,
    val pollIntervalMinutes: Int = 30,
    val enabled: Boolean = true,
    val aggregate: Boolean? = null,
    val maxFailures: Int? = null,
    val maxBackoffHours: Int? = null,
    val pollDelaySeconds: Int? = null,
    val categoryFilter: String? = null,
    val label: String? = null
)

data class SourceResponse(
    val id: String,
    val podcastId: String,
    val type: String,
    val url: String,
    val pollIntervalMinutes: Int,
    val enabled: Boolean,
    val aggregate: Boolean?,
    val maxFailures: Int?,
    val maxBackoffHours: Int?,
    val pollDelaySeconds: Int?,
    val categoryFilter: String?,
    val label: String?,
    val createdAt: String,
    val lastPolled: String?,
    val lastSeenId: String?,
    val consecutiveFailures: Int,
    val lastFailureType: String?,
    val disabledReason: String?,
    val articleCount: Int = 0,
    val relevantArticleCount: Int = 0,
    val postCount: Int = 0
)
