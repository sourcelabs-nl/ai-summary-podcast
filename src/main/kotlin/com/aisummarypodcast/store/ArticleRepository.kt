package com.aisummarypodcast.store

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface ArticleRepository : CrudRepository<Article, Long> {

    @Query("SELECT * FROM articles WHERE is_relevant IS NULL")
    fun findUnfiltered(): List<Article>

    @Query("SELECT * FROM articles WHERE is_relevant = 1 AND is_processed = 0")
    fun findRelevantUnprocessed(): List<Article>

    fun findByContentHash(contentHash: String): Article?
}
