package com.aisummarypodcast.llm

import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.user.UserApiKeyService
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.stereotype.Component


@Component
class ChatClientFactory(
    private val userApiKeyService: UserApiKeyService
) {

    fun createForPodcast(podcast: Podcast): ChatClient {
        val apiKey = userApiKeyService.resolveKey(podcast.userId, "openrouter")
            ?: throw IllegalStateException("No API key available for provider 'openrouter'. Configure a user API key or set the OPENROUTER_API_KEY environment variable.")

        val openAiApi = OpenAiApi.builder()
            .apiKey(apiKey)
            .baseUrl("https://openrouter.ai/api")
            .build()
        val chatModel = OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .build()
        return ChatClient.builder(chatModel).build()
    }
}
