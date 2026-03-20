package com.aisummarypodcast.store

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

interface PostRepositoryCustom {
    fun getPostCountsBySourceIds(sourceIds: List<String>): Map<String, Int>
}

@Repository
class PostRepositoryCustomImpl(
    private val jdbcClient: JdbcClient
) : PostRepositoryCustom {

    override fun getPostCountsBySourceIds(sourceIds: List<String>): Map<String, Int> {
        if (sourceIds.isEmpty()) return emptyMap()
        val placeholders = sourceIds.joinToString(",") { "?" }
        val sql = """
            SELECT source_id, COUNT(*) as total
            FROM posts
            WHERE source_id IN ($placeholders)
            GROUP BY source_id
        """.trimIndent()
        return jdbcClient.sql(sql)
            .params(*sourceIds.toTypedArray())
            .query { rs, _ ->
                rs.getString("source_id") to rs.getInt("total")
            }
            .list()
            .toMap()
    }
}
