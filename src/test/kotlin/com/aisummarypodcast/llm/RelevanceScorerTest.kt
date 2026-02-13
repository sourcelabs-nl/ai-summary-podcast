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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient

class RelevanceScorerTest {

    private val articleRepository = mockk<ArticleRepository> {
        every { save(any()) } answers { firstArg() }
    }
    private val modelResolver = mockk<ModelResolver>()
    private val chatClientFactory = mockk<ChatClientFactory>()
    private val chatClient = mockk<ChatClient>()

    private val filterModelDef = ModelDefinition(provider = "openrouter", model = "test-model")

    private val scorer = RelevanceScorer(articleRepository, modelResolver, chatClientFactory)

    private val podcast = Podcast(id = "p1", userId = "u1", name = "Tech Daily", topic = "AI engineering")

    private fun mockLlmResponse(result: RelevanceScoringResult) {
        val callResponseSpec = mockk<ChatClient.CallResponseSpec>()
        every { callResponseSpec.entity(RelevanceScoringResult::class.java) } returns result

        val chatClientRequestSpec = mockk<ChatClient.ChatClientRequestSpec>()
        every { chatClientRequestSpec.user(any<String>()) } returns chatClientRequestSpec
        every { chatClientRequestSpec.options(any()) } returns chatClientRequestSpec
        every { chatClientRequestSpec.call() } returns callResponseSpec

        every { chatClient.prompt() } returns chatClientRequestSpec
        every { chatClientFactory.createForModel(podcast.userId, filterModelDef) } returns chatClient
    }

    @Test
    fun `article receives relevance score and is persisted`() {
        val article = Article(
            id = 1, sourceId = "s1", title = "GPT-5 Released", body = "OpenAI released GPT-5 today.",
            url = "https://example.com/1", contentHash = "hash1"
        )
        mockLlmResponse(RelevanceScoringResult(score = 8, justification = "Directly about AI"))

        val result = scorer.score(listOf(article), podcast, filterModelDef)

        assertEquals(1, result.size)
        assertEquals(8, result[0].relevanceScore)

        val saved = slot<Article>()
        verify { articleRepository.save(capture(saved)) }
        assertEquals(8, saved.captured.relevanceScore)
    }

    @Test
    fun `low scoring article is still persisted with score`() {
        val article = Article(
            id = 2, sourceId = "s1", title = "Best Pizza Recipes", body = "Here are the best pizza recipes.",
            url = "https://example.com/2", contentHash = "hash2"
        )
        mockLlmResponse(RelevanceScoringResult(score = 1, justification = "Not about AI"))

        val result = scorer.score(listOf(article), podcast, filterModelDef)

        assertEquals(1, result.size)
        assertEquals(1, result[0].relevanceScore)

        val saved = slot<Article>()
        verify { articleRepository.save(capture(saved)) }
        assertEquals(1, saved.captured.relevanceScore)
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

        val result = scorer.score(listOf(article), podcast, filterModelDef)

        assertTrue(result.isEmpty())
        verify(exactly = 0) { articleRepository.save(any()) }
    }

    @Test
    fun `score of zero is persisted`() {
        val article = Article(
            id = 4, sourceId = "s1", title = "Completely Off Topic", body = "Unrelated content.",
            url = "https://example.com/4", contentHash = "hash4"
        )
        mockLlmResponse(RelevanceScoringResult(score = 0, justification = "Completely unrelated"))

        val result = scorer.score(listOf(article), podcast, filterModelDef)

        assertEquals(1, result.size)
        assertEquals(0, result[0].relevanceScore)
    }
}
