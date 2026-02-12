package com.aisummarypodcast.store

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("episodes")
data class Episode(
    @Id val id: Long? = null,
    val generatedAt: String,
    val scriptText: String,
    val audioFilePath: String,
    val durationSeconds: Int
)
