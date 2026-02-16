package com.aisummarypodcast.store

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("posts")
data class Post(
    @Id val id: Long? = null,
    val sourceId: String,
    val title: String,
    val body: String,
    val url: String,
    val publishedAt: String? = null,
    val author: String? = null,
    val contentHash: String,
    val createdAt: String
)
