package com.aisummarypodcast.llm

import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.store.Podcast
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.metadata.ChatResponseMetadata
import org.springframework.ai.chat.metadata.DefaultUsage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation

class EpisodeRecapGeneratorTest {

    private val chatClientFactory = mockk<ChatClientFactory>()
    private val generator = EpisodeRecapGenerator(chatClientFactory)

    private val podcast = Podcast(id = "p1", userId = "u1", name = "Tech Daily", topic = "tech")
    private val filterModelDef = ModelDefinition(provider = "openrouter", model = "anthropic/claude-haiku-4.5")

    private fun mockChatClient(responseText: String, inputTokens: Int = 800, outputTokens: Int = 60): ChatClient {
        val metadata = ChatResponseMetadata.builder()
            .usage(DefaultUsage(inputTokens, outputTokens))
            .build()
        val chatResponse = ChatResponse(listOf(Generation(AssistantMessage(responseText))), metadata)

        val callResponseSpec = mockk<ChatClient.CallResponseSpec>()
        every { callResponseSpec.chatResponse() } returns chatResponse

        val chatClientRequestSpec = mockk<ChatClient.ChatClientRequestSpec>()
        every { chatClientRequestSpec.user(any<String>()) } returns chatClientRequestSpec
        every { chatClientRequestSpec.options(any()) } returns chatClientRequestSpec
        every { chatClientRequestSpec.call() } returns callResponseSpec

        val chatClient = mockk<ChatClient>()
        every { chatClient.prompt() } returns chatClientRequestSpec
        return chatClient
    }

    @Test
    fun `generates recap from episode script`() {
        val chatClient = mockChatClient("AI chip shortages continue. New EU regulations proposed.")
        every { chatClientFactory.createForModel("u1", filterModelDef) } returns chatClient

        val result = generator.generate("Welcome to Tech Daily...", podcast, filterModelDef)

        assertEquals("AI chip shortages continue. New EU regulations proposed.", result.recap)
    }

    @Test
    fun `tracks token usage`() {
        val chatClient = mockChatClient("Recap text.", inputTokens = 800, outputTokens = 60)
        every { chatClientFactory.createForModel("u1", filterModelDef) } returns chatClient

        val result = generator.generate("Episode script text...", podcast, filterModelDef)

        assertEquals(800, result.usage.inputTokens)
        assertEquals(60, result.usage.outputTokens)
    }

    @Test
    fun `trims whitespace from recap`() {
        val chatClient = mockChatClient("  Recap with whitespace.  \n")
        every { chatClientFactory.createForModel("u1", filterModelDef) } returns chatClient

        val result = generator.generate("Script text...", podcast, filterModelDef)

        assertEquals("Recap with whitespace.", result.recap)
    }

    @Test
    fun `buildPrompt includes episode script`() {
        val scriptText = "Welcome to Tech Daily. Today we discuss AI breakthroughs."
        val prompt = generator.buildPrompt(scriptText)

        assertTrue(prompt.contains(scriptText))
        assertTrue(prompt.contains("2-3 sentences"))
    }
}
