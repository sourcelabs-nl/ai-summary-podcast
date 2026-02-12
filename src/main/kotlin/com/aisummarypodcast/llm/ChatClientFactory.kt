package com.aisummarypodcast.llm

import com.aisummarypodcast.store.ApiKeyCategory
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.user.UserProviderConfigService
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.stereotype.Component


@Component
class ChatClientFactory(
    private val providerConfigService: UserProviderConfigService
) {

    fun createForPodcast(podcast: Podcast): ChatClient {
        val config = providerConfigService.resolveConfig(podcast.userId, ApiKeyCategory.LLM)
            ?: throw IllegalStateException("No provider config available for category 'LLM'. Configure a user provider or set the OPENROUTER_API_KEY environment variable.")

        val openAiApi = OpenAiApi.builder()
            .apiKey(config.apiKey ?: "")
            .baseUrl(config.baseUrl)
            .build()
        val chatModel = OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .build()
        return ChatClient.builder(chatModel).build()
    }
}
