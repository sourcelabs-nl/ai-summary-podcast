package com.aisummarypodcast.tts

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ElevenLabsTtsProvider(
    private val apiClient: ElevenLabsApiClient
) : TtsProvider {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun generate(request: TtsRequest): TtsResult {
        val voiceId = request.ttsVoices["default"]
            ?: throw IllegalStateException("ElevenLabs TTS requires a 'default' voice in ttsVoices")

        val chunks = TextChunker.chunk(request.script)
        log.info("Generating ElevenLabs TTS audio for {} chunks (voice: {}, language: {})", chunks.size, voiceId, request.language)

        val voiceSettings = request.ttsSettings.ifEmpty { null }
        val totalCharacters = chunks.sumOf { it.length }

        val audioChunks = chunks.mapIndexed { index, chunk ->
            log.info("Generating ElevenLabs TTS chunk {}/{} ({} chars)", index + 1, chunks.size, chunk.length)
            apiClient.textToSpeech(request.userId, voiceId, chunk, voiceSettings)
        }

        return TtsResult(audioChunks, totalCharacters, requiresConcatenation = chunks.size > 1, model = "eleven_flash_v2_5")
    }
}
