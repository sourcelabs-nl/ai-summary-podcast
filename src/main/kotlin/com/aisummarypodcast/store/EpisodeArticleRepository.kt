package com.aisummarypodcast.store

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface EpisodeArticleRepository : CrudRepository<EpisodeArticle, Long> {

    @Query("SELECT * FROM episode_articles WHERE episode_id = :episodeId")
    fun findByEpisodeId(episodeId: Long): List<EpisodeArticle>
}
