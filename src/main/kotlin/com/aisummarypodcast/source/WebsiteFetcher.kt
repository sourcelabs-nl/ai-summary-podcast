package com.aisummarypodcast.source

import com.aisummarypodcast.store.Post
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class WebsiteFetcher(private val contentExtractor: ContentExtractor) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun fetch(url: String, sourceId: String): Post? {
        val document = Jsoup.connect(url)
            .userAgent("AiSummaryPodcast/1.0")
            .timeout(30_000)
            .get()

        val title = document.title()
        val body = contentExtractor.extract(document)

        if (body.isBlank()) {
            log.warn("No content extracted from website {}", url)
            return null
        }

        val author = extractAuthor(document)

        return Post(
            sourceId = sourceId,
            title = title,
            body = body,
            url = url,
            author = author,
            contentHash = "",
            createdAt = ""
        )
    }

    internal fun extractAuthor(document: Document): String? =
        document.selectFirst("meta[name=author]")?.attr("content")?.takeIf { it.isNotBlank() }
            ?: document.selectFirst("meta[property=article:author]")?.attr("content")?.takeIf { it.isNotBlank() }
}
