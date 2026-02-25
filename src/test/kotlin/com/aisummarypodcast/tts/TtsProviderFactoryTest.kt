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
    private val inworldProvider = mockk<InworldTtsProvider>()

    private val factory = TtsProviderFactory(openAiProvider, elevenLabsProvider, elevenLabsDialogueProvider, inworldProvider)

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

    @Test
    fun `resolves Inworld provider for monologue style`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech", ttsProvider = TtsProviderType.INWORLD, style = PodcastStyle.NEWS_BRIEFING)
        assertSame(inworldProvider, factory.resolve(podcast))
    }

    @Test
    fun `resolves Inworld provider for casual style`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech", ttsProvider = TtsProviderType.INWORLD, style = PodcastStyle.CASUAL)
        assertSame(inworldProvider, factory.resolve(podcast))
    }

    @Test
    fun `resolves Inworld provider for dialogue style`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech", ttsProvider = TtsProviderType.INWORLD, style = PodcastStyle.DIALOGUE)
        assertSame(inworldProvider, factory.resolve(podcast))
    }

    @Test
    fun `resolves Inworld provider for interview style`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech", ttsProvider = TtsProviderType.INWORLD, style = PodcastStyle.INTERVIEW)
        assertSame(inworldProvider, factory.resolve(podcast))
    }

}
