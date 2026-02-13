package com.aisummarypodcast.publishing

import com.aisummarypodcast.config.ApiKeyEncryptor
import com.aisummarypodcast.store.OAuthConnection
import com.aisummarypodcast.store.OAuthConnectionRepository
import org.springframework.stereotype.Service
import java.time.Instant

data class DecryptedOAuthConnection(
    val userId: String,
    val provider: String,
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: Instant?,
    val scopes: String?,
    val createdAt: String,
    val updatedAt: String
)

data class OAuthConnectionStatus(
    val connected: Boolean,
    val scopes: String? = null,
    val connectedAt: String? = null
)

@Service
class OAuthConnectionService(
    private val repository: OAuthConnectionRepository,
    private val encryptor: ApiKeyEncryptor
) {

    fun storeConnection(
        userId: String,
        provider: String,
        accessToken: String,
        refreshToken: String?,
        expiresAt: Instant?,
        scopes: String?
    ) {
        val now = Instant.now().toString()
        val existing = repository.findByUserIdAndProvider(userId, provider)

        val connection = OAuthConnection(
            id = existing?.id,
            userId = userId,
            provider = provider,
            encryptedAccessToken = encryptor.encrypt(accessToken),
            encryptedRefreshToken = refreshToken?.let { encryptor.encrypt(it) },
            expiresAt = expiresAt?.toString(),
            scopes = scopes,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        repository.save(connection)
    }

    fun getConnection(userId: String, provider: String): DecryptedOAuthConnection? {
        val connection = repository.findByUserIdAndProvider(userId, provider) ?: return null
        return DecryptedOAuthConnection(
            userId = connection.userId,
            provider = connection.provider,
            accessToken = encryptor.decrypt(connection.encryptedAccessToken),
            refreshToken = connection.encryptedRefreshToken?.let { encryptor.decrypt(it) },
            expiresAt = connection.expiresAt?.let { Instant.parse(it) },
            scopes = connection.scopes,
            createdAt = connection.createdAt,
            updatedAt = connection.updatedAt
        )
    }

    fun deleteConnection(userId: String, provider: String): Boolean {
        return repository.deleteByUserIdAndProvider(userId, provider) > 0
    }

    fun deleteByUserId(userId: String) {
        repository.deleteByUserId(userId)
    }

    fun getStatus(userId: String, provider: String): OAuthConnectionStatus {
        val connection = repository.findByUserIdAndProvider(userId, provider)
        return if (connection != null) {
            OAuthConnectionStatus(
                connected = true,
                scopes = connection.scopes,
                connectedAt = connection.createdAt
            )
        } else {
            OAuthConnectionStatus(connected = false)
        }
    }
}
