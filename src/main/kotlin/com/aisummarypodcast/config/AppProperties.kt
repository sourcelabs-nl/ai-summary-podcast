package com.aisummarypodcast.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val llm: LlmProperties,
    val briefing: BriefingProperties,
    val episodes: EpisodesProperties,
    val feed: FeedProperties,
    val encryption: EncryptionProperties
)

data class EncryptionProperties(
    val masterKey: String
)

data class LlmProperties(
    val cheapModel: String = "anthropic/claude-3-haiku"
)

data class BriefingProperties(
    val cron: String = "0 0 6 * * *",
    val targetWords: Int = 1500
)

data class EpisodesProperties(
    val directory: String = "./data/episodes",
    val retentionDays: Int = 30
)

data class FeedProperties(
    val baseUrl: String = "http://localhost:8080",
    val title: String = "AI Summary Podcast",
    val description: String = "AI-generated audio briefings from your favourite content sources"
)
