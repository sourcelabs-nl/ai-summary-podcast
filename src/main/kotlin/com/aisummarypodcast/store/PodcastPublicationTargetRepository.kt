package com.aisummarypodcast.store

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface PodcastPublicationTargetRepository : CrudRepository<PodcastPublicationTarget, Long>, PodcastPublicationTargetRepositoryCustom {

    @Query("SELECT * FROM podcast_publication_targets WHERE podcast_id = :podcastId")
    fun findByPodcastId(podcastId: String): List<PodcastPublicationTarget>

    @Query("SELECT * FROM podcast_publication_targets WHERE podcast_id = :podcastId AND target = :target")
    fun findByPodcastIdAndTarget(podcastId: String, target: String): PodcastPublicationTarget?

    @Query("DELETE FROM podcast_publication_targets WHERE podcast_id = :podcastId AND target = :target")
    fun deleteByPodcastIdAndTarget(podcastId: String, target: String): Int
}
