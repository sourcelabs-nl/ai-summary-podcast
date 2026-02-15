package com.aisummarypodcast.source

import com.aisummarypodcast.store.Article
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import java.time.Instant
import java.time.format.DateTimeFormatter

@Component
class TwitterFetcher(
    private val xTokenManager: XTokenManager,
    private val xClient: XClient
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun fetch(url: String, sourceId: String, lastSeenId: String?, userId: String): List<Article> {
        return try {
            val username = extractUsername(url)
            val accessToken = xTokenManager.getValidAccessToken(userId)
            val (cachedUserId, sinceId) = parseLastSeenId(lastSeenId)

            val xUserId = cachedUserId ?: resolveUserId(accessToken, username)
            val tweets = xClient.getUserTimeline(accessToken, xUserId, sinceId)

            tweets.map { tweet -> mapTweetToArticle(tweet, username, sourceId) }
        } catch (e: HttpClientErrorException) {
            handleApiError(e, sourceId)
            emptyList()
        } catch (e: Exception) {
            log.error("[Twitter] Error fetching source {}: {}", sourceId, e.message)
            emptyList()
        }
    }

    fun buildLastSeenId(currentLastSeenId: String?, articles: List<Article>, url: String, userId: String): String? {
        if (articles.isEmpty() && currentLastSeenId != null) return currentLastSeenId

        val (cachedUserId, _) = parseLastSeenId(currentLastSeenId)
        val xUserId = cachedUserId ?: return currentLastSeenId

        // Articles from X have URLs like https://x.com/user/status/TWEET_ID
        // The latest tweet ID is the highest one (tweets are returned newest first)
        val latestTweetId = articles.mapNotNull { article ->
            article.url.substringAfterLast("/").toLongOrNull()
        }.maxOrNull()

        return if (latestTweetId != null) {
            "$xUserId:$latestTweetId"
        } else {
            currentLastSeenId
        }
    }

    internal fun extractUsername(url: String): String {
        val cleaned = url.trim().removeSuffix("/")
        // Handle full URLs like https://x.com/username or https://twitter.com/username
        val urlPattern = Regex("https?://(?:x\\.com|twitter\\.com)/([a-zA-Z0-9_]+)")
        val match = urlPattern.find(cleaned)
        if (match != null) {
            return match.groupValues[1]
        }
        // Handle @username or plain username
        return cleaned.removePrefix("@")
    }

    internal fun parseLastSeenId(lastSeenId: String?): Pair<String?, String?> {
        if (lastSeenId == null) return Pair(null, null)
        val parts = lastSeenId.split(":", limit = 2)
        return if (parts.size == 2) {
            Pair(parts[0], parts[1])
        } else {
            Pair(null, null)
        }
    }

    private fun resolveUserId(accessToken: String, username: String): String {
        return try {
            xClient.resolveUsername(accessToken, username).id
        } catch (e: HttpClientErrorException) {
            if (xClient.isNotFound(e)) {
                throw RuntimeException("X user not found: $username")
            }
            throw e
        }
    }

    internal fun mapTweetToArticle(tweet: XTweet, username: String, sourceId: String): Article {
        val title = if (tweet.text.length > 100) {
            tweet.text.take(100) + "..."
        } else {
            tweet.text
        }

        val publishedAt = tweet.createdAt?.let {
            try {
                Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(it)).toString()
            } catch (_: Exception) {
                it
            }
        }

        return Article(
            sourceId = sourceId,
            title = title,
            body = tweet.text,
            url = "https://x.com/$username/status/${tweet.id}",
            publishedAt = publishedAt,
            author = "@$username",
            contentHash = ""
        )
    }

    private fun handleApiError(e: HttpClientErrorException, sourceId: String) {
        when {
            xClient.isRateLimited(e) -> log.warn("[Twitter] Rate limited for source {}: {}", sourceId, e.message)
            xClient.isAuthError(e) -> log.error("[Twitter] Auth error for source {} — OAuth token may be invalid or revoked: {}", sourceId, e.message)
            xClient.isNotFound(e) -> log.error("[Twitter] User not found for source {}: {}", sourceId, e.message)
            else -> log.error("[Twitter] API error for source {}: {}", sourceId, e.message, e)
        }
    }
}
