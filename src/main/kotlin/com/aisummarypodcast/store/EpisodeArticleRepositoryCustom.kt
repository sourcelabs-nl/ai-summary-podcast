package com.aisummarypodcast.store

import com.aisummarypodcast.podcast.ArticleSourceResponse
import com.aisummarypodcast.podcast.EpisodeArticleResponse
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

data class FeedArticle(val title: String, val url: String, val topic: String? = null)

interface EpisodeArticleRepositoryCustom {
    fun findArticlesWithSourcesByEpisodeId(episodeId: Long): List<EpisodeArticleResponse>
    fun findArticlesByEpisodeIds(episodeIds: List<Long>): Map<Long, List<FeedArticle>>
}

@Repository
class EpisodeArticleRepositoryCustomImpl(
    private val jdbcClient: JdbcClient
) : EpisodeArticleRepositoryCustom {

    override fun findArticlesWithSourcesByEpisodeId(episodeId: Long): List<EpisodeArticleResponse> {
        return jdbcClient.sql(
            """
            SELECT a.id, a.title, a.url, a.author, a.published_at, a.relevance_score, a.summary, a.body,
                   s.id AS source_id, s.type AS source_type, s.url AS source_url, s.label AS source_label
            FROM episode_articles ea
            JOIN articles a ON ea.article_id = a.id
            JOIN sources s ON a.source_id = s.id
            WHERE ea.episode_id = :episodeId
            ORDER BY a.relevance_score DESC NULLS LAST
            """.trimIndent()
        )
            .param("episodeId", episodeId)
            .query { rs, _ ->
                EpisodeArticleResponse(
                    id = rs.getLong("id"),
                    title = rs.getString("title"),
                    url = rs.getString("url"),
                    author = rs.getString("author"),
                    publishedAt = rs.getString("published_at"),
                    relevanceScore = rs.getObject("relevance_score") as? Int,
                    summary = rs.getString("summary"),
                    body = rs.getString("body"),
                    source = ArticleSourceResponse(
                        id = rs.getString("source_id"),
                        type = rs.getString("source_type"),
                        url = rs.getString("source_url"),
                        label = rs.getString("source_label")
                    )
                )
            }
            .list()
    }

    override fun findArticlesByEpisodeIds(episodeIds: List<Long>): Map<Long, List<FeedArticle>> {
        if (episodeIds.isEmpty()) return emptyMap()
        val placeholders = episodeIds.indices.joinToString(",") { ":id$it" }
        val params = mutableMapOf<String, Any>()
        episodeIds.forEachIndexed { i, id -> params["id$i"] = id }

        data class Row(val episodeId: Long, val title: String, val url: String, val topic: String?)

        val rows = jdbcClient.sql(
            """
            SELECT ea.episode_id, a.title, a.url, ea.topic
            FROM episode_articles ea
            JOIN articles a ON ea.article_id = a.id
            WHERE ea.episode_id IN ($placeholders)
            ORDER BY a.relevance_score DESC NULLS LAST
            """.trimIndent()
        )
            .params(params)
            .query { rs, _ ->
                Row(rs.getLong("episode_id"), rs.getString("title"), rs.getString("url"), rs.getString("topic"))
            }
            .list()

        return rows.groupBy({ it.episodeId }, { FeedArticle(it.title, it.url, it.topic) })
    }
}
