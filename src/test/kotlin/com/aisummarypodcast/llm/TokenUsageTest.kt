package com.aisummarypodcast.llm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.metadata.ChatResponseMetadata
import org.springframework.ai.chat.metadata.DefaultUsage
import org.springframework.ai.chat.model.ChatResponse

import org.springframework.ai.chat.model.Generation

class TokenUsageTest {

    @Test
    fun `extracts usage from ChatResponse with metadata`() {
        val metadata = ChatResponseMetadata.builder()
            .usage(DefaultUsage(500, 100))
            .build()
        val response = ChatResponse(listOf(Generation(AssistantMessage("text"))), metadata)

        val usage = TokenUsage.fromChatResponse(response)

        assertEquals(500, usage.inputTokens)
        assertEquals(100, usage.outputTokens)
    }

    @Test
    fun `returns zero when ChatResponse has no metadata`() {
        val response = ChatResponse(listOf(Generation(AssistantMessage("text"))))

        val usage = TokenUsage.fromChatResponse(response)

        assertEquals(0, usage.inputTokens)
        assertEquals(0, usage.outputTokens)
    }

    @Test
    fun `returns zero when response is null`() {
        val usage = TokenUsage.fromChatResponse(null)

        assertEquals(0, usage.inputTokens)
        assertEquals(0, usage.outputTokens)
    }

    @Test
    fun `plus operator adds token counts`() {
        val a = TokenUsage(100, 50)
        val b = TokenUsage(200, 75)

        val sum = a + b

        assertEquals(300, sum.inputTokens)
        assertEquals(125, sum.outputTokens)
    }
}
