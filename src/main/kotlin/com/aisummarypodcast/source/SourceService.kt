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
    private val jdbcTemplate: JdbcTemplate
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun create(podcastId: String, type: SourceType, url: String, pollIntervalMinutes: Int = 30, enabled: Boolean = true, aggregate: Boolean? = null, maxFailures: Int? = null, maxBackoffHours: Int? = null, pollDelaySeconds: Int? = null, categoryFilter: String? = null, label: String? = null): Source {
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
