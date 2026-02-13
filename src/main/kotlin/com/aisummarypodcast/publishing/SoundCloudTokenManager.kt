package com.aisummarypodcast.publishing

import com.aisummarypodcast.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SoundCloudTokenManager(
    private val oauthConnectionService: OAuthConnectionService,
    private val soundCloudClient: SoundCloudClient,
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun getValidAccessToken(userId: String): String {
        val connection = oauthConnectionService.getConnection(userId, "soundcloud")
            ?: throw IllegalStateException("No SoundCloud connection found for user $userId. Please connect your SoundCloud account first.")

        val expiresAt = connection.expiresAt
        if (expiresAt != null && expiresAt.isBefore(Instant.now().plusSeconds(300))) {
            return refreshToken(userId, connection)
        }

        return connection.accessToken
    }

    private fun refreshToken(userId: String, connection: DecryptedOAuthConnection): String {
        val refreshToken = connection.refreshToken
            ?: throw IllegalStateException("SoundCloud connection for user $userId has no refresh token. Please re-authorize.")

        return try {
            log.info("Proactively refreshing SoundCloud token for user {}", userId)
            val tokenResponse = soundCloudClient.refreshAccessToken(
                refreshToken = refreshToken,
                clientId = appProperties.soundcloud.clientId!!,
                clientSecret = appProperties.soundcloud.clientSecret!!
            )

            val newExpiresAt = tokenResponse.expiresIn?.let { Instant.now().plusSeconds(it) }

            oauthConnectionService.storeConnection(
                userId = userId,
                provider = "soundcloud",
                accessToken = tokenResponse.accessToken,
                refreshToken = tokenResponse.refreshToken ?: refreshToken,
                expiresAt = newExpiresAt,
                scopes = tokenResponse.scope ?: connection.scopes
            )

            tokenResponse.accessToken
        } catch (e: Exception) {
            log.error("Failed to refresh SoundCloud token for user {}: {}", userId, e.message, e)
            throw IllegalStateException("SoundCloud token refresh failed. Please re-authorize your SoundCloud account.", e)
        }
    }
}
