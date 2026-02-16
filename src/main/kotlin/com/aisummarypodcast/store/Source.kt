package com.aisummarypodcast.store

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Table

@Table("sources")
data class Source(
    @Id val id: String,
    val podcastId: String,
    val type: String,
    val url: String,
    val pollIntervalMinutes: Int = 60,
    val enabled: Boolean = true,
    val lastPolled: String? = null,
    val lastSeenId: String? = null,
    val aggregate: Boolean? = null,
    @Version val version: Long? = null
)
