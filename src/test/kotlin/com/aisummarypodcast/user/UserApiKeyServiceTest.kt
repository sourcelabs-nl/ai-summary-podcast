package com.aisummarypodcast.user

import com.aisummarypodcast.config.ApiKeyEncryptor
import com.aisummarypodcast.store.ApiKeyCategory
import com.aisummarypodcast.store.UserApiKey
import com.aisummarypodcast.store.UserApiKeyRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UserApiKeyServiceTest {

    private val repository = mockk<UserApiKeyRepository>()
    private val encryptor = mockk<ApiKeyEncryptor>()
    private val service = UserApiKeyService(repository, encryptor)

    private val userId = "user-1"

    @Test
    fun `resolveKey returns decrypted user key when present`() {
        val userKey = UserApiKey(userId, "openrouter", ApiKeyCategory.LLM, "encrypted-value")
        every { repository.findByUserIdAndCategory(userId, ApiKeyCategory.LLM) } returns userKey
        every { encryptor.decrypt("encrypted-value") } returns "sk-real-key"

        val result = service.resolveKey(userId, ApiKeyCategory.LLM)

        assertEquals("sk-real-key", result)
    }

    @Test
    fun `resolveKey queries repository when no user key exists`() {
        every { repository.findByUserIdAndCategory(userId, ApiKeyCategory.LLM) } returns null

        service.resolveKey(userId, ApiKeyCategory.LLM)

        verify { repository.findByUserIdAndCategory(userId, ApiKeyCategory.LLM) }
    }

    @Test
    fun `setKey encrypts and saves with category`() {
        every { encryptor.encrypt("sk-123") } returns "encrypted-123"
        every { repository.save(any()) } returns Unit

        service.setKey(userId, ApiKeyCategory.TTS, "openai", "sk-123")

        verify {
            repository.save(
                UserApiKey(userId = userId, provider = "openai", category = ApiKeyCategory.TTS, encryptedApiKey = "encrypted-123")
            )
        }
    }

    @Test
    fun `setKey replaces existing key for same category`() {
        every { encryptor.encrypt("sk-first") } returns "encrypted-first"
        every { encryptor.encrypt("sk-second") } returns "encrypted-second"
        every { repository.save(any()) } returns Unit

        service.setKey(userId, ApiKeyCategory.LLM, "openrouter", "sk-first")
        service.setKey(userId, ApiKeyCategory.LLM, "other-provider", "sk-second")

        verify(exactly = 2) { repository.save(match { it.category == ApiKeyCategory.LLM }) }
        verify {
            repository.save(
                UserApiKey(userId = userId, provider = "other-provider", category = ApiKeyCategory.LLM, encryptedApiKey = "encrypted-second")
            )
        }
    }

    @Test
    fun `deleteKey delegates to repository by category`() {
        every { repository.deleteByUserIdAndCategory(userId, ApiKeyCategory.LLM) } returns 1

        val result = service.deleteKey(userId, ApiKeyCategory.LLM)

        assertEquals(true, result)
    }

    @Test
    fun `deleteKey returns false when nothing deleted`() {
        every { repository.deleteByUserIdAndCategory(userId, ApiKeyCategory.TTS) } returns 0

        val result = service.deleteKey(userId, ApiKeyCategory.TTS)

        assertEquals(false, result)
    }

    @Test
    fun `listKeys returns all keys for user`() {
        val keys = listOf(
            UserApiKey(userId, "openrouter", ApiKeyCategory.LLM, "enc1"),
            UserApiKey(userId, "openai", ApiKeyCategory.TTS, "enc2")
        )
        every { repository.findByUserId(userId) } returns keys

        val result = service.listKeys(userId)

        assertEquals(2, result.size)
        assertEquals(ApiKeyCategory.LLM, result[0].category)
        assertEquals(ApiKeyCategory.TTS, result[1].category)
    }
}
