package com.aisummarypodcast.publishing

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.EncryptionProperties
import com.aisummarypodcast.config.FeedProperties
import com.aisummarypodcast.config.SoundCloudProperties
import com.aisummarypodcast.store.ApiKeyCategory
import com.aisummarypodcast.user.ProviderConfig
import com.aisummarypodcast.user.UserProviderConfigService
import tools.jackson.databind.json.JsonMapper
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SoundCloudCredentialResolverTest {

    private val providerConfigService = mockk<UserProviderConfigService>()
    private val objectMapper = JsonMapper()

    private fun resolver(clientId: String? = null, clientSecret: String? = null, baseUrl: String = "http://localhost:8085") =
        SoundCloudCredentialResolver(
            providerConfigService,
            mockk<AppProperties> {
                every { soundcloud } returns SoundCloudProperties(clientId, clientSecret)
                every { feed } returns FeedProperties(baseUrl = baseUrl)
            },
            objectMapper
        )

    @Test
    fun `resolve returns DB credentials when available`() {
        val json = """{"clientId":"db-id","clientSecret":"db-secret","callbackUri":"https://example.com/callback"}"""
        every { providerConfigService.resolveConfig("user1", ApiKeyCategory.PUBLISHING, "soundcloud") } returns
            ProviderConfig(baseUrl = "", apiKey = json)

        val result = resolver().resolve("user1")

        assertEquals("db-id", result.clientId)
        assertEquals("db-secret", result.clientSecret)
        assertEquals("https://example.com/callback", result.callbackUri)
    }

    @Test
    fun `resolve falls back to env vars when no DB config`() {
        every { providerConfigService.resolveConfig("user1", ApiKeyCategory.PUBLISHING, "soundcloud") } returns null

        val result = resolver(clientId = "env-id", clientSecret = "env-secret", baseUrl = "https://myapp.com").resolve("user1")

        assertEquals("env-id", result.clientId)
        assertEquals("env-secret", result.clientSecret)
        assertEquals("https://myapp.com/oauth/soundcloud/callback", result.callbackUri)
    }

    @Test
    fun `resolve throws when no credentials available`() {
        every { providerConfigService.resolveConfig("user1", ApiKeyCategory.PUBLISHING, "soundcloud") } returns null

        assertThrows<IllegalStateException> {
            resolver().resolve("user1")
        }
    }

    @Test
    fun `isConfigured returns true when credentials exist`() {
        val json = """{"clientId":"id","clientSecret":"secret","callbackUri":"https://example.com/cb"}"""
        every { providerConfigService.resolveConfig("user1", ApiKeyCategory.PUBLISHING, "soundcloud") } returns
            ProviderConfig(baseUrl = "", apiKey = json)

        assertTrue(resolver().isConfigured("user1"))
    }

    @Test
    fun `isConfigured returns false when no credentials`() {
        every { providerConfigService.resolveConfig("user1", ApiKeyCategory.PUBLISHING, "soundcloud") } returns null

        assertFalse(resolver().isConfigured("user1"))
    }
}
