package com.aisummarypodcast.source

import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.PostRepository
import com.aisummarypodcast.store.Source
import com.aisummarypodcast.store.SourceRepository
import com.aisummarypodcast.store.SourceType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import java.util.Optional

class SourceServiceTest {

    private val sourceSlot = slot<Source>()
    private val sourceRepository = mockk<SourceRepository> {
        every { save(capture(sourceSlot)) } answers { firstArg() }
    }
    private val articleRepository = mockk<ArticleRepository>()
    private val postRepository = mockk<PostRepository>()
    private val jdbcTemplate = mockk<JdbcTemplate>()

    private val service = SourceService(sourceRepository, articleRepository, postRepository, jdbcTemplate)

    @Test
    fun `re-enabling a disabled source clears failure tracking`() {
        val disabledSource = Source(
            id = "s1", podcastId = "p1", type = SourceType.RSS, url = "https://example.com/feed",
            enabled = false, consecutiveFailures = 5, lastFailureType = "permanent",
            disabledReason = "Auto-disabled after 5 consecutive HTTP 404 Not Found errors"
        )
        every { sourceRepository.findById("s1") } returns Optional.of(disabledSource)

        service.update("s1", SourceType.RSS, "https://example.com/feed", 60, enabled = true)

        assertTrue(sourceSlot.captured.enabled)
        assertEquals(0, sourceSlot.captured.consecutiveFailures)
        assertNull(sourceSlot.captured.lastFailureType)
        assertNull(sourceSlot.captured.disabledReason)
    }

    @Test
    fun `updating an already-enabled source does not reset failure tracking`() {
        val source = Source(
            id = "s1", podcastId = "p1", type = SourceType.RSS, url = "https://example.com/feed",
            enabled = true, consecutiveFailures = 2, lastFailureType = "transient"
        )
        every { sourceRepository.findById("s1") } returns Optional.of(source)

        service.update("s1", SourceType.RSS, "https://example.com/new-feed", 60, enabled = true)

        assertEquals(2, sourceSlot.captured.consecutiveFailures)
        assertEquals("transient", sourceSlot.captured.lastFailureType)
    }

    @Test
    fun `getArticleCounts returns counts grouped by source`() {
        every { jdbcTemplate.query(any<String>(), any<Array<Any>>(), any<RowMapper<SourceArticleCounts>>()) } answers {
            val mapper = thirdArg<RowMapper<SourceArticleCounts>>()
            val rs1 = mockk<java.sql.ResultSet> {
                every { getString("source_id") } returns "s1"
                every { getInt("total") } returns 42
                every { getInt("relevant") } returns 18
            }
            val rs2 = mockk<java.sql.ResultSet> {
                every { getString("source_id") } returns "s2"
                every { getInt("total") } returns 10
                every { getInt("relevant") } returns 0
            }
            listOf(mapper.mapRow(rs1, 0)!!, mapper.mapRow(rs2, 1)!!)
        }

        val result = service.getArticleCounts(listOf("s1", "s2"), 5)

        assertEquals(2, result.size)
        assertEquals(42, result["s1"]?.total)
        assertEquals(18, result["s1"]?.relevant)
        assertEquals(10, result["s2"]?.total)
        assertEquals(0, result["s2"]?.relevant)
    }

    @Test
    fun `getArticleCounts returns empty map for empty source list`() {
        val result = service.getArticleCounts(emptyList(), 5)
        assertEquals(emptyMap<String, SourceArticleCounts>(), result)
    }

    @Test
    fun `getArticleCounts returns empty map when no articles exist`() {
        every { jdbcTemplate.query(any<String>(), any<Array<Any>>(), any<RowMapper<SourceArticleCounts>>()) } returns emptyList()

        val result = service.getArticleCounts(listOf("s1"), 5)
        assertEquals(emptyMap<String, SourceArticleCounts>(), result)
    }
}
