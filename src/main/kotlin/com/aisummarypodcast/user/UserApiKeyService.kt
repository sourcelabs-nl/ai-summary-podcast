package com.aisummarypodcast.user

import com.aisummarypodcast.config.ApiKeyEncryptor
import com.aisummarypodcast.store.UserApiKey
import com.aisummarypodcast.store.UserApiKeyRepository
import org.springframework.stereotype.Service

@Service
class UserApiKeyService(
    private val userApiKeyRepository: UserApiKeyRepository,
    private val apiKeyEncryptor: ApiKeyEncryptor
) {

    fun setKey(userId: String, provider: String, apiKey: String) {
        val encrypted = apiKeyEncryptor.encrypt(apiKey)
        userApiKeyRepository.save(UserApiKey(userId = userId, provider = provider, encryptedApiKey = encrypted))
    }

    fun listProviders(userId: String): List<String> =
        userApiKeyRepository.findByUserId(userId).map { it.provider }

    fun deleteKey(userId: String, provider: String): Boolean =
        userApiKeyRepository.deleteByUserIdAndProvider(userId, provider) > 0

    fun deleteAllByUserId(userId: String) =
        userApiKeyRepository.deleteByUserId(userId)

    fun resolveKey(userId: String, provider: String): String? {
        val userKey = userApiKeyRepository.findByUserIdAndProvider(userId, provider)
        if (userKey != null) {
            return apiKeyEncryptor.decrypt(userKey.encryptedApiKey)
        }
        return globalKeyForProvider(provider)
    }

    private fun globalKeyForProvider(provider: String): String? = when (provider) {
        "openrouter" -> System.getenv("OPENROUTER_API_KEY")
        "openai" -> System.getenv("OPENAI_API_KEY")
        else -> null
    }
}
