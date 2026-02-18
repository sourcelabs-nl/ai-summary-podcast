package com.aisummarypodcast.tts

import com.aisummarypodcast.store.Podcast
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TtsProviderFactoryTest {

    private val openAiProvider = mockk<OpenAiTtsProvider>()
    private val elevenLabsProvider = mockk<ElevenLabsTtsProvider>()
    private val elevenLabsDialogueProvider = mockk<ElevenLabsDialogueTtsProvider>()

    private val factory = TtsProviderFactory(openAiProvider, elevenLabsProvider, elevenLabsDialogueProvider)

    @Test
    fun `resolves OpenAI provider`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech", ttsProvider = "openai")
        assertSame(openAiProvider, factory.resolve(podcast))
    }

    @Test
    fun `resolves ElevenLabs monologue provider`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech", ttsProvider = "elevenlabs", style = "news-briefing")
        assertSame(elevenLabsProvider, factory.resolve(podcast))
    }

    @Test
    fun `resolves ElevenLabs dialogue provider for dialogue style`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech", ttsProvider = "elevenlabs", style = "dialogue")
        assertSame(elevenLabsDialogueProvider, factory.resolve(podcast))
    }

    @Test
    fun `throws for unsupported provider`() {
        val podcast = Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech", ttsProvider = "azure")
        assertThrows<IllegalArgumentException> { factory.resolve(podcast) }
    }
}
