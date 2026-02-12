package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.BriefingProperties
import com.aisummarypodcast.config.EncryptionProperties
import com.aisummarypodcast.config.EpisodesProperties
import com.aisummarypodcast.config.FeedProperties
import com.aisummarypodcast.config.LlmProperties
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Podcast
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient

class ArticleProcessorTest {

    private val articleRepository = mockk<ArticleRepository> {
        every { save(any()) } answers { firstArg() }
    }
    private val appProperties = AppProperties(
        llm = LlmProperties(cheapModel = "test-model"),
        briefing = BriefingProperties(),
        episodes = EpisodesProperties(),
        feed = FeedProperties(),
        encryption = EncryptionProperties(masterKey = "test-key")
    )
    private val chatClientFactory = mockk<ChatClientFactory>()
    private val chatClient = mockk<ChatClient>()

    private val processor = ArticleProcessor(articleRepository, appProperties, chatClientFactory)

    private val podcast = Podcast(id = "p1", userId = "u1", name = "Tech Daily", topic = "AI engineering")

    private fun mockLlmResponse(result: ArticleProcessingResult) {
        val callResponseSpec = mockk<ChatClient.CallResponseSpec>()
        every { callResponseSpec.entity(ArticleProcessingResult::class.java) } returns result

        val chatClientRequestSpec = mockk<ChatClient.ChatClientRequestSpec>()
        every { chatClientRequestSpec.user(any<String>()) } returns chatClientRequestSpec
        every { chatClientRequestSpec.options(any()) } returns chatClientRequestSpec
        every { chatClientRequestSpec.call() } returns callResponseSpec

        every { chatClient.prompt() } returns chatClientRequestSpec
        every { chatClientFactory.createForPodcast(podcast) } returns chatClient
    }

    @Test
    fun `relevant article gets score and summary`() {
        val article = Article(
            id = 1, sourceId = "s1", title = "GPT-5 Released", body = "OpenAI released GPT-5 today.",
            url = "https://example.com/1", contentHash = "hash1"
        )
        mockLlmResponse(
            ArticleProcessingResult(score = 4, justification = "Directly about AI", summary = "GPT-5 was released by OpenAI.")
        )

        val result = processor.process(listOf(article), podcast)

        assertEquals(1, result.size)
        assertEquals(true, result[0].isRelevant)
        assertEquals("GPT-5 was released by OpenAI.", result[0].summary)

        val saved = slot<Article>()
        verify { articleRepository.save(capture(saved)) }
        assertEquals(true, saved.captured.isRelevant)
        assertEquals("GPT-5 was released by OpenAI.", saved.captured.summary)
    }

    @Test
    fun `irrelevant article gets score only, no summary`() {
        val article = Article(
            id = 2, sourceId = "s1", title = "Best Pizza Recipes", body = "Here are the best pizza recipes.",
            url = "https://example.com/2", contentHash = "hash2"
        )
        mockLlmResponse(
            ArticleProcessingResult(score = 1, justification = "Not about AI")
        )

        val result = processor.process(listOf(article), podcast)

        assertTrue(result.isEmpty())

        val saved = slot<Article>()
        verify { articleRepository.save(capture(saved)) }
        assertEquals(false, saved.captured.isRelevant)
        assertNull(saved.captured.summary)
    }

    @Test
    fun `summary ignored when score below threshold even if LLM returns one`() {
        val article = Article(
            id = 3, sourceId = "s1", title = "Cooking with AI", body = "AI-powered cooking app.",
            url = "https://example.com/3", contentHash = "hash3"
        )
        mockLlmResponse(
            ArticleProcessingResult(score = 2, justification = "Tangentially related", summary = "An AI cooking app was launched.")
        )

        val result = processor.process(listOf(article), podcast)

        assertTrue(result.isEmpty())

        val saved = slot<Article>()
        verify { articleRepository.save(capture(saved)) }
        assertEquals(false, saved.captured.isRelevant)
        assertNull(saved.captured.summary)
    }

    @Test
    fun `LLM error returns empty list and does not save`() {
        val article = Article(
            id = 4, sourceId = "s1", title = "Some Article", body = "body",
            url = "https://example.com/4", contentHash = "hash4"
        )

        val chatClientRequestSpec = mockk<ChatClient.ChatClientRequestSpec>()
        every { chatClientRequestSpec.user(any<String>()) } returns chatClientRequestSpec
        every { chatClientRequestSpec.options(any()) } returns chatClientRequestSpec
        every { chatClientRequestSpec.call() } throws RuntimeException("LLM unavailable")
        every { chatClient.prompt() } returns chatClientRequestSpec
        every { chatClientFactory.createForPodcast(podcast) } returns chatClient

        val result = processor.process(listOf(article), podcast)

        assertTrue(result.isEmpty())
        verify(exactly = 0) { articleRepository.save(any()) }
    }
}
