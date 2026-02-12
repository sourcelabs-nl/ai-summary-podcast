package com.aisummarypodcast.store

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface PodcastRepository : CrudRepository<Podcast, String> {

    @Query("SELECT * FROM podcasts WHERE user_id = :userId")
    fun findByUserId(userId: String): List<Podcast>
}
