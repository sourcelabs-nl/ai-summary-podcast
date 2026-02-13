package com.aisummarypodcast.llm

import org.slf4j.LoggerFactory
import org.springframework.ai.chat.model.ChatResponse

data class TokenUsage(
    val inputTokens: Int,
    val outputTokens: Int
) {
    companion object {
        private val log = LoggerFactory.getLogger(TokenUsage::class.java)
        val ZERO = TokenUsage(0, 0)

        fun fromChatResponse(response: ChatResponse?): TokenUsage {
            if (response != null && response.metadata?.usage == null) {
                log.warn("LLM response missing usage metadata — reporting zero tokens")
            }
            val usage = response?.metadata?.usage ?: return ZERO
            return TokenUsage(
                inputTokens = usage.promptTokens ?: 0,
                outputTokens = usage.completionTokens ?: 0
            )
        }
    }

    operator fun plus(other: TokenUsage) = TokenUsage(
        inputTokens = inputTokens + other.inputTokens,
        outputTokens = outputTokens + other.outputTokens
    )
}
