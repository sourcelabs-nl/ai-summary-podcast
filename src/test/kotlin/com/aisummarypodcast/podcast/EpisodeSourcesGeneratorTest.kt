package com.aisummarypodcast.podcast

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.Podcast
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class EpisodeSourcesGeneratorTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var generator: EpisodeSourcesGenerator

    private val podcast = Podcast(id = "pod-1", userId = "user-1", name = "Tech Daily", topic = "tech")
    private val episode = Episode(
        id = 1,
        podcastId = "pod-1",
        generatedAt = "2026-03-12T10:00:00Z",
        scriptText = "Some script",
        recap = "Today we covered breaking news in tech.",
        audioFilePath = "${System.getProperty("java.io.tmpdir")}/pod-1/briefing-20260312-100000.mp3"
    )

    @BeforeEach
    fun setup() {
        val appProperties = mockk<AppProperties> {
            every { episodes } returns mockk {
                every { directory } returns tempDir.toString()
            }
        }
        generator = EpisodeSourcesGenerator(appProperties)
    }

    @Test
    fun `generates sources md with recap and articles`() {
        val articles = listOf(
            article(1, "Spring Boot 4 Released", "https://spring.io/blog/spring-boot-4"),
            article(2, "Kotlin 2.2 Features", "https://blog.jetbrains.com/kotlin-2.2")
        )

        val path = generator.generate(episode, podcast, articles)

        assertNotNull(path)
        val content = Files.readString(path!!)
        assertTrue(content.contains("# Tech Daily"))
        assertTrue(content.contains("**Episode date:** 2026-03-12"))
        assertTrue(content.contains("Today we covered breaking news in tech."))
        assertTrue(content.contains("- [Spring Boot 4 Released](https://spring.io/blog/spring-boot-4)"))
        assertTrue(content.contains("- [Kotlin 2.2 Features](https://blog.jetbrains.com/kotlin-2.2)"))
    }

    @Test
    fun `generates sources md with recap only`() {
        val path = generator.generate(episode, podcast, emptyList())

        assertNotNull(path)
        val content = Files.readString(path!!)
        assertTrue(content.contains("# Tech Daily"))
        assertTrue(content.contains("Today we covered"))
        assertFalse(content.contains("## Sources"))
    }

    @Test
    fun `returns null when no recap and no articles`() {
        val noRecapEpisode = episode.copy(recap = null)
        assertNull(generator.generate(noRecapEpisode, podcast, emptyList()))
    }

    @Test
    fun `derives slug from audio file path`() {
        assertEquals("briefing-20260312-100000", generator.deriveSlug(episode))
    }

    @Test
    fun `derives slug from generatedAt when no audio path`() {
        val noAudioEpisode = episode.copy(audioFilePath = null)
        assertEquals("briefing-20260312-100000", generator.deriveSlug(noAudioEpisode))
    }

    @Test
    fun `file is written to correct path`() {
        val path = generator.generate(episode, podcast, emptyList())

        assertNotNull(path)
        assertTrue(path!!.toString().endsWith("briefing-20260312-100000-sources.txt"))
        assertTrue(path.toString().contains("pod-1"))
    }

    private fun article(id: Long, title: String, url: String) = Article(
        id = id, sourceId = "src-1", title = title, body = "body", url = url,
        contentHash = "hash-$id", relevanceScore = 8
    )
}
