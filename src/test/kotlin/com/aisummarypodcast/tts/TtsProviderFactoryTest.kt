package com.aisummarypodcast.tts

import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastStyle
import com.aisummarypodcast.store.TtsProviderType
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class TtsProviderFactoryTest {

    private val openAiProvider = mockk<OpenAiTtsProvider>()
    private val elevenLabsProvider = mockk<ElevenLabsTtsProvider>()
    private val elevenLabsDialogueProvider = mockk<ElevenLabsDialogueTtsProvider>()

    private val factory = TtsProviderFactory(openAiProvider, elevenLabsProvider, elevenLabsDialogueProvider)

    @Test
    fun `resolves OpenAI provider`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech", ttsProvider = TtsProviderType.OPENAI)
        assertSame(openAiProvider, factory.resolve(podcast))
    }

    @Test
    fun `resolves ElevenLabs monologue provider`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech", ttsProvider = TtsProviderType.ELEVENLABS, style = PodcastStyle.NEWS_BRIEFING)
        assertSame(elevenLabsProvider, factory.resolve(podcast))
    }

    @Test
    fun `resolves ElevenLabs dialogue provider for dialogue style`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech", ttsProvider = TtsProviderType.ELEVENLABS, style = PodcastStyle.DIALOGUE)
        assertSame(elevenLabsDialogueProvider, factory.resolve(podcast))
    }

    @Test
    fun `resolves ElevenLabs dialogue provider for interview style`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech", ttsProvider = TtsProviderType.ELEVENLABS, style = PodcastStyle.INTERVIEW)
        assertSame(elevenLabsDialogueProvider, factory.resolve(podcast))
    }

}
