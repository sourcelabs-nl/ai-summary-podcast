package com.aisummarypodcast.source

import com.aisummarypodcast.store.Article
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class WebsiteFetcher(private val contentExtractor: ContentExtractor) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun fetch(url: String, sourceId: String): Article? {
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

        return Article(
            sourceId = sourceId,
            title = title,
            body = body,
            url = url,
            contentHash = "" // computed by SourcePoller
        )
    }
}
