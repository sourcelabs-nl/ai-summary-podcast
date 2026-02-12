package com.aisummarypodcast.store

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

@Repository
class UserApiKeyRepository(private val jdbcTemplate: JdbcTemplate) {

    private val rowMapper = RowMapper<UserApiKey> { rs, _ ->
        UserApiKey(
            userId = rs.getString("user_id"),
            provider = rs.getString("provider"),
            category = ApiKeyCategory.valueOf(rs.getString("category")),
            encryptedApiKey = rs.getString("encrypted_api_key")
        )
    }

    fun findByUserId(userId: String): List<UserApiKey> =
        jdbcTemplate.query("SELECT * FROM user_api_keys WHERE user_id = ?", rowMapper, userId)

    fun findByUserIdAndCategory(userId: String, category: ApiKeyCategory): UserApiKey? =
        jdbcTemplate.query(
            "SELECT * FROM user_api_keys WHERE user_id = ? AND category = ?",
            rowMapper, userId, category.name
        ).firstOrNull()

    fun save(key: UserApiKey) {
        jdbcTemplate.update(
            """
            INSERT INTO user_api_keys (user_id, provider, category, encrypted_api_key) VALUES (?, ?, ?, ?)
            ON CONFLICT (user_id, category) DO UPDATE SET provider = ?, encrypted_api_key = ?
            """.trimIndent(),
            key.userId, key.provider, key.category.name, key.encryptedApiKey,
            key.provider, key.encryptedApiKey
        )
    }

    fun deleteByUserId(userId: String) {
        jdbcTemplate.update("DELETE FROM user_api_keys WHERE user_id = ?", userId)
    }

    fun deleteByUserIdAndCategory(userId: String, category: ApiKeyCategory): Int =
        jdbcTemplate.update("DELETE FROM user_api_keys WHERE user_id = ? AND category = ?", userId, category.name)
}
