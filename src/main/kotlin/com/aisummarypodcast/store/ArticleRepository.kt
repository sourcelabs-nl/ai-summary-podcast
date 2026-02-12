package com.aisummarypodcast.store

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface ArticleRepository : CrudRepository<Article, Long> {

    @Query("SELECT * FROM articles WHERE is_relevant IS NULL AND source_id IN (:sourceIds)")
    fun findUnfilteredBySourceIds(sourceIds: List<String>): List<Article>

    @Query("SELECT * FROM articles WHERE is_relevant = 1 AND is_processed = 0 AND source_id IN (:sourceIds)")
    fun findRelevantUnprocessedBySourceIds(sourceIds: List<String>): List<Article>

    @Query("SELECT * FROM articles WHERE source_id = :sourceId AND content_hash = :contentHash")
    fun findBySourceIdAndContentHash(sourceId: String, contentHash: String): Article?

    @Modifying
    @Query("DELETE FROM articles WHERE source_id = :sourceId")
    fun deleteBySourceId(sourceId: String)

    @Modifying
    @Query("DELETE FROM articles WHERE published_at IS NOT NULL AND published_at < :cutoff AND is_processed = 0")
    fun deleteOldUnprocessedArticles(cutoff: String)
}
