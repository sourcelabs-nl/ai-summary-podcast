package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.BriefingProperties
import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.Podcast
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.metadata.ChatResponseMetadata
import org.springframework.ai.chat.metadata.DefaultUsage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation

class BriefingComposerTest {

    private val appProperties = mockk<AppProperties>().also {
        every { it.briefing } returns BriefingProperties(targetWords = 1500)
    }
    private val modelResolver = mockk<ModelResolver>()
    private val chatClientFactory = mockk<ChatClientFactory>()
    private val composer = BriefingComposer(appProperties, modelResolver, chatClientFactory)

    @Test
    fun `stripSectionHeaders removes Opening header line`() {
        val input = "[Opening]\nWelcome to today's briefing.\n"
        val result = composer.stripSectionHeaders(input)
        assertEquals("Welcome to today's briefing.\n", result)
    }

    @Test
    fun `stripSectionHeaders removes multiple header lines`() {
        val input = "[Opening]\nWelcome.\n[Transition]\nNext topic.\n[Closing]\nThat's all.\n"
        val result = composer.stripSectionHeaders(input)
        assertEquals("Welcome.\nNext topic.\nThat's all.\n", result)
    }

    @Test
    fun `stripSectionHeaders preserves inline bracketed text`() {
        val input = "The company [ACME Corp] announced earnings.\n"
        val result = composer.stripSectionHeaders(input)
        assertEquals("The company [ACME Corp] announced earnings.\n", result)
    }

    @Test
    fun `stripSectionHeaders preserves brackets within sentences`() {
        val input = "Results were mixed [see chart] for the quarter.\nOverall performance improved.\n"
        val result = composer.stripSectionHeaders(input)
        assertEquals("Results were mixed [see chart] for the quarter.\nOverall performance improved.\n", result)
    }

    @Test
    fun `stripSectionHeaders handles script with no headers`() {
        val input = "Welcome to today's briefing.\nHere are the top stories.\n"
        val result = composer.stripSectionHeaders(input)
        assertEquals(input, result)
    }

    @Test
    fun `extractDomain extracts domain from standard URL`() {
        assertEquals("techcrunch.com", composer.extractDomain("https://techcrunch.com/2026/02/12/example"))
    }

    @Test
    fun `extractDomain strips www prefix`() {
        assertEquals("theverge.com", composer.extractDomain("https://www.theverge.com/article"))
    }

    @Test
    fun `extractDomain handles URL without path`() {
        assertEquals("example.com", composer.extractDomain("https://example.com"))
    }

    @Test
    fun `extractDomain returns original string for invalid URL`() {
        assertEquals("not-a-url", composer.extractDomain("not-a-url"))
    }

    @Test
    fun `extractDomain handles http URL`() {
        assertEquals("blog.example.org", composer.extractDomain("http://blog.example.org/posts/123"))
    }

    @Test
    fun `summary block includes source domain`() {
        val articles = listOf(
            Article(
                id = 1,
                sourceId = "src-1",
                title = "AI Breakthrough",
                body = "body",
                url = "https://techcrunch.com/2026/02/12/ai",
                contentHash = "hash1",
                summary = "A major AI breakthrough was announced."
            ),
            Article(
                id = 2,
                sourceId = "src-2",
                title = "New Chip",
                body = "body",
                url = "https://www.theverge.com/chip",
                contentHash = "hash2",
                summary = "A new chip was unveiled."
            )
        )

        val block = articles.mapIndexed { index, article ->
            val source = composer.extractDomain(article.url)
            "${index + 1}. [$source] ${article.title}\n${article.summary}"
        }.joinToString("\n\n")

        assertEquals(
            "1. [techcrunch.com] AI Breakthrough\nA major AI breakthrough was announced.\n\n" +
                "2. [theverge.com] New Chip\nA new chip was unveiled.",
            block
        )
    }

    private val sampleArticles = listOf(
        Article(
            id = 1, sourceId = "src-1", title = "AI News",
            body = "body", url = "https://techcrunch.com/ai",
            contentHash = "hash1", summary = "AI summary."
        )
    )

