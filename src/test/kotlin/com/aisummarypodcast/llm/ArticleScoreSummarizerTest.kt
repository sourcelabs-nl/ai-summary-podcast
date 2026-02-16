package com.aisummarypodcast.llm

import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Podcast
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.ResponseEntity
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.metadata.ChatResponseMetadata
import org.springframework.ai.chat.metadata.DefaultUsage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation

class ArticleScoreSummarizerTest {

    private val articleRepository = mockk<ArticleRepository> {
        every { save(any()) } answers { firstArg() }
    }
    private val chatClientFactory = mockk<ChatClientFactory>()
    private val chatClient = mockk<ChatClient>()

    private val filterModelDef = ModelDefinition(
        provider = "openrouter", model = "test-model",
        inputCostPerMtok = 0.15, outputCostPerMtok = 0.60
    )

    private val scoreSummarizer = ArticleScoreSummarizer(articleRepository, chatClientFactory)

    private val podcast = Podcast(id = "p1", userId = "u1", name = "Tech Daily", topic = "AI engineering")

    private fun mockLlmResponse(result: ScoreSummarizeResult, inputTokens: Int = 500, outputTokens: Int = 80) {
        val metadata = ChatResponseMetadata.builder()
            .usage(DefaultUsage(inputTokens, outputTokens))
            .build()
        val chatResponse = ChatResponse(listOf(Generation(AssistantMessage("{}"))), metadata)
        val responseEntity = ResponseEntity(chatResponse, result)

        val callResponseSpec = mockk<ChatClient.CallResponseSpec>()
        every { callResponseSpec.responseEntity(ScoreSummarizeResult::class.java) } returns responseEntity

        val chatClientRequestSpec = mockk<ChatClient.ChatClientRequestSpec>()
        every { chatClientRequestSpec.user(any<String>()) } returns chatClientRequestSpec
        every { chatClientRequestSpec.options(any()) } returns chatClientRequestSpec
        every { chatClientRequestSpec.call() } returns callResponseSpec

        every { chatClient.prompt() } returns chatClientRequestSpec
        every { chatClientFactory.createForModel(podcast.userId, filterModelDef) } returns chatClient
    }

    @Test
    fun `article receives relevance score and summary`() {
        val article = Article(
            id = 1, sourceId = "s1", title = "GPT-5 Released", body = "OpenAI released GPT-5 today.",
            url = "https://example.com/1", contentHash = "hash1"
        )
        mockLlmResponse(ScoreSummarizeResult(relevanceScore = 8, summary = "OpenAI launched GPT-5, a major AI milestone."))

        val result = scoreSummarizer.scoreSummarize(listOf(article), podcast, filterModelDef)

        assertEquals(1, result.size)
        assertEquals(8, result[0].relevanceScore)
        assertEquals("OpenAI launched GPT-5, a major AI milestone.", result[0].summary)

        val saved = slot<Article>()
        verify { articleRepository.save(capture(saved)) }
        assertEquals(8, saved.captured.relevanceScore)
        assertNotNull(saved.captured.summary)
    }

    @Test
    fun `irrelevant article receives low score and null summary`() {
        val article = Article(
            id = 2, sourceId = "s1", title = "Best Pizza Recipes", body = "Here are the best pizza recipes.",
            url = "https://example.com/2", contentHash = "hash2"
        )
        mockLlmResponse(ScoreSummarizeResult(relevanceScore = 1, summary = ""))

        val result = scoreSummarizer.scoreSummarize(listOf(article), podcast, filterModelDef)

        assertEquals(1, result.size)
        assertEquals(1, result[0].relevanceScore)
        assertEquals(null, result[0].summary) // blank summary is stored as null
    }

    @Test
    fun `LLM error returns empty list and does not save`() {
        val article = Article(
            id = 3, sourceId = "s1", title = "Some Article", body = "body",
            url = "https://example.com/3", contentHash = "hash3"
        )

        val chatClientRequestSpec = mockk<ChatClient.ChatClientRequestSpec>()
        every { chatClientRequestSpec.user(any<String>()) } returns chatClientRequestSpec
        every { chatClientRequestSpec.options(any()) } returns chatClientRequestSpec
        every { chatClientRequestSpec.call() } throws RuntimeException("LLM unavailable")
        every { chatClient.prompt() } returns chatClientRequestSpec
        every { chatClientFactory.createForModel(podcast.userId, filterModelDef) } returns chatClient

        val result = scoreSummarizer.scoreSummarize(listOf(article), podcast, filterModelDef)

        assertTrue(result.isEmpty())
        verify(exactly = 0) { articleRepository.save(any()) }
    }

    @Test
    fun `token counts and cost are persisted on article`() {
        val article = Article(
            id = 5, sourceId = "s1", title = "AI News", body = "AI content",
            url = "https://example.com/5", contentHash = "hash5"
        )
        mockLlmResponse(ScoreSummarizeResult(relevanceScore = 7, summary = "Summary of AI news."), inputTokens = 800, outputTokens = 120)

        scoreSummarizer.scoreSummarize(listOf(article), podcast, filterModelDef)

        val saved = slot<Article>()
        verify { articleRepository.save(capture(saved)) }
        assertEquals(800, saved.captured.llmInputTokens)
        assertEquals(120, saved.captured.llmOutputTokens)
    }

    @Test
    fun `attribution preserved in summary prompt`() {
        val article = Article(
            id = 6, sourceId = "s1", title = "MIT Study", body = "Researchers at MIT published a study showing AI advances.",
            url = "https://example.com/6", contentHash = "hash6"
        )
        mockLlmResponse(ScoreSummarizeResult(relevanceScore = 9, summary = "MIT researchers found significant AI advances."))

        val result = scoreSummarizer.scoreSummarize(listOf(article), podcast, filterModelDef)

        assertEquals("MIT researchers found significant AI advances.", result[0].summary)
    }
}
