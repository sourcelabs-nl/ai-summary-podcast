package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.BriefingProperties
import com.aisummarypodcast.config.EpisodesProperties
import com.aisummarypodcast.config.EncryptionProperties
import com.aisummarypodcast.config.FeedProperties
import com.aisummarypodcast.config.LlmProperties
import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Podcast
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient

class ArticleSummarizerTest {

    private val articleRepository = mockk<ArticleRepository> {
        every { save(any()) } answers { firstArg() }
    }
    private val modelResolver = mockk<ModelResolver>()
    private val chatClientFactory = mockk<ChatClientFactory>()
    private val chatClient = mockk<ChatClient>()

    private val filterModelDef = ModelDefinition(provider = "openrouter", model = "test-model")

    private val appProperties = AppProperties(
        llm = LlmProperties(summarizationMinWords = 500),
        briefing = BriefingProperties(),
        episodes = EpisodesProperties(),
        feed = FeedProperties(),
        encryption = EncryptionProperties(masterKey = "test-key")
    )

    private val summarizer = ArticleSummarizer(articleRepository, modelResolver, chatClientFactory, appProperties)

    private val podcast = Podcast(id = "p1", userId = "u1", name = "Tech Daily", topic = "AI engineering")

    private fun mockLlmResponse(result: SummarizationResult) {
        val callResponseSpec = mockk<ChatClient.CallResponseSpec>()
        every { callResponseSpec.entity(SummarizationResult::class.java) } returns result

        val chatClientRequestSpec = mockk<ChatClient.ChatClientRequestSpec>()
        every { chatClientRequestSpec.user(any<String>()) } returns chatClientRequestSpec
        every { chatClientRequestSpec.options(any()) } returns chatClientRequestSpec
        every { chatClientRequestSpec.call() } returns callResponseSpec

        every { chatClient.prompt() } returns chatClientRequestSpec
        every { chatClientFactory.createForModel(podcast.userId, filterModelDef) } returns chatClient
    }

    private fun articleWithWordCount(words: Int): Article {
        val body = (1..words).joinToString(" ") { "word$it" }
        return Article(
            id = 1, sourceId = "s1", title = "Test Article", body = body,
            url = "https://example.com/1", contentHash = "hash1"
        )
    }

    @Test
    fun `long article is summarized and persisted`() {
        val article = articleWithWordCount(600)
        mockLlmResponse(SummarizationResult(summary = "This article covers important developments."))

        val result = summarizer.summarize(listOf(article), podcast, filterModelDef)

        assertEquals(1, result.size)
        assertEquals("This article covers important developments.", result[0].summary)

        val saved = slot<Article>()
        verify { articleRepository.save(capture(saved)) }
        assertEquals("This article covers important developments.", saved.captured.summary)
    }

    @Test
    fun `short article is skipped and not summarized`() {
        val article = articleWithWordCount(200)

        val result = summarizer.summarize(listOf(article), podcast, filterModelDef)

        assertEquals(1, result.size)
        assertNull(result[0].summary)
        verify(exactly = 0) { articleRepository.save(any()) }
    }

    @Test
    fun `article at exact threshold is summarized`() {
        val article = articleWithWordCount(500)
        mockLlmResponse(SummarizationResult(summary = "Summary at threshold."))

        val result = summarizer.summarize(listOf(article), podcast, filterModelDef)

        assertEquals("Summary at threshold.", result[0].summary)
        verify { articleRepository.save(any()) }
    }

    @Test
    fun `article just below threshold is skipped`() {
        val article = articleWithWordCount(499)

        val result = summarizer.summarize(listOf(article), podcast, filterModelDef)

        assertNull(result[0].summary)
        verify(exactly = 0) { articleRepository.save(any()) }
    }
}
