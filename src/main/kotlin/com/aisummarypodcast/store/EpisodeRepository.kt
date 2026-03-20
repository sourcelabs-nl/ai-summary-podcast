package com.aisummarypodcast.store

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface EpisodeRepository : CrudRepository<Episode, Long> {

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastId")
    fun findByPodcastId(podcastId: String): List<Episode>

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastId AND status = :status")
    fun findByPodcastIdAndStatus(podcastId: String, status: String): List<Episode>

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastId AND status IN (:statuses)")
    fun findByPodcastIdAndStatusIn(podcastId: String, statuses: List<String>): List<Episode>

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastId ORDER BY generated_at DESC LIMIT 1")
    fun findMostRecentByPodcastId(podcastId: String): Episode?

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastId AND recap IS NOT NULL AND status = 'GENERATED' ORDER BY generated_at DESC LIMIT :limit")
    fun findRecentWithRecapByPodcastId(podcastId: String, limit: Int): List<Episode>

    @Query("""
        SELECT e.* FROM episodes e
        JOIN episode_publications ep ON ep.episode_id = e.id
        WHERE e.podcast_id = :podcastId
          AND e.status = 'GENERATED'
          AND ep.status = 'PUBLISHED'
        ORDER BY e.generated_at DESC
        LIMIT 1
    """)
    fun findLatestPublishedByPodcastId(podcastId: String): Episode?

    @Query("SELECT * FROM episodes WHERE podcast_id = :podcastId AND status = 'GENERATED' ORDER BY generated_at DESC LIMIT :limit")
    fun findRecentGeneratedByPodcastId(podcastId: String, limit: Int): List<Episode>

    @Query("SELECT * FROM episodes WHERE status = :status")
    fun findByStatus(status: String): List<Episode>
}
