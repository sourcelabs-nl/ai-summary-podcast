package com.aisummarypodcast.podcast

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.FeedProperties
import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.EpisodeStatus
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.User
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FeedGeneratorTest {

    private val episodeRepository = mockk<EpisodeRepository>()
    private val appProperties = mockk<AppProperties>().also {
        every { it.feed } returns FeedProperties(
            baseUrl = "http://localhost:8085",
            title = "AI Summary Podcast",
            description = "Test feed"
        )
    }
    private val feedGenerator = FeedGenerator(episodeRepository, appProperties)

    @Test
    fun `feed includes language element matching podcast language`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Tech NL", topic = "tech", language = "nl")
        val user = User(id = "u1", name = "Test User")
        every { episodeRepository.findByPodcastIdAndStatus("p1", "GENERATED") } returns emptyList()

        val xml = feedGenerator.generate(podcast, user)
        assertTrue(xml.contains("<language>nl</language>"), "Expected <language>nl</language> in RSS feed XML")
    }

    @Test
    fun `feed includes English language element for default podcast`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Tech EN", topic = "tech", language = "en")
        val user = User(id = "u1", name = "Test User")
        every { episodeRepository.findByPodcastIdAndStatus("p1", "GENERATED") } returns emptyList()

        val xml = feedGenerator.generate(podcast, user)
        assertTrue(xml.contains("<language>en</language>"), "Expected <language>en</language> in RSS feed XML")
    }

    @Test
    fun `feed only includes GENERATED episodes`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Tech", topic = "tech")
        val user = User(id = "u1", name = "Test User")

        val generatedEpisode = Episode(
            id = 1L, podcastId = "p1", generatedAt = "2025-01-01T00:00:00Z",
            scriptText = "Generated episode script", status = EpisodeStatus.GENERATED,
            audioFilePath = "/data/episodes/p1/briefing-20250101-000000.mp3", durationSeconds = 120
        )

        every { episodeRepository.findByPodcastIdAndStatus("p1", "GENERATED") } returns listOf(generatedEpisode)

        val xml = feedGenerator.generate(podcast, user)
        assertTrue(xml.contains("Generated episode script..."), "Expected generated episode in feed")
        assertTrue(xml.contains("briefing-20250101-000000.mp3"), "Expected audio file reference in feed")
    }

    @Test
    fun `feed excludes pending review episodes`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Tech", topic = "tech")
        val user = User(id = "u1", name = "Test User")

        // Only GENERATED episodes are returned by the repository query
        every { episodeRepository.findByPodcastIdAndStatus("p1", "GENERATED") } returns emptyList()

        val xml = feedGenerator.generate(podcast, user)
        assertFalse(xml.contains("<item>"), "Expected no items in feed when no GENERATED episodes exist")
    }
}
