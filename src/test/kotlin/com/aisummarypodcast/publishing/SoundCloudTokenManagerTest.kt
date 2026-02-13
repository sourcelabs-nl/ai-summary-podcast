package com.aisummarypodcast.publishing

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.SoundCloudProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class SoundCloudTokenManagerTest {

    private val oauthConnectionService = mockk<OAuthConnectionService>(relaxed = true)
    private val soundCloudClient = mockk<SoundCloudClient>()
    private val appProperties = mockk<AppProperties> {
        every { soundcloud } returns SoundCloudProperties(clientId = "test-id", clientSecret = "test-secret")
    }
    private val tokenManager = SoundCloudTokenManager(oauthConnectionService, soundCloudClient, appProperties)

    @Test
    fun `getValidAccessToken returns existing token when not near expiry`() {
        val connection = DecryptedOAuthConnection(
            userId = "user1",
            provider = "soundcloud",
            accessToken = "valid-token",
            refreshToken = "refresh",
            expiresAt = Instant.now().plusSeconds(3600),
            scopes = "non-expiring",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-02-01T00:00:00Z"
        )
        every { oauthConnectionService.getConnection("user1", "soundcloud") } returns connection

        val token = tokenManager.getValidAccessToken("user1")

        assertEquals("valid-token", token)
    }

    @Test
    fun `getValidAccessToken refreshes token when near expiry`() {
        val connection = DecryptedOAuthConnection(
            userId = "user1",
            provider = "soundcloud",
            accessToken = "old-token",
            refreshToken = "refresh-token",
            expiresAt = Instant.now().plusSeconds(60),
            scopes = "non-expiring",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-02-01T00:00:00Z"
        )
        every { oauthConnectionService.getConnection("user1", "soundcloud") } returns connection
        every {
            soundCloudClient.refreshAccessToken("refresh-token", "test-id", "test-secret")
        } returns SoundCloudTokenResponse(
            accessToken = "new-token",
            refreshToken = "new-refresh",
            expiresIn = 3600
        )

        val token = tokenManager.getValidAccessToken("user1")

        assertEquals("new-token", token)
        verify {
            oauthConnectionService.storeConnection(
                userId = "user1",
                provider = "soundcloud",
                accessToken = "new-token",
                refreshToken = "new-refresh",
                expiresAt = any(),
                scopes = any()
            )
        }
    }

    @Test
    fun `getValidAccessToken throws when no connection found`() {
        every { oauthConnectionService.getConnection("user1", "soundcloud") } returns null

        val ex = assertThrows<IllegalStateException> {
            tokenManager.getValidAccessToken("user1")
        }
        assertTrue(ex.message!!.contains("No SoundCloud connection"))
    }

    @Test
    fun `getValidAccessToken throws when refresh fails and no refresh token`() {
        val connection = DecryptedOAuthConnection(
            userId = "user1",
            provider = "soundcloud",
            accessToken = "old-token",
            refreshToken = null,
            expiresAt = Instant.now().minusSeconds(60),
            scopes = null,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-02-01T00:00:00Z"
        )
        every { oauthConnectionService.getConnection("user1", "soundcloud") } returns connection

        assertThrows<IllegalStateException> {
            tokenManager.getValidAccessToken("user1")
        }
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.jupiter.api.Assertions.assertTrue(condition)
    }
}
