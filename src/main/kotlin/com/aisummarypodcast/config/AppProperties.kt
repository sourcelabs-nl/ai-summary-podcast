package com.aisummarypodcast.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val llm: LlmProperties,
    val briefing: BriefingProperties,
    val episodes: EpisodesProperties,
    val feed: FeedProperties,
    val encryption: EncryptionProperties,
    val llmCache: LlmCacheProperties = LlmCacheProperties(),
    val source: SourceProperties = SourceProperties()
)

data class EncryptionProperties(
    val masterKey: String
)

data class LlmProperties(
    val cheapModel: String = "anthropic/claude-3-haiku"
)

data class BriefingProperties(
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

data class LlmCacheProperties(
    val maxAgeDays: Int? = null
)

data class SourceProperties(
    val maxArticleAgeDays: Int = 7
)
