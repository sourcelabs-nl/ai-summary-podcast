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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/**
 * Guards against verbatim example phrases creeping back into composer prompts. The LLM was
 * copying these word-for-word into generated scripts, which is exactly what `add-script-variety`
 * removes. Fails CI if any banned phrase reappears in any composer prompt.
 */
class ComposerBannedPromptsTest {

    private val appProperties = AppProperties(
        llm = LlmProperties(),
        briefing = BriefingProperties(targetWords = 1500),
        episodes = EpisodesProperties(),
        feed = FeedProperties(),
        encryption = EncryptionProperties(masterKey = "test-key")
    )
    private val varietyPicker = PromptVarietyPicker()

    private val sampleArticles = (1..6).map { i ->
        Article(
            id = i.toLong(), sourceId = "s1", title = "Article $i",
            body = "Body $i.", url = "https://example.com/$i",
            contentHash = "h$i", summary = "Summary $i."
        )
    }

    @ParameterizedTest
    @EnumSource(PodcastStyle::class)
    fun `built compose prompt contains no banned example phrases`(style: PodcastStyle) {
        val prompt = buildPromptFor(style)
        BannedPrompts.phrases.forEach { banned ->
            assertFalse(
                prompt.contains(banned),
                "Compose prompt for style $style still contains banned phrase: \"$banned\""
            )
        }
    }

    private fun buildPromptFor(style: PodcastStyle): String {
        val podcast = podcastForStyle(style)
        return when (style) {
            PodcastStyle.DIALOGUE ->
                DialogueComposer(appProperties, mockk(), mockk(), varietyPicker).buildPrompt(sampleArticles, podcast)

            PodcastStyle.INTERVIEW ->
                InterviewComposer(appProperties, mockk(), mockk(), varietyPicker).buildPrompt(sampleArticles, podcast)

            else ->
                BriefingComposer(appProperties, mockk(), mockk(), varietyPicker).buildPrompt(sampleArticles, podcast)
        }
    }

    private fun podcastForStyle(style: PodcastStyle): Podcast = when (style) {
        PodcastStyle.DIALOGUE -> Podcast(
            id = "p-dialogue", userId = "u1", name = "Test", topic = "tech",
            style = PodcastStyle.DIALOGUE,
            ttsProvider = TtsProviderType.ELEVENLABS,
            ttsVoices = mapOf("host" to "v1", "cohost" to "v2")
        )

        PodcastStyle.INTERVIEW -> Podcast(
            id = "p-interview", userId = "u1", name = "Test", topic = "tech",
            style = PodcastStyle.INTERVIEW,
            ttsProvider = TtsProviderType.ELEVENLABS,
            ttsVoices = mapOf("interviewer" to "v1", "expert" to "v2")
        )

        else -> Podcast(id = "p-${style.value}", userId = "u1", name = "Test", topic = "tech", style = style)
    }
}
