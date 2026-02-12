package com.aisummarypodcast.store

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface EpisodeRepository : CrudRepository<Episode, Long> {

    @Query("SELECT * FROM episodes WHERE generated_at < :cutoff")
    fun findOlderThan(cutoff: String): List<Episode>
}
