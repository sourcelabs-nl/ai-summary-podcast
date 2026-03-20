package com.aisummarypodcast.store

import com.aisummarypodcast.source.SourceArticleCounts
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

interface ArticleRepositoryCustom {
    fun getArticleCountsBySourceIds(sourceIds: List<String>, relevanceThreshold: Int): Map<String, SourceArticleCounts>
}

// Positional parameters required for dynamic IN clause (Spring Data @Query does not support dynamic list sizes)
@Repository
class ArticleRepositoryCustomImpl(
    private val jdbcClient: JdbcClient
) : ArticleRepositoryCustom {

    override fun getArticleCountsBySourceIds(sourceIds: List<String>, relevanceThreshold: Int): Map<String, SourceArticleCounts> {
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
        return jdbcClient.sql(sql)
            .params(*args)
            .query { rs, _ ->
                SourceArticleCounts(
                    sourceId = rs.getString("source_id"),
                    total = rs.getInt("total"),
                    relevant = rs.getInt("relevant")
                )
            }
            .list()
            .associateBy { it.sourceId }
    }
}
