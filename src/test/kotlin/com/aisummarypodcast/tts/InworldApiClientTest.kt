package com.aisummarypodcast.tts

import com.aisummarypodcast.store.ApiKeyCategory
import com.aisummarypodcast.user.ProviderConfig
import com.aisummarypodcast.user.UserProviderConfigService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InworldApiClientTest {

    private val providerConfigService = mockk<UserProviderConfigService>()

    @Test
    fun `buildBasicToken creates base64-encoded token from credentials`() {
        val client = InworldApiClient(providerConfigService, mockk())
        val token = client.buildBasicToken("my-key:my-secret")

        assertNotNull(token)
        val decoded = String(java.util.Base64.getDecoder().decode(token))
        assertEquals("my-key:my-secret", decoded)
    }

    @Test
    fun `createClient throws when no config available`() {
        every {
            providerConfigService.resolveConfig("u1", ApiKeyCategory.TTS, "inworld")
        } returns null

        val client = InworldApiClient(providerConfigService, mockk())

        assertThrows<IllegalStateException> {
            client.createClient("u1")
        }
    }

    @Test
    fun `createClient throws when apiKey is null`() {
        every {
            providerConfigService.resolveConfig("u1", ApiKeyCategory.TTS, "inworld")
        } returns ProviderConfig(baseUrl = "https://api.inworld.ai", apiKey = null)

        val client = InworldApiClient(providerConfigService, mockk())

        assertThrows<IllegalStateException> {
            client.createClient("u1")
        }
    }
}
