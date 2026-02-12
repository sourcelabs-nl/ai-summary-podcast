package com.aisummarypodcast.source

import com.aisummarypodcast.store.Article
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI
import java.time.Instant

@Component
class RssFeedFetcher {

    private val log = LoggerFactory.getLogger(javaClass)

    fun fetch(url: String, sourceId: String, lastSeenId: String?): List<Article> {
        val input = SyndFeedInput()
        @Suppress("DEPRECATION")
        val feed = input.build(XmlReader(URI(url).toURL()))
        val lastSeenInstant = lastSeenId?.let { Instant.parse(it) }

        return feed.entries
            .filter { entry ->
                val publishedDate = entry.publishedDate ?: entry.updatedDate
                if (publishedDate == null) return@filter true
                lastSeenInstant == null || publishedDate.toInstant().isAfter(lastSeenInstant)
            }
            .mapNotNull { entry ->
                val title = entry.title ?: return@mapNotNull null
                val body = entry.contents.firstOrNull()?.value
                    ?: entry.description?.value
                    ?: return@mapNotNull null
                val link = entry.link ?: entry.uri ?: return@mapNotNull null
                val publishedAt = (entry.publishedDate ?: entry.updatedDate)?.toInstant()?.toString()

                Article(
                    sourceId = sourceId,
                    title = title,
                    body = body,
                    url = link,
                    publishedAt = publishedAt,
                    contentHash = ""
                )
            }
            .also { log.info("Fetched {} new entries from RSS feed {}", it.size, sourceId) }
    }
}
