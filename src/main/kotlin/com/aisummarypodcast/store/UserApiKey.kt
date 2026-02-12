package com.aisummarypodcast.store

import org.springframework.data.relational.core.mapping.Table

enum class ApiKeyCategory {
    LLM, TTS
}

@Table("user_api_keys")
data class UserApiKey(
    val userId: String,
    val provider: String,
    val category: ApiKeyCategory,
    val encryptedApiKey: String
)
