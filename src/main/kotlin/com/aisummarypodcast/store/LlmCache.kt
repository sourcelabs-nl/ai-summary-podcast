package com.aisummarypodcast.store

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("llm_cache")
data class LlmCache(
    @Id val id: Long? = null,
    val promptHash: String,
    val model: String,
    val response: String,
    val createdAt: String
)
