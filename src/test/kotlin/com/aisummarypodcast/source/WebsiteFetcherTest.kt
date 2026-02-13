package com.aisummarypodcast.source

import io.mockk.mockk
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WebsiteFetcherTest {

    private val contentExtractor = mockk<ContentExtractor>()
    private val fetcher = WebsiteFetcher(contentExtractor)

    @Test
    fun `extractAuthor returns author from meta name tag`() {
        val document = Jsoup.parse("""
            <html>
            <head><meta name="author" content="Alice Johnson"></head>
            <body><p>Content</p></body>
            </html>
        """.trimIndent())

        assertEquals("Alice Johnson", fetcher.extractAuthor(document))
    }

    @Test
    fun `extractAuthor returns author from article author property`() {
        val document = Jsoup.parse("""
            <html>
            <head><meta property="article:author" content="Bob Williams"></head>
            <body><p>Content</p></body>
            </html>
        """.trimIndent())

        assertEquals("Bob Williams", fetcher.extractAuthor(document))
    }

    @Test
    fun `extractAuthor prefers meta name over article author`() {
        val document = Jsoup.parse("""
            <html>
            <head>
                <meta name="author" content="Alice Johnson">
                <meta property="article:author" content="Bob Williams">
            </head>
            <body><p>Content</p></body>
            </html>
        """.trimIndent())

        assertEquals("Alice Johnson", fetcher.extractAuthor(document))
    }

    @Test
    fun `extractAuthor returns null when no author meta tags present`() {
        val document = Jsoup.parse("""
            <html>
            <head><title>Page Title</title></head>
            <body><p>Content</p></body>
            </html>
        """.trimIndent())

        assertNull(fetcher.extractAuthor(document))
    }

    @Test
    fun `extractAuthor returns null when author meta tag has blank content`() {
        val document = Jsoup.parse("""
            <html>
            <head><meta name="author" content="  "></head>
            <body><p>Content</p></body>
            </html>
        """.trimIndent())

        assertNull(fetcher.extractAuthor(document))
    }
}
