package com.aisummarypodcast.source

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.XProperties
import com.aisummarypodcast.publishing.DecryptedOAuthConnection
import com.aisummarypodcast.publishing.OAuthConnectionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class XTokenManagerTest {

    private val oauthConnectionService = mockk<OAuthConnectionService>(relaxed = true)
    private val xClient = mockk<XClient>()
    private val appProperties = mockk<AppProperties> {
        every { x } returns XProperties(clientId = "test-id", clientSecret = "test-secret")
    }
    private val tokenManager = XTokenManager(oauthConnectionService, xClient, appProperties)

    @Test
    fun `getValidAccessToken returns existing token when not near expiry`() {
        val connection = DecryptedOAuthConnection(
            userId = "user1",
            provider = "x",
            accessToken = "valid-token",
            refreshToken = "refresh",
            expiresAt = Instant.now().plusSeconds(3600),
            scopes = "tweet.read users.read offline.access",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-02-01T00:00:00Z"
        )
        every { oauthConnectionService.getConnection("user1", "x") } returns connection

        val token = tokenManager.getValidAccessToken("user1")

        assertEquals("valid-token", token)
    }

    @Test
    fun `getValidAccessToken refreshes token when near expiry`() {
        val connection = DecryptedOAuthConnection(
            userId = "user1",
            provider = "x",
            accessToken = "old-token",
            refreshToken = "refresh-token",
            expiresAt = Instant.now().plusSeconds(60),
            scopes = "tweet.read users.read offline.access",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-02-01T00:00:00Z"
        )
        every { oauthConnectionService.getConnection("user1", "x") } returns connection
        every {
            xClient.refreshAccessToken("refresh-token", "test-id", "test-secret")
        } returns XTokenResponse(
            accessToken = "new-token",
            refreshToken = "new-refresh",
            expiresIn = 7200
        )

        val token = tokenManager.getValidAccessToken("user1")

        assertEquals("new-token", token)
        verify {
            oauthConnectionService.storeConnection(
                userId = "user1",
                provider = "x",
                accessToken = "new-token",
                refreshToken = "new-refresh",
                expiresAt = any(),
                scopes = any()
            )
        }
    }

    @Test
    fun `getValidAccessToken throws when no connection found`() {
        every { oauthConnectionService.getConnection("user1", "x") } returns null

        val ex = assertThrows<IllegalStateException> {
            tokenManager.getValidAccessToken("user1")
        }
        assert(ex.message!!.contains("No X connection"))
    }

    @Test
    fun `getValidAccessToken throws when no refresh token available`() {
        val connection = DecryptedOAuthConnection(
            userId = "user1",
            provider = "x",
            accessToken = "old-token",
            refreshToken = null,
            expiresAt = Instant.now().minusSeconds(60),
            scopes = null,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-02-01T00:00:00Z"
        )
        every { oauthConnectionService.getConnection("user1", "x") } returns connection

        assertThrows<IllegalStateException> {
            tokenManager.getValidAccessToken("user1")
        }
    }
}
