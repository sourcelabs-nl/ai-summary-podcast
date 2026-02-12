package com.aisummarypodcast.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.sources")
data class SourceProperties(
    val topic: String,
    val entries: List<SourceEntry> = emptyList()
)

data class SourceEntry(
    val id: String,
    val type: String,
    val url: String,
    val pollIntervalMinutes: Int = 60,
    val enabled: Boolean = true
)
