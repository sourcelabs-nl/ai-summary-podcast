package com.aisummarypodcast.store

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface LlmCacheRepository : CrudRepository<LlmCache, Long> {

    @Query("SELECT * FROM llm_cache WHERE prompt_hash = :promptHash AND model = :model")
    fun findByPromptHashAndModel(promptHash: String, model: String): LlmCache?

    @Modifying
    @Query("DELETE FROM llm_cache WHERE created_at < :cutoff")
    fun deleteOlderThan(cutoff: String)
}
