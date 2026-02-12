package com.aisummarypodcast.store

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("episodes")
data class Episode(
    @Id val id: Long? = null,
    val podcastId: String,
    val generatedAt: String,
    val scriptText: String,
    val status: String = "GENERATED",
    val audioFilePath: String? = null,
    val durationSeconds: Int? = null
)
