package com.aisummarypodcast.podcast

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.EpisodesProperties
import com.aisummarypodcast.config.FeedProperties
import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.EpisodeStatus
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.User
import com.aisummarypodcast.store.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional

class StaticFeedExporterTest {

    private val episodeRepository = mockk<EpisodeRepository>()
    private val userRepository = mockk<UserRepository>()

    @TempDir
    lateinit var tempDir: Path

    private fun createExporter(staticBaseUrl: String? = null): StaticFeedExporter {
        val feedProperties = FeedProperties(
            baseUrl = "http://localhost:8085",
            title = "AI Summary Podcast",
            description = "Test feed",
            staticBaseUrl = staticBaseUrl
        )
        val appProperties = mockk<AppProperties>().also {
            every { it.feed } returns feedProperties
            every { it.episodes } returns EpisodesProperties(directory = tempDir.toString())
        }
        val feedGenerator = FeedGenerator(episodeRepository, appProperties)
        return StaticFeedExporter(feedGenerator, userRepository, appProperties)
    }

    @Test
    fun `exports feed xml with correct content`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Tech", topic = "tech")
        val user = User(id = "u1", name = "Test User")
        every { userRepository.findById("u1") } returns Optional.of(user)
        every { episodeRepository.findByPodcastIdAndStatus("p1", "GENERATED") } returns emptyList()

        val exporter = createExporter()
        exporter.export(podcast)

        val feedFile = tempDir.resolve("p1/feed.xml")
        assertTrue(Files.exists(feedFile), "feed.xml should be created")
        val content = Files.readString(feedFile)
        assertTrue(content.contains("<rss"), "Should contain RSS XML")
        assertTrue(content.contains("AI Summary Podcast - Test User - Tech"), "Should contain feed title")
    }

    @Test
    fun `uses static base url in enclosure urls when configured`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Tech", topic = "tech")
        val user = User(id = "u1", name = "Test User")
        val episode = Episode(
            id = 1L, podcastId = "p1", generatedAt = "2025-01-01T00:00:00Z",
            scriptText = "Episode script", status = EpisodeStatus.GENERATED,
            audioFilePath = "/data/episodes/p1/briefing-20250101-000000.mp3", durationSeconds = 120
        )
        every { userRepository.findById("u1") } returns Optional.of(user)
        every { episodeRepository.findByPodcastIdAndStatus("p1", "GENERATED") } returns listOf(episode)

        val exporter = createExporter(staticBaseUrl = "https://cdn.example.com")
        exporter.export(podcast)

        val content = Files.readString(tempDir.resolve("p1/feed.xml"))
        assertTrue(
            content.contains("https://cdn.example.com/episodes/p1/briefing-20250101-000000.mp3"),
            "Enclosure URL should use static base URL"
        )
    }

    @Test
    fun `logs warning on write failure without throwing`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Tech", topic = "tech")
        val user = User(id = "u1", name = "Test User")
        every { userRepository.findById("u1") } returns Optional.of(user)
        every { episodeRepository.findByPodcastIdAndStatus("p1", "GENERATED") } returns emptyList()

        val feedGenerator = mockk<FeedGenerator>()
        every { feedGenerator.generate(any(), any(), any()) } throws RuntimeException("disk full")

        val appProperties = mockk<AppProperties>().also {
            every { it.feed } returns FeedProperties(staticBaseUrl = null)
            every { it.episodes } returns EpisodesProperties(directory = tempDir.toString())
        }

        val exporter = StaticFeedExporter(feedGenerator, userRepository, appProperties)

        // Should not throw
        exporter.export(podcast)

        // Verify generate was called (and failed)
        verify { feedGenerator.generate(podcast, user, any()) }
    }
}
