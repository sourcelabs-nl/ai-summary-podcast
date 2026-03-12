package com.aisummarypodcast.podcast

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.FeedProperties
import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.EpisodeStatus
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.User
import com.aisummarypodcast.store.EpisodePublicationRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

class FeedGeneratorTest {

    private val episodeRepository = mockk<EpisodeRepository>()
    private val podcastImageService = mockk<PodcastImageService>()
    private val episodeSourcesGenerator = mockk<EpisodeSourcesGenerator>()
    private val publicationRepository = mockk<EpisodePublicationRepository>()
    private val appProperties = mockk<AppProperties>().also {
        every { it.feed } returns FeedProperties(
            baseUrl = "http://localhost:8085",
            title = "AI Summary Podcast",
            description = "Test feed"
        )
    }
    private val feedGenerator = FeedGenerator(episodeRepository, appProperties, podcastImageService, episodeSourcesGenerator, publicationRepository)

    private val podcast = Podcast(id = "p1", userId = "u1", name = "Tech Daily", topic = "tech")
    private val user = User(id = "u1", name = "Test User")

    @Test
    fun `feed title is podcast name only`() {
        every { episodeRepository.findByPodcastIdAndStatus("p1", "GENERATED") } returns emptyList()
        every { podcastImageService.get("p1") } returns null

        val xml = feedGenerator.generate(podcast, user)
        assertTrue(xml.contains("<title>Tech Daily</title>"), "Expected podcast name as feed title")
        assertFalse(xml.contains("AI Summary Podcast"), "Expected no app title in feed")
    }

    @Test
    fun `feed includes language element matching podcast language`() {
        val nlPodcast = podcast.copy(language = "nl")
        every { episodeRepository.findByPodcastIdAndStatus("p1", "GENERATED") } returns emptyList()
        every { podcastImageService.get("p1") } returns null

        val xml = feedGenerator.generate(nlPodcast, user)
        assertTrue(xml.contains("<language>nl</language>"))
    }

    @Test
    fun `feed includes image element when publicUrl and image are present`() {
        every { episodeRepository.findByPodcastIdAndStatus("p1", "GENERATED") } returns emptyList()
        every { podcastImageService.get("p1") } returns Path.of("/data/p1/podcast-image.jpg")

        val xml = feedGenerator.generate(podcast, user, publicUrl = "https://podcast.example.com/shows/tech/")
        assertTrue(xml.contains("https://podcast.example.com/shows/tech/podcast-image.jpg"), "Expected image URL in feed")
    }

    @Test
    fun `feed includes image with local url when no publicUrl`() {
        every { episodeRepository.findByPodcastIdAndStatus("p1", "GENERATED") } returns emptyList()
        every { podcastImageService.get("p1") } returns Path.of("/data/p1/podcast-image.jpg")

        val xml = feedGenerator.generate(podcast, user)
        assertTrue(xml.contains("http://localhost:8085/data/p1/podcast-image.jpg"), "Expected local image URL in feed")
    }

    @Test
    fun `feed omits image element when no image exists`() {
        every { episodeRepository.findByPodcastIdAndStatus("p1", "GENERATED") } returns emptyList()
        every { podcastImageService.get("p1") } returns null

        val xml = feedGenerator.generate(podcast, user, publicUrl = "https://podcast.example.com/shows/tech/")
        assertFalse(xml.contains("<image>"), "Expected no image element when podcast has no image")
    }

    @Test
    fun `episode description includes sources link when publicUrl set`() {
        val episode = Episode(
            id = 1L, podcastId = "p1", generatedAt = "2025-01-01T00:00:00Z",
            scriptText = "Full script", status = EpisodeStatus.GENERATED,
            audioFilePath = "/data/p1/episodes/briefing-20250101-000000.mp3", durationSeconds = 120,
            showNotes = "Today's recap summary."
        )
        every { episodeRepository.findByPodcastIdAndStatus("p1", "GENERATED") } returns listOf(episode)
        every { podcastImageService.get("p1") } returns null
        every { episodeSourcesGenerator.deriveSlug(episode) } returns "briefing-20250101-000000"

        val xml = feedGenerator.generate(podcast, user, publicUrl = "https://podcast.example.com/shows/tech/")
        assertTrue(xml.contains("Today's recap summary."), "Expected recap in description")
        assertTrue(xml.contains("Sources: https://podcast.example.com/shows/tech/episodes/briefing-20250101-000000-sources.txt"), "Expected sources link")
    }

    @Test
    fun `episode description includes local sources link without publicUrl`() {
        val episode = Episode(
            id = 1L, podcastId = "p1", generatedAt = "2025-01-01T00:00:00Z",
            scriptText = "Full script", status = EpisodeStatus.GENERATED,
            audioFilePath = "/data/p1/episodes/briefing-20250101-000000.mp3", durationSeconds = 120,
            showNotes = "Today's recap summary."
        )
        every { episodeRepository.findByPodcastIdAndStatus("p1", "GENERATED") } returns listOf(episode)
        every { podcastImageService.get("p1") } returns null
        every { episodeSourcesGenerator.deriveSlug(episode) } returns "briefing-20250101-000000"

        val xml = feedGenerator.generate(podcast, user)
        assertTrue(xml.contains("Today's recap summary."), "Expected recap in description")
        assertTrue(xml.contains("Sources: http://localhost:8085/data/p1/episodes/briefing-20250101-000000-sources.txt"), "Expected local sources link")
    }

    @Test
    fun `feed uses show notes when available`() {
        val episode = Episode(
            id = 1L, podcastId = "p1", generatedAt = "2025-01-01T00:00:00Z",
            scriptText = "Full script text here", status = EpisodeStatus.GENERATED,
            audioFilePath = "/data/p1/episodes/briefing-20250101-000000.mp3", durationSeconds = 120,
            showNotes = "Recap summary."
        )
        every { episodeRepository.findByPodcastIdAndStatus("p1", "GENERATED") } returns listOf(episode)
        every { podcastImageService.get("p1") } returns null
        every { episodeSourcesGenerator.deriveSlug(episode) } returns "briefing-20250101-000000"

        val xml = feedGenerator.generate(podcast, user)
        assertTrue(xml.contains("Recap summary."), "Expected show notes in feed description")
        assertFalse(xml.contains("Full script text here"), "Expected script text NOT in feed when show notes present")
    }

    @Test
    fun `feed excludes pending review episodes`() {
        every { episodeRepository.findByPodcastIdAndStatus("p1", "GENERATED") } returns emptyList()
        every { podcastImageService.get("p1") } returns null

        val xml = feedGenerator.generate(podcast, user)
        assertFalse(xml.contains("<item>"), "Expected no items in feed")
    }
}
