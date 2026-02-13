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
    val source: SourceProperties = SourceProperties(),
    val tts: TtsProperties = TtsProperties()
)

data class EncryptionProperties(
    val masterKey: String
)

data class LlmProperties(
    val models: Map<String, ModelDefinition> = emptyMap(),
    val defaults: StageDefaults = StageDefaults(),
    val summarizationMinWords: Int = 500
)

data class ModelDefinition(
    val provider: String,
    val model: String,
    val inputCostPerMtok: Double? = null,
    val outputCostPerMtok: Double? = null
)

data class StageDefaults(
    val filter: String = "cheap",
    val compose: String = "capable"
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
    val description: String = "AI-generated audio briefings from your favourite content sources",
    val staticBaseUrl: String? = null
)

data class LlmCacheProperties(
    val maxAgeDays: Int? = null
)

data class SourceProperties(
    val maxArticleAgeDays: Int = 7
)

data class TtsProperties(
    val costPerMillionChars: Double? = null
)
