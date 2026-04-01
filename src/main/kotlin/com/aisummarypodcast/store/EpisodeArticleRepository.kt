package com.aisummarypodcast.store

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface EpisodeArticleRepository : CrudRepository<EpisodeArticle, Long>, EpisodeArticleRepositoryCustom {

    fun findByEpisodeId(episodeId: Long): List<EpisodeArticle>

    @Modifying
    @Query("INSERT OR IGNORE INTO episode_articles (episode_id, article_id, topic, topic_order) VALUES (:episodeId, :articleId, :topic, :topicOrder)")
    fun insertIgnore(episodeId: Long, articleId: Long, topic: String? = null, topicOrder: Int? = null)

    // Status values must match EpisodeStatus enum names
    @Query("""
        SELECT COUNT(*) > 0 FROM episode_articles ea
        JOIN episodes e ON ea.episode_id = e.id
        JOIN episode_publications ep ON ep.episode_id = e.id
        WHERE ea.article_id = :articleId
          AND e.status = 'GENERATED'
          AND ep.status = 'PUBLISHED'
    """)
    fun isArticleLinkedToPublishedEpisode(articleId: Long): Boolean

}
