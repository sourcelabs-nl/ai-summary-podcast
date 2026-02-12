package com.aisummarypodcast.store

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

@Repository
class UserProviderConfigRepository(private val jdbcTemplate: JdbcTemplate) {

    private val rowMapper = RowMapper<UserProviderConfig> { rs, _ ->
        UserProviderConfig(
            userId = rs.getString("user_id"),
            provider = rs.getString("provider"),
            category = ApiKeyCategory.valueOf(rs.getString("category")),
            baseUrl = rs.getString("base_url"),
            encryptedApiKey = rs.getString("encrypted_api_key")
        )
    }

    fun findByUserId(userId: String): List<UserProviderConfig> =
        jdbcTemplate.query("SELECT * FROM user_provider_configs WHERE user_id = ?", rowMapper, userId)

    fun findByUserIdAndCategory(userId: String, category: ApiKeyCategory): List<UserProviderConfig> =
        jdbcTemplate.query(
            "SELECT * FROM user_provider_configs WHERE user_id = ? AND category = ?",
            rowMapper, userId, category.name
        )

    fun save(config: UserProviderConfig) {
        jdbcTemplate.update(
            """
            INSERT INTO user_provider_configs (user_id, provider, category, base_url, encrypted_api_key) VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (user_id, category, provider) DO UPDATE SET base_url = ?, encrypted_api_key = ?
            """.trimIndent(),
            config.userId, config.provider, config.category.name, config.baseUrl, config.encryptedApiKey,
            config.baseUrl, config.encryptedApiKey
        )
    }

    fun deleteByUserId(userId: String) {
        jdbcTemplate.update("DELETE FROM user_provider_configs WHERE user_id = ?", userId)
    }

    fun deleteByUserIdAndCategoryAndProvider(userId: String, category: ApiKeyCategory, provider: String): Int =
        jdbcTemplate.update("DELETE FROM user_provider_configs WHERE user_id = ? AND category = ? AND provider = ?", userId, category.name, provider)
}
