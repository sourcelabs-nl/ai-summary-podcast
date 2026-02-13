package com.aisummarypodcast.source

import com.rometools.rome.io.SyndFeedInput
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.StringReader

class RssFeedFetcherTest {

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
}
