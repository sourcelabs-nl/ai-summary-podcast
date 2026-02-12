package com.aisummarypodcast.llm

import com.aisummarypodcast.store.LlmCache
import com.aisummarypodcast.store.LlmCacheRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatOptions

class CachingChatModelTest {

    private val delegate = mockk<ChatModel>()
    private val llmCacheRepository = mockk<LlmCacheRepository>(relaxed = true) {
        every { save(any<LlmCache>()) } answers { firstArg() }
    }
    private val cachingChatModel = CachingChatModel(delegate, llmCacheRepository)

    @Test
    fun `cache miss delegates to wrapped model and stores result`() {
        val prompt = Prompt("Summarize this article", OpenAiChatOptions.builder().model("test-model").build())
        val expectedResponse = ChatResponse(listOf(Generation(AssistantMessage("A summary"))))

        every { llmCacheRepository.findByPromptHashAndModel(any(), "test-model") } returns null
        every { delegate.call(prompt) } returns expectedResponse

        val result = cachingChatModel.call(prompt)

        assertEquals("A summary", result.result!!.output.text)
        verify(exactly = 1) { delegate.call(prompt) }

        val savedSlot = slot<LlmCache>()
        verify { llmCacheRepository.save(capture(savedSlot)) }
        assertEquals("A summary", savedSlot.captured.response)
        assertEquals("test-model", savedSlot.captured.model)
    }

    @Test
    fun `cache hit returns cached response without delegating`() {
        val prompt = Prompt("Summarize this article", OpenAiChatOptions.builder().model("test-model").build())
        val cachedEntry = LlmCache(
            id = 1,
            promptHash = "somehash",
            model = "test-model",
            response = "Cached summary",
            createdAt = "2026-01-01T00:00:00Z"
        )

        every { llmCacheRepository.findByPromptHashAndModel(any(), "test-model") } returns cachedEntry

        val result = cachingChatModel.call(prompt)

        assertEquals("Cached summary", result.result!!.output.text)
        verify(exactly = 0) { delegate.call(any<Prompt>()) }
    }

    @Test
    fun `different model produces cache miss even for same prompt`() {
        val promptA = Prompt("Summarize this article", OpenAiChatOptions.builder().model("model-a").build())
        val promptB = Prompt("Summarize this article", OpenAiChatOptions.builder().model("model-b").build())
        val responseA = ChatResponse(listOf(Generation(AssistantMessage("Summary A"))))
        val responseB = ChatResponse(listOf(Generation(AssistantMessage("Summary B"))))

        every { llmCacheRepository.findByPromptHashAndModel(any(), "model-a") } returns null
        every { llmCacheRepository.findByPromptHashAndModel(any(), "model-b") } returns null
        every { delegate.call(promptA) } returns responseA
        every { delegate.call(promptB) } returns responseB

        val resultA = cachingChatModel.call(promptA)
        val resultB = cachingChatModel.call(promptB)

        assertEquals("Summary A", resultA.result!!.output.text)
        assertEquals("Summary B", resultB.result!!.output.text)
        verify(exactly = 1) { delegate.call(promptA) }
        verify(exactly = 1) { delegate.call(promptB) }

        val savedSlots = mutableListOf<LlmCache>()
        verify(exactly = 2) { llmCacheRepository.save(capture(savedSlots)) }
        assertEquals("model-a", savedSlots[0].model)
        assertEquals("model-b", savedSlots[1].model)
    }

    @Test
    fun `prompt without model options uses default key`() {
        val prompt = Prompt("Summarize this article")
        val expectedResponse = ChatResponse(listOf(Generation(AssistantMessage("A summary"))))

        every { llmCacheRepository.findByPromptHashAndModel(any(), "default") } returns null
        every { delegate.call(prompt) } returns expectedResponse

        val result = cachingChatModel.call(prompt)

        assertEquals("A summary", result.result!!.output.text)

        val savedSlot = slot<LlmCache>()
        verify { llmCacheRepository.save(capture(savedSlot)) }
        assertEquals("default", savedSlot.captured.model)
    }
}
