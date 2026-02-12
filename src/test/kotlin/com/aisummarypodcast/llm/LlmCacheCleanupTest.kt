package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.LlmCacheProperties
import com.aisummarypodcast.store.LlmCacheRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class LlmCacheCleanupTest {

    private val llmCacheRepository = mockk<LlmCacheRepository>(relaxed = true)
    private val appProperties = mockk<AppProperties>()

    @Test
    fun `cleanup deletes old entries when max-age-days is configured`() {
        every { appProperties.llmCache } returns LlmCacheProperties(maxAgeDays = 30)

        val cleanup = LlmCacheCleanup(llmCacheRepository, appProperties)
        cleanup.cleanup()

        verify(exactly = 1) { llmCacheRepository.deleteOlderThan(any()) }
    }

    @Test
    fun `cleanup skips deletion when max-age-days is not configured`() {
        every { appProperties.llmCache } returns LlmCacheProperties(maxAgeDays = null)

        val cleanup = LlmCacheCleanup(llmCacheRepository, appProperties)
        cleanup.cleanup()

        verify(exactly = 0) { llmCacheRepository.deleteOlderThan(any()) }
    }
}
