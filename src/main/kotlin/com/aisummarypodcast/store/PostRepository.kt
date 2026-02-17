package com.aisummarypodcast.store

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface PostRepository : CrudRepository<Post, Long> {

    @Query("SELECT * FROM posts WHERE source_id = :sourceId AND content_hash = :contentHash")
    fun findBySourceIdAndContentHash(sourceId: String, contentHash: String): Post?

    @Query("SELECT * FROM posts WHERE content_hash = :contentHash AND source_id IN (:sourceIds) LIMIT 1")
    fun findByContentHashAndSourceIdIn(contentHash: String, sourceIds: List<String>): Post?

    @Query("""
        SELECT p.* FROM posts p
        LEFT JOIN post_articles pa ON p.id = pa.post_id
        WHERE pa.id IS NULL
          AND p.source_id IN (:sourceIds)
          AND p.created_at >= :cutoff
        ORDER BY p.created_at ASC
    """)
    fun findUnlinkedBySourceIds(sourceIds: List<String>, cutoff: String): List<Post>

    @Modifying
    @Query("""
        DELETE FROM posts
        WHERE created_at < :cutoff
          AND id NOT IN (SELECT post_id FROM post_articles)
    """)
    fun deleteOldUnlinkedPosts(cutoff: String)

    @Modifying
    @Query("DELETE FROM posts WHERE source_id = :sourceId")
    fun deleteBySourceId(sourceId: String)
}
