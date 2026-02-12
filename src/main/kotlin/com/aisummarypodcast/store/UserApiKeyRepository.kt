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
            encryptedApiKey = rs.getString("encrypted_api_key")
        )
    }

    fun findByUserId(userId: String): List<UserApiKey> =
        jdbcTemplate.query("SELECT * FROM user_api_keys WHERE user_id = ?", rowMapper, userId)

    fun findByUserIdAndProvider(userId: String, provider: String): UserApiKey? =
        jdbcTemplate.query("SELECT * FROM user_api_keys WHERE user_id = ? AND provider = ?", rowMapper, userId, provider)
            .firstOrNull()

    fun save(key: UserApiKey) {
        jdbcTemplate.update(
            """
            INSERT INTO user_api_keys (user_id, provider, encrypted_api_key) VALUES (?, ?, ?)
            ON CONFLICT (user_id, provider) DO UPDATE SET encrypted_api_key = ?
            """.trimIndent(),
            key.userId, key.provider, key.encryptedApiKey, key.encryptedApiKey
        )
    }

    fun deleteByUserId(userId: String) {
        jdbcTemplate.update("DELETE FROM user_api_keys WHERE user_id = ?", userId)
    }

    fun deleteByUserIdAndProvider(userId: String, provider: String): Int =
        jdbcTemplate.update("DELETE FROM user_api_keys WHERE user_id = ? AND provider = ?", userId, provider)
}
