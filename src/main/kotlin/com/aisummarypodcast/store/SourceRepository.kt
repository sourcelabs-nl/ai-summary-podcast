package com.aisummarypodcast.store

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface SourceRepository : CrudRepository<Source, String> {

    @Query("SELECT * FROM sources WHERE podcast_id = :podcastId")
    fun findByPodcastId(podcastId: String): List<Source>
}
