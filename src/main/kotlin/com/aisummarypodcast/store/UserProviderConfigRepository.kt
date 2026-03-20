package com.aisummarypodcast.store

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class UserProviderConfigRepository(private val jdbcClient: JdbcClient) {

    fun findByUserId(userId: String): List<UserProviderConfig> =
        jdbcClient.sql("SELECT * FROM user_provider_configs WHERE user_id = :userId")
            .param("userId", userId)
            .query { rs, _ -> mapRow(rs) }
            .list()

    fun findByUserIdAndCategory(userId: String, category: ApiKeyCategory): List<UserProviderConfig> =
        jdbcClient.sql("SELECT * FROM user_provider_configs WHERE user_id = :userId AND category = :category")
            .param("userId", userId)
            .param("category", category.name)
            .query { rs, _ -> mapRow(rs) }
            .list()

    fun findByUserIdAndCategoryAndProvider(userId: String, category: ApiKeyCategory, provider: String): UserProviderConfig? =
        jdbcClient.sql("SELECT * FROM user_provider_configs WHERE user_id = :userId AND category = :category AND provider = :provider")
            .param("userId", userId)
            .param("category", category.name)
            .param("provider", provider)
            .query { rs, _ -> mapRow(rs) }
            .list()
            .firstOrNull()

    fun save(config: UserProviderConfig) {
        jdbcClient.sql(
            """
            INSERT INTO user_provider_configs (user_id, provider, category, base_url, encrypted_api_key)
            VALUES (:userId, :provider, :category, :baseUrl, :encryptedApiKey)
            ON CONFLICT (user_id, category, provider) DO UPDATE SET base_url = :baseUrl, encrypted_api_key = :encryptedApiKey
            """.trimIndent()
        )
            .param("userId", config.userId)
            .param("provider", config.provider)
            .param("category", config.category.name)
            .param("baseUrl", config.baseUrl)
            .param("encryptedApiKey", config.encryptedApiKey)
            .update()
    }

    fun deleteByUserId(userId: String) {
        jdbcClient.sql("DELETE FROM user_provider_configs WHERE user_id = :userId")
            .param("userId", userId)
            .update()
    }

    fun deleteByUserIdAndCategoryAndProvider(userId: String, category: ApiKeyCategory, provider: String): Int =
        jdbcClient.sql("DELETE FROM user_provider_configs WHERE user_id = :userId AND category = :category AND provider = :provider")
            .param("userId", userId)
            .param("category", category.name)
            .param("provider", provider)
            .update()

    private fun mapRow(rs: java.sql.ResultSet): UserProviderConfig =
        UserProviderConfig(
            userId = rs.getString("user_id"),
            provider = rs.getString("provider"),
            category = ApiKeyCategory.valueOf(rs.getString("category")),
            baseUrl = rs.getString("base_url"),
            encryptedApiKey = rs.getString("encrypted_api_key")
        )
}
