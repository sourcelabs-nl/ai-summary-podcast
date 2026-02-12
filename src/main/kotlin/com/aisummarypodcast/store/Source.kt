package com.aisummarypodcast.store

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("sources")
data class Source(
    @Id val id: String,
    val type: String,
    val url: String,
    val lastPolled: String? = null,
    val lastSeenId: String? = null
)
