package com.aisummarypodcast.store

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface EpisodePublicationRepository : CrudRepository<EpisodePublication, Long> {

    @Query("SELECT * FROM episode_publications WHERE episode_id = :episodeId")
    fun findByEpisodeId(episodeId: Long): List<EpisodePublication>

    @Query("SELECT * FROM episode_publications WHERE episode_id = :episodeId AND target = :target")
    fun findByEpisodeIdAndTarget(episodeId: Long, target: String): EpisodePublication?

    @Modifying
    @Query("DELETE FROM episode_publications WHERE episode_id = :episodeId")
    fun deleteByEpisodeId(episodeId: Long)
}
