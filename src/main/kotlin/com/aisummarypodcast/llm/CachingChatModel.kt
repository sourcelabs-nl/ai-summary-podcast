package com.aisummarypodcast.llm

import com.aisummarypodcast.store.LlmCache
import com.aisummarypodcast.store.LlmCacheRepository
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.metadata.ChatResponseMetadata
import org.springframework.ai.chat.metadata.DefaultUsage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.chat.prompt.Prompt
import reactor.core.publisher.Flux
import java.security.MessageDigest
import java.time.Instant

class CachingChatModel(
    private val delegate: ChatModel,
    private val llmCacheRepository: LlmCacheRepository
) : ChatModel {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun call(prompt: Prompt): ChatResponse {
        val model = prompt.options?.model ?: "default"
        val promptText = prompt.contents
        val promptHash = sha256("$model:$promptText")

        val cached = llmCacheRepository.findByPromptHashAndModel(promptHash, model)
        if (cached != null) {
            log.debug("LLM cache hit for model={} hash={}", model, promptHash.take(12))
            return reconstructResponse(cached.response)
        }

        val response = delegate.call(prompt)

        val responseText = response.result?.output?.text
        if (responseText != null) {
            llmCacheRepository.save(
                LlmCache(
                    promptHash = promptHash,
                    model = model,
                    response = responseText,
                    createdAt = Instant.now().toString()
                )
            )
            log.debug("LLM cache miss — stored for model={} hash={}", model, promptHash.take(12))
        }

        return response
    }

    override fun stream(prompt: Prompt): Flux<ChatResponse> = delegate.stream(prompt)

    override fun getDefaultOptions(): ChatOptions = delegate.defaultOptions

    private fun reconstructResponse(text: String): ChatResponse {
        val metadata = ChatResponseMetadata.builder()
            .usage(DefaultUsage(0, 0))
            .build()
        return ChatResponse(listOf(Generation(AssistantMessage(text))), metadata)
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
