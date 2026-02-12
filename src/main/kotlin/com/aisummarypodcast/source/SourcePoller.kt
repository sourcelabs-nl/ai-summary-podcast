package com.aisummarypodcast.source

import com.aisummarypodcast.config.SourceEntry
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Source
import com.aisummarypodcast.store.SourceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.time.Instant

@Component
class SourcePoller(
    private val rssFeedFetcher: RssFeedFetcher,
    private val websiteFetcher: WebsiteFetcher,
    private val articleRepository: ArticleRepository,
    private val sourceRepository: SourceRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun poll(sourceEntry: SourceEntry) {
        log.info("Polling source: {} ({})", sourceEntry.id, sourceEntry.type)

        val source = sourceRepository.findById(sourceEntry.id).orElseGet {
            sourceRepository.save(Source(id = sourceEntry.id, type = sourceEntry.type, url = sourceEntry.url))
        }

        try {
            val articles = when (sourceEntry.type) {
                "rss" -> rssFeedFetcher.fetch(sourceEntry.url, sourceEntry.id, source.lastSeenId)
                "website" -> listOfNotNull(websiteFetcher.fetch(sourceEntry.url, sourceEntry.id))
                else -> {
                    log.warn("Unknown source type: {}", sourceEntry.type)
                    emptyList()
                }
            }

            var latestTimestamp = source.lastSeenId
            var savedCount = 0

            for (article in articles) {
                val hash = sha256(article.body)
                if (articleRepository.findByContentHash(hash) != null) continue

                articleRepository.save(article.copy(contentHash = hash))
                savedCount++

                article.publishedAt?.let { publishedAt ->
                    if (latestTimestamp == null || publishedAt > latestTimestamp!!) {
                        latestTimestamp = publishedAt
                    }
                }
            }

            sourceRepository.save(source.copy(lastPolled = Instant.now().toString(), lastSeenId = latestTimestamp))
            log.info("Source {} polled: {} new articles saved", sourceEntry.id, savedCount)
        } catch (e: Exception) {
            log.error("Error polling source {}: {}", sourceEntry.id, e.message, e)
            sourceRepository.save(source.copy(lastPolled = Instant.now().toString()))
        }
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
