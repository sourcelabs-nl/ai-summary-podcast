package com.aisummarypodcast.store

import org.springframework.data.relational.core.mapping.Table

enum class ApiKeyCategory {
    LLM, TTS
}

@Table("user_provider_configs")
data class UserProviderConfig(
    val userId: String,
    val provider: String,
    val category: ApiKeyCategory,
    val baseUrl: String? = null,
    val encryptedApiKey: String? = null
)