    @Test
    fun `buildPrompt includes language instruction for non-English podcast`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Tech NL", topic = "tech", language = "nl")
        val prompt = composer.buildPrompt(sampleArticles, podcast)
        assertTrue(prompt.contains("Write the entire script in Dutch"), "Expected Dutch language instruction in prompt")
    }

    @Test
    fun `buildPrompt does not include language instruction for English podcast`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Tech EN", topic = "tech", language = "en")
        val prompt = composer.buildPrompt(sampleArticles, podcast)
        assertFalse(prompt.contains("Write the entire script in"), "Expected no language instruction for English")
    }

    @Test
    fun `buildPrompt formats date in podcast language locale`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Tech FR", topic = "tech", language = "fr")
        val prompt = composer.buildPrompt(sampleArticles, podcast)
        val dateLine = prompt.lines().find { it.trim().startsWith("Date:") }
        assertNotNull(dateLine, "Expected a Date line in the prompt")
        val englishDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val containsEnglishDay = englishDays.any { dateLine!!.contains(it) }
        assertFalse(containsEnglishDay, "Expected French date formatting, but found English day name in: $dateLine")
    }

    @Test
    fun `buildPrompt includes language instruction for French podcast`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Tech FR", topic = "tech", language = "fr")
        val prompt = composer.buildPrompt(sampleArticles, podcast)
        assertTrue(prompt.contains("Write the entire script in French"))
    }

    @Test
    fun `buildPrompt uses summary when available`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech")
        val articles = listOf(
            Article(
                id = 1, sourceId = "s1", title = "Long Article",
                body = "This is the full article body that is quite long.",
                url = "https://example.com/1", contentHash = "h1",
                summary = "This is the summary."
            )
        )
        val prompt = composer.buildPrompt(articles, podcast)
        assertTrue(prompt.contains("This is the summary."))
        assertFalse(prompt.contains("This is the full article body"))
    }

    @Test
    fun `buildPrompt uses body when summary is null`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech")
        val articles = listOf(
            Article(
                id = 1, sourceId = "s1", title = "Short Article",
                body = "Short article body content.",
                url = "https://example.com/1", contentHash = "h1",
                summary = null
            )
        )
        val prompt = composer.buildPrompt(articles, podcast)
        assertTrue(prompt.contains("Short article body content."))
    }

    @Test
    fun `compose returns CompositionResult with token usage`() {
        val composeModelDef = ModelDefinition(provider = "openrouter", model = "test-model")
        val podcast = Podcast(id = "p1", userId = "u1", name = "Test Pod", topic = "tech")
        val articles = listOf(
            Article(
                id = 1, sourceId = "s1", title = "AI News", body = "body",
                url = "https://example.com/1", contentHash = "h1", summary = "AI summary."
            )
        )

        val metadata = ChatResponseMetadata.builder()
            .usage(DefaultUsage(1200, 400))
            .build()
        val chatResponse = ChatResponse(listOf(Generation(AssistantMessage("Welcome to the briefing."))), metadata)

        val callResponseSpec = mockk<ChatClient.CallResponseSpec>()
        every { callResponseSpec.chatResponse() } returns chatResponse

        val chatClientRequestSpec = mockk<ChatClient.ChatClientRequestSpec>()
        every { chatClientRequestSpec.user(any<String>()) } returns chatClientRequestSpec
        every { chatClientRequestSpec.options(any()) } returns chatClientRequestSpec
        every { chatClientRequestSpec.call() } returns callResponseSpec

        val chatClient = mockk<ChatClient>()
        every { chatClient.prompt() } returns chatClientRequestSpec
        every { chatClientFactory.createForModel(podcast.userId, composeModelDef) } returns chatClient

        val result = composer.compose(articles, podcast, composeModelDef)

        assertEquals("Welcome to the briefing.", result.script)
        assertEquals(1200, result.usage.inputTokens)
        assertEquals(400, result.usage.outputTokens)
    }

    @Test
    fun `buildPrompt handles mixed summary and body articles`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech")
        val articles = listOf(
            Article(
                id = 1, sourceId = "s1", title = "Long Article",
                body = "Long body text.",
                url = "https://example.com/1", contentHash = "h1",
                summary = "Summary of long article."
            ),
            Article(
                id = 2, sourceId = "s1", title = "Short Article",
                body = "Short body text.",
                url = "https://example.com/2", contentHash = "h2",
                summary = null
            )
        )
        val prompt = composer.buildPrompt(articles, podcast)
        assertTrue(prompt.contains("Summary of long article."))
        assertTrue(prompt.contains("Short body text."))
        assertFalse(prompt.contains("Long body text."))
    }
}
