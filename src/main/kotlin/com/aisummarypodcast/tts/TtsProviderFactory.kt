package com.aisummarypodcast.tts

import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastStyle
import com.aisummarypodcast.store.TtsProviderType
import org.springframework.stereotype.Component

@Component
class TtsProviderFactory(
    private val openAiTtsProvider: OpenAiTtsProvider,
    private val elevenLabsTtsProvider: ElevenLabsTtsProvider,
    private val elevenLabsDialogueTtsProvider: ElevenLabsDialogueTtsProvider
) {

    fun resolve(podcast: Podcast): TtsProvider = when (podcast.ttsProvider) {
        TtsProviderType.OPENAI -> openAiTtsProvider
        TtsProviderType.ELEVENLABS -> if (podcast.style in setOf(PodcastStyle.DIALOGUE, PodcastStyle.INTERVIEW)) elevenLabsDialogueTtsProvider else elevenLabsTtsProvider
    }
}
