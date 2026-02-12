package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.Article
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BriefingComposerTest {

    private val appProperties = mockk<AppProperties>()
    private val chatClientFactory = mockk<ChatClientFactory>()
    private val composer = BriefingComposer(appProperties, chatClientFactory)

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
}
