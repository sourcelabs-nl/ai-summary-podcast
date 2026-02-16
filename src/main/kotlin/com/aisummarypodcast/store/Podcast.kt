package com.aisummarypodcast.store

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Table

@Table("podcasts")
data class Podcast(
    @Id val id: String,
    val userId: String,
    val name: String,
    val topic: String,
    val llmModels: Map<String, String>? = null,
    val language: String = "en",
    val ttsVoice: String = "nova",
    val ttsSpeed: Double = 1.0,
    val style: String = "news-briefing",
    val targetWords: Int? = null,
    val cron: String = "0 0 6 * * *",
    val customInstructions: String? = null,
    val relevanceThreshold: Int = 5,
    val requireReview: Boolean = false,
    val maxLlmCostCents: Int? = null,
    val maxArticleAgeDays: Int? = null,
    val soundcloudPlaylistId: String? = null,
    val lastGeneratedAt: String? = null,
    @Version val version: Long? = null
)
