package com.aisummarypodcast.user

import com.aisummarypodcast.config.ApiKeyEncryptor
import com.aisummarypodcast.store.ApiKeyCategory
import com.aisummarypodcast.store.UserProviderConfig
import com.aisummarypodcast.store.UserProviderConfigRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UserProviderConfigServiceTest {

    private val repository = mockk<UserProviderConfigRepository>()
    private val encryptor = mockk<ApiKeyEncryptor>()
    private val service = UserProviderConfigService(repository, encryptor)

    private val userId = "user-1"

    @Test
    fun `resolveConfig returns decrypted key and stored base URL when present`() {
        val config = UserProviderConfig(userId, "openrouter", ApiKeyCategory.LLM, "https://custom.api/v1", "encrypted-value")
        every { repository.findByUserIdAndCategory(userId, ApiKeyCategory.LLM) } returns config
        every { encryptor.decrypt("encrypted-value") } returns "sk-real-key"

        val result = service.resolveConfig(userId, ApiKeyCategory.LLM)

        assertEquals("sk-real-key", result?.apiKey)
        assertEquals("https://custom.api/v1", result?.baseUrl)
    }

    @Test
    fun `resolveConfig uses provider default URL when base URL is null`() {
        val config = UserProviderConfig(userId, "openrouter", ApiKeyCategory.LLM, null, "encrypted-value")
        every { repository.findByUserIdAndCategory(userId, ApiKeyCategory.LLM) } returns config
        every { encryptor.decrypt("encrypted-value") } returns "sk-real-key"

        val result = service.resolveConfig(userId, ApiKeyCategory.LLM)

        assertEquals("sk-real-key", result?.apiKey)
        assertEquals("https://openrouter.ai/api", result?.baseUrl)
    }

    @Test
    fun `resolveConfig returns null apiKey for Ollama provider with no key`() {
        val config = UserProviderConfig(userId, "ollama", ApiKeyCategory.LLM, null, null)
        every { repository.findByUserIdAndCategory(userId, ApiKeyCategory.LLM) } returns config

        val result = service.resolveConfig(userId, ApiKeyCategory.LLM)

        assertNull(result?.apiKey)
        assertEquals("http://localhost:11434", result?.baseUrl)
    }

    @Test
    fun `resolveConfig queries repository when no user config exists`() {
        every { repository.findByUserIdAndCategory(userId, ApiKeyCategory.LLM) } returns null

        service.resolveConfig(userId, ApiKeyCategory.LLM)

        verify { repository.findByUserIdAndCategory(userId, ApiKeyCategory.LLM) }
    }

    @Test
    fun `setConfig encrypts key and saves with category and base URL`() {
        every { encryptor.encrypt("sk-123") } returns "encrypted-123"
        every { repository.save(any()) } returns Unit

        service.setConfig(userId, ApiKeyCategory.TTS, "openai", "sk-123", "https://custom.openai.com")

        verify {
            repository.save(
                UserProviderConfig(
                    userId = userId,
                    provider = "openai",
                    category = ApiKeyCategory.TTS,
                    baseUrl = "https://custom.openai.com",
                    encryptedApiKey = "encrypted-123"
                )
            )
        }
    }

    @Test
    fun `setConfig saves null encrypted key when apiKey is null`() {
        every { repository.save(any()) } returns Unit

        service.setConfig(userId, ApiKeyCategory.LLM, "ollama", null, null)

        verify {
            repository.save(
                UserProviderConfig(
                    userId = userId,
                    provider = "ollama",
                    category = ApiKeyCategory.LLM,
                    baseUrl = null,
                    encryptedApiKey = null
                )
            )
        }
    }

    @Test
    fun `deleteConfig delegates to repository by category`() {
        every { repository.deleteByUserIdAndCategory(userId, ApiKeyCategory.LLM) } returns 1

        val result = service.deleteConfig(userId, ApiKeyCategory.LLM)

        assertEquals(true, result)
    }

    @Test
    fun `deleteConfig returns false when nothing deleted`() {
        every { repository.deleteByUserIdAndCategory(userId, ApiKeyCategory.TTS) } returns 0

        val result = service.deleteConfig(userId, ApiKeyCategory.TTS)

        assertEquals(false, result)
    }

    @Test
    fun `listConfigs returns all configs for user`() {
        val configs = listOf(
            UserProviderConfig(userId, "openrouter", ApiKeyCategory.LLM, null, "enc1"),
            UserProviderConfig(userId, "openai", ApiKeyCategory.TTS, null, "enc2")
        )
        every { repository.findByUserId(userId) } returns configs

        val result = service.listConfigs(userId)

        assertEquals(2, result.size)
        assertEquals(ApiKeyCategory.LLM, result[0].category)
        assertEquals(ApiKeyCategory.TTS, result[1].category)
    }

    @Test
    fun `hasDefaultUrl returns true for known providers`() {
        assertEquals(true, service.hasDefaultUrl("openrouter"))
        assertEquals(true, service.hasDefaultUrl("openai"))
        assertEquals(true, service.hasDefaultUrl("ollama"))
    }

    @Test
    fun `hasDefaultUrl returns false for unknown providers`() {
        assertEquals(false, service.hasDefaultUrl("azure"))
        assertEquals(false, service.hasDefaultUrl("custom"))
    }

    @Test
    fun `hasDefaultUrl is case-insensitive`() {
        assertEquals(true, service.hasDefaultUrl("OpenRouter"))
        assertEquals(true, service.hasDefaultUrl("OLLAMA"))
    }
}
