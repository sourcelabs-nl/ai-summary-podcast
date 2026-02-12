package com.aisummarypodcast.user

import com.aisummarypodcast.config.ApiKeyEncryptor
import com.aisummarypodcast.store.ApiKeyCategory
import com.aisummarypodcast.store.UserProviderConfig
import com.aisummarypodcast.store.UserProviderConfigRepository
import org.springframework.stereotype.Service

data class ProviderConfig(val baseUrl: String, val apiKey: String?)

@Service
class UserProviderConfigService(
    private val repository: UserProviderConfigRepository,
    private val apiKeyEncryptor: ApiKeyEncryptor
) {

    companion object {
        val PROVIDER_DEFAULT_URLS = mapOf(
            "openrouter" to "https://openrouter.ai/api",
            "openai" to "https://api.openai.com",
            "ollama" to "http://localhost:11434"
        )
    }

    fun setConfig(userId: String, category: ApiKeyCategory, provider: String, apiKey: String?, baseUrl: String?) {
        val encrypted = apiKey?.let { apiKeyEncryptor.encrypt(it) }
        repository.save(
            UserProviderConfig(
                userId = userId,
                provider = provider,
                category = category,
                baseUrl = baseUrl,
                encryptedApiKey = encrypted
            )
        )
    }

    fun listConfigs(userId: String): List<UserProviderConfig> =
        repository.findByUserId(userId)

    fun deleteConfig(userId: String, category: ApiKeyCategory): Boolean =
        repository.deleteByUserIdAndCategory(userId, category) > 0

    fun deleteAllByUserId(userId: String) =
        repository.deleteByUserId(userId)

    fun resolveConfig(userId: String, category: ApiKeyCategory): ProviderConfig? {
        val config = repository.findByUserIdAndCategory(userId, category)
        if (config != null) {
            val apiKey = config.encryptedApiKey?.let { apiKeyEncryptor.decrypt(it) }
            val baseUrl = config.baseUrl ?: resolveDefaultUrl(config.provider)
            return ProviderConfig(baseUrl = baseUrl, apiKey = apiKey)
        }
        return globalFallbackForCategory(category)
    }

    fun resolveDefaultUrl(provider: String): String =
        PROVIDER_DEFAULT_URLS[provider.lowercase()]
            ?: throw IllegalStateException("No default base URL for provider '$provider'")

    fun hasDefaultUrl(provider: String): Boolean =
        provider.lowercase() in PROVIDER_DEFAULT_URLS

    private fun globalFallbackForCategory(category: ApiKeyCategory): ProviderConfig? = when (category) {
        ApiKeyCategory.LLM -> System.getenv("OPENROUTER_API_KEY")?.let {
            ProviderConfig(baseUrl = "https://openrouter.ai/api", apiKey = it)
        }
        ApiKeyCategory.TTS -> System.getenv("OPENAI_API_KEY")?.let {
            ProviderConfig(baseUrl = "https://api.openai.com", apiKey = it)
        }
    }
}
