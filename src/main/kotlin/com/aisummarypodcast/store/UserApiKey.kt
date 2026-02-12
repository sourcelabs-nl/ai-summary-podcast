package com.aisummarypodcast.store

import org.springframework.data.relational.core.mapping.Table

@Table("user_api_keys")
data class UserApiKey(
    val userId: String,
    val provider: String,
    val encryptedApiKey: String
)
