package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.BriefingProperties
import com.aisummarypodcast.config.EncryptionProperties
import com.aisummarypodcast.config.EpisodesProperties
import com.aisummarypodcast.config.FeedProperties
import com.aisummarypodcast.config.LlmProperties
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastStyle
import com.aisummarypodcast.store.TtsProviderType
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
        style = PodcastStyle.INTERVIEW,
        ttsProvider = TtsProviderType.ELEVENLABS,
        ttsVoices = mapOf("interviewer" to "v1", "expert" to "v2"),
        speakerNames = mapOf("interviewer" to "Alice", "expert" to "Bob"),
        fullBodyThreshold = 1
    )

    private val articles = listOf(
        Article(sourceId = "s1", title = "AI News", body = "AI is advancing.", url = "https://example.com/ai", contentHash = "h1", summary = "AI progress."),
        Article(sourceId = "s1", title = "Cloud News", body = "Cloud is growing.", url = "https://example.com/cloud", contentHash = "h2", summary = "Cloud growth.")
    )

    @Test
    fun `prompt includes sponsor message instructions`() {
        val prompt = composer.buildPrompt(articles, podcast)
        assertTrue(prompt.contains("This podcast is brought to you by source-labs"))
        assertTrue(prompt.contains("End with a sign-off that includes a mention of the sponsor: source-labs"))
    }

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
        assertTrue(prompt.contains("never as bare turn openers"))
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
    fun `prompt includes transition guidance`() {
        val prompt = composer.buildPrompt(articles, podcast)

        assertTrue(prompt.contains("do NOT start a turn with a bare name address"))
        assertTrue(prompt.contains("conversational bridges"))
        assertTrue(prompt.contains("Vary transition patterns"))
    }

    @Test
    fun `prompt includes TTS guidelines when provided`() {
        val guidelines = "You MAY include emotion cues in square brackets."
        val prompt = composer.buildPrompt(articles, podcast, ttsScriptGuidelines = guidelines)

        assertTrue(prompt.contains("TTS script formatting:"))
        assertTrue(prompt.contains("emotion cues in square brackets"))
    }

    @Test
    fun `prompt omits TTS guidelines when empty`() {
        val prompt = composer.buildPrompt(articles, podcast, ttsScriptGuidelines = "")

        assertFalse(prompt.contains("TTS script formatting:"))
    }

    @Test
    fun `prompt does not include hardcoded emotion cues`() {
        val prompt = composer.buildPrompt(articles, podcast)

        assertFalse(prompt.contains("[curious]"))
    }

    @Test
    fun `prompt uses full body when article count is below threshold`() {
        val podcastDefault = podcast.copy(fullBodyThreshold = null) // falls back to appProperties default of 5
        val fewArticles = listOf(
            Article(sourceId = "s1", title = "AI News", body = "Full AI body.", url = "https://example.com/ai", contentHash = "h1", summary = "AI summary.")
        )
        // Default threshold is 5, 1 article < 5 → use full body
        val prompt = composer.buildPrompt(fewArticles, podcastDefault)

        assertTrue(prompt.contains("Full AI body."))
        assertFalse(prompt.contains("AI summary."))
    }

    @Test
    fun `prompt uses summaries when article count is at or above threshold`() {
        val podcastDefault = podcast.copy(fullBodyThreshold = null) // falls back to appProperties default of 5
        val manyArticles = (1..5).map { i ->
            Article(sourceId = "s1", title = "News $i", body = "Body $i.", url = "https://example.com/$i", contentHash = "h$i", summary = "Summary $i.")
        }
        val prompt = composer.buildPrompt(manyArticles, podcastDefault)

        assertTrue(prompt.contains("Summary 1."))
        assertFalse(prompt.contains("Body 1."))
    }
}
