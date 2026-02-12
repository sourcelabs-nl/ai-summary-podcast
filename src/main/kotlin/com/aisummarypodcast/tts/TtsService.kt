package com.aisummarypodcast.tts

import org.slf4j.LoggerFactory
import org.springframework.ai.audio.tts.TextToSpeechPrompt
import org.springframework.ai.openai.OpenAiAudioSpeechModel
import org.springframework.stereotype.Service

@Service
class TtsService(private val speechModel: OpenAiAudioSpeechModel) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun generateAudio(chunks: List<String>): List<ByteArray> {
        log.info("Generating TTS audio for {} chunks", chunks.size)

        return chunks.mapIndexed { index, chunk ->
            log.info("Generating TTS chunk {}/{} ({} chars)", index + 1, chunks.size, chunk.length)
            val response = speechModel.call(TextToSpeechPrompt(chunk))
            response.result.output
        }
    }
}
