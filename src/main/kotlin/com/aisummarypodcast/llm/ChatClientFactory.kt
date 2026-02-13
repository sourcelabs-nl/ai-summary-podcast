package com.aisummarypodcast.llm

import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.store.ApiKeyCategory
import com.aisummarypodcast.store.LlmCacheRepository
import com.aisummarypodcast.user.UserProviderConfigService
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration

@Component
class ChatClientFactory(
    private val providerConfigService: UserProviderConfigService,
    private val llmCacheRepository: LlmCacheRepository
) {

    fun createForModel(userId: String, modelDefinition: ModelDefinition): ChatClient {
        val config = providerConfigService.resolveConfig(userId, ApiKeyCategory.LLM, modelDefinition.provider)
            ?: throw IllegalStateException(
                "No provider config available for provider '${modelDefinition.provider}'. " +
                    "Configure a user provider for '${modelDefinition.provider}' or set the appropriate environment variable."
            )

        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setReadTimeout(Duration.ofMinutes(5))
        }
        val restClientBuilder = RestClient.builder().requestFactory(requestFactory)

        val openAiApi = OpenAiApi.builder()
            .apiKey(config.apiKey ?: "")
            .baseUrl(config.baseUrl)
            .restClientBuilder(restClientBuilder)
            .build()
        val chatModel = OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .build()
        val cachingModel = CachingChatModel(chatModel, llmCacheRepository)
        return ChatClient.builder(cachingModel).build()
    }
}
