package com.aisummarypodcast.store

import org.springframework.data.repository.CrudRepository

interface PodcastRepository : CrudRepository<Podcast, String> {

    fun findByUserId(userId: String): List<Podcast>
}
