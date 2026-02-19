package com.aisummarypodcast.tts

import com.aisummarypodcast.store.Podcast
import org.springframework.stereotype.Component

@Component
class TtsProviderFactory(
    private val openAiTtsProvider: OpenAiTtsProvider,
    private val elevenLabsTtsProvider: ElevenLabsTtsProvider,
    private val elevenLabsDialogueTtsProvider: ElevenLabsDialogueTtsProvider
) {

    fun resolve(podcast: Podcast): TtsProvider = when (podcast.ttsProvider) {
        "openai" -> openAiTtsProvider
        "elevenlabs" -> if (podcast.style in setOf("dialogue", "interview")) elevenLabsDialogueTtsProvider else elevenLabsTtsProvider
        else -> throw IllegalArgumentException("Unsupported TTS provider: ${podcast.ttsProvider}. Supported: openai, elevenlabs")
    }
}
