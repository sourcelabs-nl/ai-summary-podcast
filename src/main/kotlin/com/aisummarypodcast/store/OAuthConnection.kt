package com.aisummarypodcast.store

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("oauth_connections")
data class OAuthConnection(
    @Id val id: Long? = null,
    val userId: String,
    val provider: String,
    val encryptedAccessToken: String,
    val encryptedRefreshToken: String? = null,
    val expiresAt: String? = null,
    val scopes: String? = null,
    val createdAt: String,
    val updatedAt: String
)
