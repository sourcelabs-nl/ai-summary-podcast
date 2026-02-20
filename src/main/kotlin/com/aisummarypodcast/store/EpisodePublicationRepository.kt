package com.aisummarypodcast.store

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface EpisodePublicationRepository : CrudRepository<EpisodePublication, Long> {

    @Query("SELECT * FROM episode_publications WHERE episode_id = :episodeId")
    fun findByEpisodeId(episodeId: Long): List<EpisodePublication>

    @Query("SELECT * FROM episode_publications WHERE episode_id = :episodeId AND target = :target")
    fun findByEpisodeIdAndTarget(episodeId: Long, target: String): EpisodePublication?

    @Query("""
        SELECT ep.* FROM episode_publications ep
        JOIN episodes e ON ep.episode_id = e.id
        WHERE e.podcast_id = :podcastId AND ep.target = :target AND ep.status = 'PUBLISHED'
    """)
    fun findPublishedByPodcastIdAndTarget(podcastId: String, target: String): List<EpisodePublication>
}
