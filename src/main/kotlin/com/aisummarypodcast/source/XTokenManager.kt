package com.aisummarypodcast.source

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.publishing.OAuthConnectionService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class XTokenManager(
    private val oauthConnectionService: OAuthConnectionService,
    private val xClient: XClient,
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun getValidAccessToken(userId: String): String {
        val connection = oauthConnectionService.getConnection(userId, "x")
            ?: throw IllegalStateException("No X connection found for user $userId. Please connect your X account first.")

        val expiresAt = connection.expiresAt
        if (expiresAt != null && expiresAt.isBefore(Instant.now().plusSeconds(300))) {
            return refreshToken(userId, connection)
        }

        return connection.accessToken
    }

    private fun refreshToken(userId: String, connection: com.aisummarypodcast.publishing.DecryptedOAuthConnection): String {
        val refreshToken = connection.refreshToken
            ?: throw IllegalStateException("X connection for user $userId has no refresh token. Please re-authorize.")

        return try {
            log.info("Proactively refreshing X token for user {}", userId)
            val tokenResponse = xClient.refreshAccessToken(
                refreshToken = refreshToken,
                clientId = appProperties.x.clientId!!,
                clientSecret = appProperties.x.clientSecret!!
            )

            val newExpiresAt = tokenResponse.expiresIn?.let { Instant.now().plusSeconds(it) }

            oauthConnectionService.storeConnection(
                userId = userId,
                provider = "x",
                accessToken = tokenResponse.accessToken,
                refreshToken = tokenResponse.refreshToken ?: refreshToken,
                expiresAt = newExpiresAt,
                scopes = tokenResponse.scope ?: connection.scopes
            )

            tokenResponse.accessToken
        } catch (e: Exception) {
            log.error("Failed to refresh X token for user {}: {}", userId, e.message, e)
            throw IllegalStateException("X token refresh failed. Please re-authorize your X account.", e)
        }
    }
}
