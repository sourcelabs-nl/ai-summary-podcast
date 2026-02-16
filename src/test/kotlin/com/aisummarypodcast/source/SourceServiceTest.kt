package com.aisummarypodcast.source

import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.PostRepository
import com.aisummarypodcast.store.Source
import com.aisummarypodcast.store.SourceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Optional

class SourceServiceTest {

    private val sourceSlot = slot<Source>()
    private val sourceRepository = mockk<SourceRepository> {
        every { save(capture(sourceSlot)) } answers { firstArg() }
    }
    private val articleRepository = mockk<ArticleRepository>()
    private val postRepository = mockk<PostRepository>()

    private val service = SourceService(sourceRepository, articleRepository, postRepository)

    @Test
    fun `re-enabling a disabled source clears failure tracking`() {
        val disabledSource = Source(
            id = "s1", podcastId = "p1", type = "rss", url = "https://example.com/feed",
            enabled = false, consecutiveFailures = 5, lastFailureType = "permanent",
            disabledReason = "Auto-disabled after 5 consecutive HTTP 404 Not Found errors"
        )
        every { sourceRepository.findById("s1") } returns Optional.of(disabledSource)

        service.update("s1", "rss", "https://example.com/feed", 60, enabled = true)

        assertTrue(sourceSlot.captured.enabled)
        assertEquals(0, sourceSlot.captured.consecutiveFailures)
        assertNull(sourceSlot.captured.lastFailureType)
        assertNull(sourceSlot.captured.disabledReason)
    }

    @Test
    fun `updating an already-enabled source does not reset failure tracking`() {
        val source = Source(
            id = "s1", podcastId = "p1", type = "rss", url = "https://example.com/feed",
            enabled = true, consecutiveFailures = 2, lastFailureType = "transient"
        )
        every { sourceRepository.findById("s1") } returns Optional.of(source)

        service.update("s1", "rss", "https://example.com/new-feed", 60, enabled = true)

        assertEquals(2, sourceSlot.captured.consecutiveFailures)
        assertEquals("transient", sourceSlot.captured.lastFailureType)
    }
}
