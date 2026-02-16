package com.aisummarypodcast.source

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.PostRepository
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
    private val twitterFetcher: TwitterFetcher,
    private val postRepository: PostRepository,
    private val sourceRepository: SourceRepository,
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun poll(source: Source, userId: String? = null) {
        log.info("[Polling] Polling source: {} ({})", source.id, source.type)

        try {
            val rawPosts = when (source.type) {
                "rss" -> rssFeedFetcher.fetch(source.url, source.id, source.lastSeenId)
                "website" -> listOfNotNull(websiteFetcher.fetch(source.url, source.id))
                "twitter" -> {
                    if (userId == null) {
                        log.warn("[Polling] Twitter source {} requires user context for OAuth — skipping", source.id)
                        emptyList()
                    } else {
                        twitterFetcher.fetch(source.url, source.id, source.lastSeenId, userId)
                    }
                }
                else -> {
                    log.warn("[Polling] Unknown source type: {}", source.type)
                    emptyList()
                }
            }

            var latestTimestamp = source.lastSeenId
            var savedCount = 0

            val maxAgeCutoff = Instant.now().minus(appProperties.source.maxArticleAgeDays.toLong(), ChronoUnit.DAYS)
            val now = Instant.now().toString()

            for (post in rawPosts) {
                if (post.publishedAt != null && Instant.parse(post.publishedAt).isBefore(maxAgeCutoff)) {
                    log.debug("[Polling] Skipping old post '{}' (published {})", post.title, post.publishedAt)
                    continue
                }

                val hash = sha256(post.body)
                if (postRepository.findBySourceIdAndContentHash(source.id, hash) != null) continue

                postRepository.save(post.copy(contentHash = hash, createdAt = now))
                savedCount++

                post.publishedAt?.let { publishedAt ->
                    if (latestTimestamp == null || publishedAt > latestTimestamp!!) {
                        latestTimestamp = publishedAt
                    }
                }
            }

            val newLastSeenId = if (source.type == "twitter" && userId != null) {
                twitterFetcher.buildLastSeenId(source.lastSeenId, rawPosts, source.url, userId)
            } else {
                latestTimestamp
            }

            sourceRepository.save(source.copy(lastPolled = Instant.now().toString(), lastSeenId = newLastSeenId))
            log.info("[Polling] Source {} polled: {} new posts saved", source.id, savedCount)
        } catch (e: Exception) {
            log.error("[Polling] Error polling source {}: {}", source.id, e.message, e)
            sourceRepository.save(source.copy(lastPolled = Instant.now().toString()))
        }
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
