package com.aisummarypodcast.podcast

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.FeedProperties
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.User
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FeedGeneratorTest {

    private val episodeRepository = mockk<EpisodeRepository>()
    private val appProperties = mockk<AppProperties>().also {
        every { it.feed } returns FeedProperties(
            baseUrl = "http://localhost:8080",
            title = "AI Summary Podcast",
            description = "Test feed"
        )
    }
    private val feedGenerator = FeedGenerator(episodeRepository, appProperties)

    @Test
    fun `feed includes language element matching podcast language`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Tech NL", topic = "tech", language = "nl")
        val user = User(id = "u1", name = "Test User")
        every { episodeRepository.findByPodcastId("p1") } returns emptyList()

        val xml = feedGenerator.generate(podcast, user)
        assertTrue(xml.contains("<language>nl</language>"), "Expected <language>nl</language> in RSS feed XML")
    }

    @Test
    fun `feed includes English language element for default podcast`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Tech EN", topic = "tech", language = "en")
        val user = User(id = "u1", name = "Test User")
        every { episodeRepository.findByPodcastId("p1") } returns emptyList()

        val xml = feedGenerator.generate(podcast, user)
        assertTrue(xml.contains("<language>en</language>"), "Expected <language>en</language> in RSS feed XML")
    }
}
