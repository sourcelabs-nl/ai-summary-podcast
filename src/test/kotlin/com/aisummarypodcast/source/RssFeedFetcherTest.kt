package com.aisummarypodcast.source

import com.rometools.rome.io.SyndFeedInput
import com.sun.net.httpserver.HttpServer
import org.jsoup.Jsoup
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.StringReader
import java.net.InetSocketAddress

class RssFeedFetcherTest {

    private val fetcher = RssFeedFetcher()
    private lateinit var server: HttpServer
    private var port: Int = 0

    @BeforeEach
    fun startServer() {
        server = HttpServer.create(InetSocketAddress(0), 0)
        port = server.address.port
        server.start()
    }

    @AfterEach
    fun stopServer() {
        server.stop(0)
    }

    private fun serveRss(xml: String) {
        server.createContext("/feed") { exchange ->
            val bytes = xml.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/rss+xml")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
    }

    @Test
    fun `HTML tags are stripped from RSS entry content`() {
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <title>Test Feed</title>
                <item>
                  <title>Test Article</title>
                  <link>https://example.com/1</link>
                  <description><![CDATA[<p>Breaking <strong>news</strong> today.</p><a href="https://example.com">Read more</a>]]></description>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val feed = SyndFeedInput().build(StringReader(rssXml))
        val entry = feed.entries.first()
        val rawBody = entry.description.value
        val cleanBody = Jsoup.parse(rawBody).text()

        assertEquals("Breaking news today. Read more", cleanBody)
    }

    @Test
    fun `plain text RSS content is preserved unchanged`() {
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <title>Test Feed</title>
                <item>
                  <title>Test Article</title>
                  <link>https://example.com/1</link>
                  <description>Simple plain text description</description>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val feed = SyndFeedInput().build(StringReader(rssXml))
        val entry = feed.entries.first()
        val rawBody = entry.description.value
        val cleanBody = Jsoup.parse(rawBody).text()

        assertEquals("Simple plain text description", cleanBody)
    }

    @Test
    fun `author extracted from RSS entry author element`() {
        serveRss("""
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <title>Test Feed</title>
                <item>
                  <title>Test Article</title>
                  <link>https://example.com/1</link>
                  <description>Some content</description>
                  <author>John Smith</author>
                </item>
              </channel>
            </rss>
        """.trimIndent())

        val articles = fetcher.fetch("http://localhost:$port/feed", "src-1", null)

        assertEquals(1, articles.size)
        assertEquals("John Smith", articles[0].author)
    }

    @Test
    fun `author extracted from dc creator when author element is absent`() {
        serveRss("""
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:dc="http://purl.org/dc/elements/1.1/">
              <channel>
                <title>Test Feed</title>
                <item>
                  <title>Test Article</title>
                  <link>https://example.com/1</link>
                  <description>Some content</description>
                  <dc:creator>Jane Doe</dc:creator>
                </item>
              </channel>
            </rss>
        """.trimIndent())

        val articles = fetcher.fetch("http://localhost:$port/feed", "src-1", null)

        assertEquals(1, articles.size)
        assertEquals("Jane Doe", articles[0].author)
    }

    @Test
    fun `author is null when no author information in RSS entry`() {
        serveRss("""
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <title>Test Feed</title>
                <item>
                  <title>Test Article</title>
                  <link>https://example.com/1</link>
                  <description>Some content</description>
                </item>
              </channel>
            </rss>
        """.trimIndent())

        val articles = fetcher.fetch("http://localhost:$port/feed", "src-1", null)

        assertEquals(1, articles.size)
        assertNull(articles[0].author)
    }

    private fun rssWithCategories(vararg categories: String): String {
        val categoryTags = categories.joinToString("\n") { "                  <category>$it</category>" }
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <title>Test Feed</title>
                <item>
                  <title>Test Article</title>
                  <link>https://example.com/1</link>
                  <description>Some content</description>
$categoryTags
                </item>
              </channel>
            </rss>
        """.trimIndent()
    }

    @Test
    fun `category filter includes entry with matching category`() {
        serveRss(rssWithCategories("Kotlin", "Programming"))

        val results = fetcher.fetch("http://localhost:$port/feed", "src-1", null, "kotlin,AI")

        assertEquals(1, results.size)
    }

    @Test
    fun `category filter excludes entry with no matching category`() {
        serveRss(rssWithCategories("Sports", "Football"))

        val results = fetcher.fetch("http://localhost:$port/feed", "src-1", null, "kotlin,AI")

        assertTrue(results.isEmpty())
    }

    @Test
    fun `category filter passes entries with no categories`() {
        serveRss("""
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <title>Test Feed</title>
                <item>
                  <title>Test Article</title>
                  <link>https://example.com/1</link>
                  <description>Some content</description>
                </item>
              </channel>
            </rss>
        """.trimIndent())

        val results = fetcher.fetch("http://localhost:$port/feed", "src-1", null, "kotlin,AI")

        assertEquals(1, results.size)
    }

    @Test
    fun `no category filter passes all entries`() {
        serveRss(rssWithCategories("Sports", "Football"))

        val results = fetcher.fetch("http://localhost:$port/feed", "src-1", null, null)

        assertEquals(1, results.size)
    }

    @Test
    fun `category filter uses case-insensitive contains matching`() {
        serveRss(rssWithCategories("Technology"))

        val results = fetcher.fetch("http://localhost:$port/feed", "src-1", null, "tech")

        assertEquals(1, results.size)
    }
}
