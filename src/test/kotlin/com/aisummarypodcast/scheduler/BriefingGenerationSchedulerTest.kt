package com.aisummarypodcast.scheduler

import com.aisummarypodcast.podcast.PodcastService
import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodeStatus
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastRepository
import io.mockk.*
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class BriefingGenerationSchedulerTest {

    private val podcastRepository = mockk<PodcastRepository>()
    private val podcastService = mockk<PodcastService>()

    // Fixed clock at 00:05 UTC — 5 minutes after the midnight cron trigger (within staleness window)
    private val defaultNow = Instant.parse("2026-02-23T00:05:00Z")
    private val defaultClock = Clock.fixed(defaultNow, ZoneOffset.UTC)

    private val scheduler = BriefingGenerationScheduler(
        podcastRepository, podcastService, defaultClock
    )

    private fun duePodcast(requireReview: Boolean = false) = Podcast(
        id = "p1", userId = "u1", name = "Test", topic = "tech",
        cron = "0 0 0 * * *",
        requireReview = requireReview,
        lastGeneratedAt = "2026-02-22T00:00:00Z"
    )

    private fun schedulerWithClock(instant: Instant): BriefingGenerationScheduler {
        val clock = Clock.fixed(instant, ZoneOffset.UTC)
        return BriefingGenerationScheduler(podcastRepository, podcastService, clock)
    }

    @Test
    fun `skips generation when podcastService returns null`() {
        val podcast = duePodcast(requireReview = true)
        every { podcastRepository.findAll() } returns listOf(podcast)
        every { podcastService.generateBriefing(podcast) } returns null

        scheduler.checkAndGenerate()

        verify { podcastService.generateBriefing(podcast) }
    }

    @Test
    fun `delegates to PodcastService when due`() {
        val podcast = duePodcast(requireReview = false)
        val episode = Episode(id = 10, podcastId = "p1", generatedAt = Instant.now().toString(), scriptText = "Generated script")

        every { podcastRepository.findAll() } returns listOf(podcast)
        every { podcastService.generateBriefing(podcast) } returns episode

        scheduler.checkAndGenerate()

        verify { podcastService.generateBriefing(podcast) }
    }

    @Test
    fun `delegates to PodcastService for review episode`() {
        val podcast = duePodcast(requireReview = true)
        val episode = Episode(id = 7, podcastId = "p1", generatedAt = Instant.now().toString(), scriptText = "Script", status = EpisodeStatus.PENDING_REVIEW)

        every { podcastRepository.findAll() } returns listOf(podcast)
        every { podcastService.generateBriefing(podcast) } returns episode

        scheduler.checkAndGenerate()

        verify { podcastService.generateBriefing(podcast) }
    }

    @Test
    fun `handles null result from generateBriefing`() {
        val podcast = duePodcast(requireReview = false)
        every { podcastRepository.findAll() } returns listOf(podcast)
        every { podcastService.generateBriefing(podcast) } returns null

        scheduler.checkAndGenerate()

        verify { podcastService.generateBriefing(podcast) }
    }

    @Test
    fun `trigger within staleness window still fires`() {
        // Cron: daily at 15:00. Now: 15:10 (10 min past trigger, within 30 min window)
        val now = Instant.parse("2026-02-23T15:10:00Z")
        val podcast = Podcast(
            id = "p1", userId = "u1", name = "Test", topic = "tech",
            cron = "0 0 15 * * *",
            lastGeneratedAt = "2026-02-22T15:00:00Z"
        )
        val episode = Episode(id = 1, podcastId = "p1", generatedAt = now.toString(), scriptText = "Script")

        every { podcastRepository.findAll() } returns listOf(podcast)
        every { podcastService.generateBriefing(podcast) } returns episode

        schedulerWithClock(now).checkAndGenerate()

        verify { podcastService.generateBriefing(podcast) }
    }

    @Test
    fun `trigger beyond staleness window is skipped`() {
        // Cron: daily at 15:00. Now: 18:00 (3 hours past trigger, beyond 30 min window)
        val now = Instant.parse("2026-02-23T18:00:00Z")
        val podcast = Podcast(
            id = "p1", userId = "u1", name = "Test", topic = "tech",
            cron = "0 0 15 * * *",
            lastGeneratedAt = "2026-02-22T15:00:00Z"
        )

        every { podcastRepository.findAll() } returns listOf(podcast)

        schedulerWithClock(now).checkAndGenerate()

        verify(exactly = 0) { podcastService.generateBriefing(any()) }
    }

    @Test
    fun `multiple missed triggers are all skipped`() {
        // Cron: daily at 15:00. Last generated 3 days ago. Now: 10:00 (all 3 past triggers are stale)
        val now = Instant.parse("2026-02-23T10:00:00Z")
        val podcast = Podcast(
            id = "p1", userId = "u1", name = "Test", topic = "tech",
            cron = "0 0 15 * * *",
            lastGeneratedAt = "2026-02-19T15:00:00Z"
        )

        every { podcastRepository.findAll() } returns listOf(podcast)

        schedulerWithClock(now).checkAndGenerate()

        // All 3 missed triggers (Feb 20, 21, 22) are stale, today's (Feb 23 15:00) is in the future
        verify(exactly = 0) { podcastService.generateBriefing(any()) }
    }

    @Test
    fun `skipped triggers do not call generateBriefing`() {
        // Cron: daily at 15:00. Now: 18:00 (3 hours past trigger)
        val now = Instant.parse("2026-02-23T18:00:00Z")
        val podcast = Podcast(
            id = "p1", userId = "u1", name = "Test", topic = "tech",
            cron = "0 0 15 * * *",
            lastGeneratedAt = "2026-02-22T15:00:00Z"
        )

        every { podcastRepository.findAll() } returns listOf(podcast)

        schedulerWithClock(now).checkAndGenerate()

        verify(exactly = 0) { podcastService.generateBriefing(any()) }
    }
}
