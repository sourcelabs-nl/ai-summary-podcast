package com.aisummarypodcast.store

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import com.aisummarypodcast.store.Article

interface EpisodeArticleRepository : CrudRepository<EpisodeArticle, Long> {

    @Query("SELECT * FROM episode_articles WHERE episode_id = :episodeId")
    fun findByEpisodeId(episodeId: Long): List<EpisodeArticle>

    @Query("""
        SELECT COUNT(*) > 0 FROM episode_articles ea
        JOIN episodes e ON ea.episode_id = e.id
        JOIN episode_publications ep ON ep.episode_id = e.id
        WHERE ea.article_id = :articleId
          AND e.status = 'GENERATED'
          AND ep.status = 'PUBLISHED'
    """)
    fun isArticleLinkedToPublishedEpisode(articleId: Long): Boolean

    @Query("""
        SELECT a.* FROM articles a
        JOIN episode_articles ea ON ea.article_id = a.id
        JOIN episodes e ON ea.episode_id = e.id
        WHERE e.podcast_id = :podcastId
          AND e.status = 'GENERATED'
        ORDER BY e.generated_at DESC
    """)
    fun findArticlesFromRecentGeneratedEpisodes(podcastId: String): List<Article>
}
