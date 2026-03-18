package com.aisummarypodcast.source

import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.PostRepository
import com.aisummarypodcast.store.Source
import com.aisummarypodcast.store.SourceRepository
import com.aisummarypodcast.store.SourceType
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

data class SourceArticleCounts(
    val sourceId: String,
    val total: Int,
    val relevant: Int
)

@Service
class SourceService(
    private val sourceRepository: SourceRepository,
    private val articleRepository: ArticleRepository,
    private val postRepository: PostRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val rssFeedFetcher: RssFeedFetcher,
    private val websiteFetcher: WebsiteFetcher
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun create(podcastId: String, type: SourceType, url: String, pollIntervalMinutes: Int = 30, enabled: Boolean = true, aggregate: Boolean? = null, maxFailures: Int? = null, maxBackoffHours: Int? = null, pollDelaySeconds: Int? = null, categoryFilter: String? = null, label: String? = null): Source {
        validateUrl(type, url, categoryFilter)
        val source = Source(
            id = UUID.randomUUID().toString(),
            podcastId = podcastId,
            type = type,
            url = url,
            pollIntervalMinutes = pollIntervalMinutes,
            enabled = enabled,
            aggregate = aggregate,
            maxFailures = maxFailures,
            maxBackoffHours = maxBackoffHours,
            pollDelaySeconds = pollDelaySeconds,
            categoryFilter = categoryFilter,
            label = label,
            createdAt = Instant.now().toString()
        )
        return sourceRepository.save(source)
    }

    internal fun validateUrl(type: SourceType, url: String, categoryFilter: String? = null) {
        when (type) {
            SourceType.RSS -> validateRssUrl(url, categoryFilter)
            SourceType.WEBSITE -> validateWebsiteUrl(url)
            else -> {} // Twitter, Reddit, YouTube — skip validation
        }
    }

    private fun validateRssUrl(url: String, categoryFilter: String?) {
        try {
            val posts = rssFeedFetcher.fetch(url, "validation", null, categoryFilter)
            if (posts.isEmpty()) {
                throw IllegalArgumentException("RSS feed at $url returned no items")
            }
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("Invalid XML", ignoreCase = true) == true ||
                    e.message?.contains("Content is not allowed in prolog", ignoreCase = true) == true ->
                    "URL does not appear to be a valid RSS/Atom feed"
                e.message?.contains("UnknownHost", ignoreCase = true) == true ||
                    e.message?.contains("Connection refused", ignoreCase = true) == true ||
                    e.message?.contains("connect timed out", ignoreCase = true) == true ->
                    "Could not reach URL: ${e.message}"
                else -> "RSS feed at $url returned no content: ${e.message}"
            }
            throw IllegalArgumentException(message)
        }
    }

    private fun validateWebsiteUrl(url: String) {
        try {
            val post = websiteFetcher.fetch(url, "validation")
            if (post == null || post.body.isBlank()) {
                throw IllegalArgumentException("Website at $url returned no extractable content")
            }
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("Could not fetch website at $url: ${e.message}")
        }
    }

    fun findByPodcastId(podcastId: String): List<Source> = sourceRepository.findByPodcastId(podcastId)

    fun findById(sourceId: String): Source? = sourceRepository.findById(sourceId).orElse(null)

    fun update(sourceId: String, type: SourceType, url: String, pollIntervalMinutes: Int, enabled: Boolean, aggregate: Boolean? = null, maxFailures: Int? = null, maxBackoffHours: Int? = null, pollDelaySeconds: Int? = null, categoryFilter: String? = null, label: String? = null): Source? {
        val source = findById(sourceId) ?: return null
        val reEnabling = enabled && !source.enabled
        var updated = source.copy(type = type, url = url, pollIntervalMinutes = pollIntervalMinutes, enabled = enabled, aggregate = aggregate, maxFailures = maxFailures, maxBackoffHours = maxBackoffHours, pollDelaySeconds = pollDelaySeconds, categoryFilter = categoryFilter, label = label)
        if (reEnabling) {
            updated = updated.copy(consecutiveFailures = 0, lastFailureType = null, disabledReason = null)
        }
        return sourceRepository.save(updated)
    }

    fun getArticleCounts(sourceIds: List<String>, relevanceThreshold: Int): Map<String, SourceArticleCounts> {
        if (sourceIds.isEmpty()) return emptyMap()
        val placeholders = sourceIds.joinToString(",") { "?" }
        val sql = """
            SELECT source_id,
                   COUNT(*) as total,
                   COUNT(CASE WHEN relevance_score >= ? THEN 1 END) as relevant
            FROM articles
            WHERE source_id IN ($placeholders)
            GROUP BY source_id
        """.trimIndent()
        val args = (listOf<Any>(relevanceThreshold) + sourceIds).toTypedArray()
        return jdbcTemplate.query(sql, args) { rs: java.sql.ResultSet, _ : Int ->
            SourceArticleCounts(
                sourceId = rs.getString("source_id"),
                total = rs.getInt("total"),
                relevant = rs.getInt("relevant")
            )
        }.associateBy { it.sourceId }
    }

    fun getPostCounts(sourceIds: List<String>): Map<String, Int> {
        if (sourceIds.isEmpty()) return emptyMap()
        val placeholders = sourceIds.joinToString(",") { "?" }
        val sql = """
            SELECT source_id, COUNT(*) as total
            FROM posts
            WHERE source_id IN ($placeholders)
            GROUP BY source_id
        """.trimIndent()
        return jdbcTemplate.query(sql, sourceIds.toTypedArray()) { rs: java.sql.ResultSet, _: Int ->
            rs.getString("source_id") to rs.getInt("total")
        }.toMap()
    }

    @Transactional
    fun delete(sourceId: String): Boolean {
        val source = findById(sourceId) ?: return false
        postRepository.deleteBySourceId(sourceId)
        articleRepository.deleteBySourceId(sourceId)
        sourceRepository.delete(source)
        log.info("Deleted source {} and its posts and articles", sourceId)
        return true
    }
}
