package com.aisummarypodcast.user

import com.aisummarypodcast.config.ApiKeyEncryptor
import com.aisummarypodcast.store.ApiKeyCategory
import com.aisummarypodcast.store.UserApiKey
import com.aisummarypodcast.store.UserApiKeyRepository
import org.springframework.stereotype.Service

@Service
class UserApiKeyService(
    private val userApiKeyRepository: UserApiKeyRepository,
    private val apiKeyEncryptor: ApiKeyEncryptor
) {

    fun setKey(userId: String, category: ApiKeyCategory, provider: String, apiKey: String) {
        val encrypted = apiKeyEncryptor.encrypt(apiKey)
        userApiKeyRepository.save(
            UserApiKey(userId = userId, provider = provider, category = category, encryptedApiKey = encrypted)
        )
    }

    fun listKeys(userId: String): List<UserApiKey> =
        userApiKeyRepository.findByUserId(userId)

    fun deleteKey(userId: String, category: ApiKeyCategory): Boolean =
        userApiKeyRepository.deleteByUserIdAndCategory(userId, category) > 0

    fun deleteAllByUserId(userId: String) =
        userApiKeyRepository.deleteByUserId(userId)

    fun resolveKey(userId: String, category: ApiKeyCategory): String? {
        val userKey = userApiKeyRepository.findByUserIdAndCategory(userId, category)
        if (userKey != null) {
            return apiKeyEncryptor.decrypt(userKey.encryptedApiKey)
        }
        return globalKeyForCategory(category)
    }

    private fun globalKeyForCategory(category: ApiKeyCategory): String? = when (category) {
        ApiKeyCategory.LLM -> System.getenv("OPENROUTER_API_KEY")
        ApiKeyCategory.TTS -> System.getenv("OPENAI_API_KEY")
    }
}
