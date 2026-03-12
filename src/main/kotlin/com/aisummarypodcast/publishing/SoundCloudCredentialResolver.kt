package com.aisummarypodcast.publishing

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.ApiKeyCategory
import com.aisummarypodcast.user.UserProviderConfigService
import tools.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

data class SoundCloudCredentials(
    val clientId: String,
    val clientSecret: String,
    val callbackUri: String
)

@Component
class SoundCloudCredentialResolver(
    private val providerConfigService: UserProviderConfigService,
    private val appProperties: AppProperties,
    private val objectMapper: ObjectMapper
) {

    fun resolve(userId: String): SoundCloudCredentials {
        val dbConfig = providerConfigService.resolveConfig(userId, ApiKeyCategory.PUBLISHING, "soundcloud")
        if (dbConfig?.apiKey != null) {
            val json = objectMapper.readTree(dbConfig.apiKey)
            val clientId = json.get("clientId")?.asText()
            val clientSecret = json.get("clientSecret")?.asText()
            val callbackUri = json.get("callbackUri")?.asText()
            if (!clientId.isNullOrBlank() && !clientSecret.isNullOrBlank() && !callbackUri.isNullOrBlank()) {
                return SoundCloudCredentials(clientId, clientSecret, callbackUri)
            }
        }

        val envClientId = appProperties.soundcloud.clientId
        val envClientSecret = appProperties.soundcloud.clientSecret
        if (!envClientId.isNullOrBlank() && !envClientSecret.isNullOrBlank()) {
            val callbackUri = "${appProperties.feed.baseUrl}/oauth/soundcloud/callback"
            return SoundCloudCredentials(envClientId, envClientSecret, callbackUri)
        }

        throw IllegalStateException("SoundCloud credentials must be configured (either via Settings or environment variables)")
    }

    fun isConfigured(userId: String): Boolean =
        try {
            resolve(userId)
            true
        } catch (_: IllegalStateException) {
            false
        }
}
