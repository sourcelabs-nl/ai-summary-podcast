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

    fun poll(source: Source, userId: String? = null, maxArticleAgeDays: Int? = null) {
        log.info("[Polling] Polling source: {} ({})", source.url, source.type)

        try {
            val rawPosts = when (source.type) {
                "rss" -> rssFeedFetcher.fetch(source.url, source.id, source.lastSeenId, source.categoryFilter)
                "website" -> listOfNotNull(websiteFetcher.fetch(source.url, source.id))
                "twitter" -> {
                    if (userId == null) {
                        log.warn("[Polling] Twitter source {} requires user context for OAuth — skipping", source.url)
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

            val effectiveMaxArticleAgeDays = maxArticleAgeDays ?: appProperties.source.maxArticleAgeDays
            val maxAgeCutoff = Instant.now().minus(effectiveMaxArticleAgeDays.toLong(), ChronoUnit.DAYS)
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

            sourceRepository.save(source.copy(
                lastPolled = Instant.now().toString(),
                lastSeenId = newLastSeenId,
                consecutiveFailures = 0,
                lastFailureType = null
            ))
            log.info("[Polling] Source {} polled: {} new posts saved", source.url, savedCount)
        } catch (e: Exception) {
            val failure = PollFailure.classify(e)
            val failureType = if (failure is PollFailure.Permanent) "permanent" else "transient"
            val newFailureCount = source.consecutiveFailures + 1

            log.error("[Polling] {} failure polling source {} (attempt {}): {}",
                failureType, source.url, newFailureCount, failure.message, e)

            var updatedSource = source.copy(
                lastPolled = Instant.now().toString(),
                consecutiveFailures = newFailureCount,
                lastFailureType = failureType
            )

            val effectiveMaxFailures = source.maxFailures ?: appProperties.source.maxFailures
            if (failure is PollFailure.Permanent && newFailureCount >= effectiveMaxFailures) {
                val reason = "Auto-disabled after $newFailureCount consecutive ${failure.message} errors"
                log.warn("[Polling] Disabling source {}: {}", source.url, reason)
                updatedSource = updatedSource.copy(enabled = false, disabledReason = reason)
            }

            sourceRepository.save(updatedSource)
        }
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
