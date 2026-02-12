package com.aisummarypodcast.source

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Source
import com.aisummarypodcast.store.SourceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class SourcePoller(
    private val rssFeedFetcher: RssFeedFetcher,
    private val websiteFetcher: WebsiteFetcher,
    private val articleRepository: ArticleRepository,
    private val sourceRepository: SourceRepository,
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun poll(source: Source) {
        log.info("Polling source: {} ({})", source.id, source.type)

        try {
            val articles = when (source.type) {
                "rss" -> rssFeedFetcher.fetch(source.url, source.id, source.lastSeenId)
                "website" -> listOfNotNull(websiteFetcher.fetch(source.url, source.id))
                else -> {
                    log.warn("Unknown source type: {}", source.type)
                    emptyList()
                }
            }

            var latestTimestamp = source.lastSeenId
            var savedCount = 0

            val maxAgeCutoff = Instant.now().minus(appProperties.source.maxArticleAgeDays.toLong(), ChronoUnit.DAYS)

            for (article in articles) {
                if (article.publishedAt != null && Instant.parse(article.publishedAt).isBefore(maxAgeCutoff)) {
                    log.debug("Skipping old article '{}' (published {})", article.title, article.publishedAt)
                    continue
                }

                val hash = sha256(article.body)
                if (articleRepository.findBySourceIdAndContentHash(source.id, hash) != null) continue

                articleRepository.save(article.copy(contentHash = hash))
                savedCount++

                article.publishedAt?.let { publishedAt ->
                    if (latestTimestamp == null || publishedAt > latestTimestamp!!) {
                        latestTimestamp = publishedAt
                    }
                }
            }

            sourceRepository.save(source.copy(lastPolled = Instant.now().toString(), lastSeenId = latestTimestamp))
            log.info("Source {} polled: {} new articles saved", source.id, savedCount)
        } catch (e: Exception) {
            log.error("Error polling source {}: {}", source.id, e.message, e)
            sourceRepository.save(source.copy(lastPolled = Instant.now().toString()))
        }
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
