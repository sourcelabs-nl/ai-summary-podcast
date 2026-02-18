package com.aisummarypodcast.tts

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ElevenLabsDialogueTtsProvider(
    private val apiClient: ElevenLabsApiClient
) : TtsProvider {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun generate(request: TtsRequest): TtsResult {
        val turns = DialogueScriptParser.parse(request.script)
        if (turns.isEmpty()) {
            throw IllegalStateException("Dialogue script produced no speaker turns")
        }

        val inputs = turns.map { turn ->
            val voiceId = request.ttsVoices[turn.role]
                ?: throw IllegalStateException(
                    "No voice configured for role '${turn.role}'. Available roles: ${request.ttsVoices.keys.joinToString()}"
                )
            DialogueInput(text = turn.text, voice_id = voiceId)
        }

        val uniqueVoices = inputs.map { it.voice_id }.distinct()
        log.info("Generating ElevenLabs dialogue: {} turns, {} unique voices", inputs.size, uniqueVoices.size)

        val settings = request.ttsSettings.ifEmpty { null }
        val audio = apiClient.textToDialogue(request.userId, inputs, settings)
        val totalCharacters = turns.sumOf { it.text.length }

        return TtsResult(
            audioChunks = listOf(audio),
            totalCharacters = totalCharacters,
            requiresConcatenation = false
        )
    }
}
