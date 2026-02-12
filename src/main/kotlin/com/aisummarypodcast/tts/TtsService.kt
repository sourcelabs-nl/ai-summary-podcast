package com.aisummarypodcast.tts

import com.aisummarypodcast.store.ApiKeyCategory
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.user.UserApiKeyService
import org.slf4j.LoggerFactory
import org.springframework.ai.audio.tts.TextToSpeechPrompt
import org.springframework.ai.openai.OpenAiAudioSpeechModel
import org.springframework.ai.openai.OpenAiAudioSpeechOptions
import org.springframework.ai.openai.api.OpenAiAudioApi
import org.springframework.stereotype.Service

@Service
class TtsService(
    private val userApiKeyService: UserApiKeyService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun generateAudio(chunks: List<String>, podcast: Podcast): List<ByteArray> {
        log.info("Generating TTS audio for {} chunks (voice: {}, speed: {})", chunks.size, podcast.ttsVoice, podcast.ttsSpeed)

        val speechModel = createSpeechModel(podcast)

        val options = OpenAiAudioSpeechOptions.builder()
            .voice(podcast.ttsVoice)
            .speed(podcast.ttsSpeed)
            .build()

        return chunks.mapIndexed { index, chunk ->
            log.info("Generating TTS chunk {}/{} ({} chars)", index + 1, chunks.size, chunk.length)
            val response = speechModel.call(TextToSpeechPrompt(chunk, options))
            response.result.output
        }
    }

    private fun createSpeechModel(podcast: Podcast): OpenAiAudioSpeechModel {
        val apiKey = userApiKeyService.resolveKey(podcast.userId, ApiKeyCategory.TTS)
            ?: throw IllegalStateException("No API key available for category 'TTS'. Configure a user API key or set the OPENAI_API_KEY environment variable.")

        val audioApi = OpenAiAudioApi.builder()
            .apiKey(apiKey)
            .baseUrl("https://api.openai.com")
            .build()
        return OpenAiAudioSpeechModel(audioApi)
    }
}
