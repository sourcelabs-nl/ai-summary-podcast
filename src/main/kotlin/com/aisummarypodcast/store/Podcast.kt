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
    val ttsProvider: TtsProviderType = TtsProviderType.OPENAI,
    val ttsVoices: Map<String, String>? = null,
    val ttsSettings: Map<String, String>? = null,
    val style: PodcastStyle = PodcastStyle.NEWS_BRIEFING,
    val targetWords: Int? = null,
    val cron: String = "0 0 6 * * *",
    val customInstructions: String? = null,
    val relevanceThreshold: Int = 5,
    val requireReview: Boolean = false,
    val maxLlmCostCents: Int? = null,
    val maxArticleAgeDays: Int? = null,
    val speakerNames: Map<String, String>? = null,
    val fullBodyThreshold: Int? = null,
    val sponsor: Map<String, String>? = null,
    val soundcloudPlaylistId: String? = null,
    val lastGeneratedAt: String? = null,
    @Version val version: Long? = null
)
