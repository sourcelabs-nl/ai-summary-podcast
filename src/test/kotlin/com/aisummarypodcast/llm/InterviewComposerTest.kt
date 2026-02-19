package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.BriefingProperties
import com.aisummarypodcast.config.EncryptionProperties
import com.aisummarypodcast.config.EpisodesProperties
import com.aisummarypodcast.config.FeedProperties
import com.aisummarypodcast.config.LlmProperties
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.Podcast
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InterviewComposerTest {

    private val appProperties = AppProperties(
        llm = LlmProperties(),
        briefing = BriefingProperties(targetWords = 1000),
        episodes = EpisodesProperties(),
        feed = FeedProperties(),
        encryption = EncryptionProperties(masterKey = "test-key")
    )

    private val composer = InterviewComposer(appProperties, mockk(), mockk())

    private val podcast = Podcast(
        id = "p1", userId = "u1", name = "Tech Talk", topic = "tech",
        style = "interview",
        ttsProvider = "elevenlabs",
        ttsVoices = mapOf("interviewer" to "v1", "expert" to "v2"),
        speakerNames = mapOf("interviewer" to "Alice", "expert" to "Bob")
    )

    private val articles = listOf(
        Article(sourceId = "s1", title = "AI News", body = "AI is advancing.", url = "https://example.com/ai", contentHash = "h1", summary = "AI progress."),
        Article(sourceId = "s1", title = "Cloud News", body = "Cloud is growing.", url = "https://example.com/cloud", contentHash = "h2", summary = "Cloud growth.")
    )

    @Test
    fun `prompt includes grounding instruction`() {
        val prompt = composer.buildPrompt(articles, podcast)
        assertTrue(prompt.contains("ONLY discuss topics that are present in the article summaries"))
        assertTrue(prompt.contains("Do NOT introduce facts, stories, or claims from outside the provided articles"))
    }

    @Test
    fun `prompt includes speaker names when provided`() {
        val prompt = composer.buildPrompt(articles, podcast)

        assertTrue(prompt.contains("Alice"))
        assertTrue(prompt.contains("Bob"))
        assertTrue(prompt.contains("use each other's names naturally"))
    }

    @Test
    fun `prompt handles missing speaker names`() {
        val podcastWithoutNames = podcast.copy(speakerNames = null)
        val prompt = composer.buildPrompt(articles, podcastWithoutNames)

        assertFalse(prompt.contains("Alice"))
        assertFalse(prompt.contains("Bob"))
        assertTrue(prompt.contains("address each other without using names"))
    }

    @Test
    fun `prompt includes article summaries`() {
        val prompt = composer.buildPrompt(articles, podcast)

        assertTrue(prompt.contains("AI progress."))
        assertTrue(prompt.contains("Cloud growth."))
    }

    @Test
    fun `prompt includes interviewer and expert tags`() {
        val prompt = composer.buildPrompt(articles, podcast)

        assertTrue(prompt.contains("<interviewer>"))
        assertTrue(prompt.contains("<expert>"))
    }

    @Test
    fun `prompt specifies asymmetric word distribution`() {
        val prompt = composer.buildPrompt(articles, podcast)

        assertTrue(prompt.contains("~20%"))
        assertTrue(prompt.contains("~80%"))
    }

    @Test
    fun `prompt respects language`() {
        val dutchPodcast = podcast.copy(language = "nl")
        val prompt = composer.buildPrompt(articles, dutchPodcast)

        assertTrue(prompt.contains("Dutch"))
    }

    @Test
    fun `prompt includes continuity context when provided`() {
        val recap = "AI chip shortages continue. New EU regulations proposed."
        val prompt = composer.buildPrompt(articles, podcast, recap)

        assertTrue(prompt.contains("Previous episode context:"))
        assertTrue(prompt.contains("AI chip shortages continue."))
        assertTrue(prompt.contains("We talked about this last time"))
    }

    @Test
    fun `prompt excludes continuity context when null`() {
        val prompt = composer.buildPrompt(articles, podcast, null)

        assertFalse(prompt.contains("Previous episode context:"))
    }

    @Test
    fun `prompt includes custom instructions`() {
        val podcastWithInstructions = podcast.copy(customInstructions = "Focus on practical implications")
        val prompt = composer.buildPrompt(articles, podcastWithInstructions)

        assertTrue(prompt.contains("Focus on practical implications"))
    }

    @Test
    fun `prompt includes target word count`() {
        val prompt = composer.buildPrompt(articles, podcast)

        assertTrue(prompt.contains("1000"))
    }

    @Test
    fun `prompt includes podcast metadata`() {
        val prompt = composer.buildPrompt(articles, podcast)

        assertTrue(prompt.contains("Tech Talk"))
        assertTrue(prompt.contains("tech"))
    }

    @Test
    fun `prompt mentions emotion cues`() {
        val prompt = composer.buildPrompt(articles, podcast)

        assertTrue(prompt.contains("[curious]"))
    }
}
