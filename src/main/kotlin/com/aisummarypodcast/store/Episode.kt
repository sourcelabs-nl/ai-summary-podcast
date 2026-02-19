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
    val durationSeconds: Int? = null,
    val filterModel: String? = null,
    val composeModel: String? = null,
    val llmInputTokens: Int? = null,
    val llmOutputTokens: Int? = null,
    val llmCostCents: Int? = null,
    val ttsCharacters: Int? = null,
    val ttsCostCents: Int? = null,
    val ttsModel: String? = null,
    val recap: String? = null
)
