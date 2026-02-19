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
}
