package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.BriefingProperties
import com.aisummarypodcast.config.EncryptionProperties
import com.aisummarypodcast.config.EpisodesProperties
import com.aisummarypodcast.config.FeedProperties
import com.aisummarypodcast.config.LlmProperties
import com.aisummarypodcast.store.Podcast
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ResolveTemperatureTest {

    private val appProperties = AppProperties(
        llm = LlmProperties(),
        briefing = BriefingProperties(defaultTemperature = 0.95),
        episodes = EpisodesProperties(),
        feed = FeedProperties(),
        encryption = EncryptionProperties(masterKey = "test")
    )

    @Test
    fun `falls back to system default when composeSettings is null`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "T", topic = "t", composeSettings = null)
        assertEquals(0.95, resolveTemperature(podcast, appProperties))
    }

    @Test
    fun `falls back to system default when temperature key is missing`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "T", topic = "t", composeSettings = emptyMap())
        assertEquals(0.95, resolveTemperature(podcast, appProperties))
    }

    @Test
    fun `falls back to system default when temperature is unparseable`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "T", topic = "t", composeSettings = mapOf("temperature" to "warm"))
        assertEquals(0.95, resolveTemperature(podcast, appProperties))
    }

    @Test
    fun `uses podcast override when present and in-range`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "T", topic = "t", composeSettings = mapOf("temperature" to "0.6"))
        assertEquals(0.6, resolveTemperature(podcast, appProperties))
    }

    @Test
    fun `clamps negative override to 0`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "T", topic = "t", composeSettings = mapOf("temperature" to "-0.5"))
        assertEquals(0.0, resolveTemperature(podcast, appProperties))
    }

    @Test
    fun `clamps over-range override to 2`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "T", topic = "t", composeSettings = mapOf("temperature" to "5.0"))
        assertEquals(2.0, resolveTemperature(podcast, appProperties))
    }

    @Test
    fun `ignores unrecognised compose-settings keys`() {
        val podcast = Podcast(
            id = "p1", userId = "u1", name = "T", topic = "t",
            composeSettings = mapOf("temperature" to "0.7", "unknown" to "value")
        )
        assertEquals(0.7, resolveTemperature(podcast, appProperties))
    }
}
