package com.aisummarypodcast.tts

import com.aisummarypodcast.store.PodcastStyle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.Base64

@Component
class InworldTtsProvider(
    private val apiClient: InworldApiClient
) : TtsProvider {

    private val log = LoggerFactory.getLogger(javaClass)

    override val maxChunkSize: Int = 2000

    companion object {
        const val DEFAULT_MODEL = "inworld-tts-1.5-max"

        private val CORE_GUIDELINES = """
            |The TTS engine supports rich expressiveness markup:
            |- Non-verbal tags: [sigh], [laugh], [breathe], [cough], [clear_throat], [yawn] — use sparingly for natural effect
            |- Emphasis: use *word* (single asterisks) for stressed words
            |- Pacing: use ellipsis (...) for trailing pauses, exclamation marks for excitement
            |- IPA phonemes: use /phoneme/ for precise pronunciation of proper nouns""".trimMargin()

        private val CASUAL_ADDITION = """
            |- Use natural filler words (uh, um, well, you know) to sound conversational and human""".trimMargin()

        private val FORMAL_ADDITION = """
            |- Avoid filler words (uh, um, well, you know) and minimize non-verbal tags — keep delivery clean and professional""".trimMargin()
    }

    override fun scriptGuidelines(style: PodcastStyle): String {
        val styleSpecific = when (style) {
            PodcastStyle.CASUAL, PodcastStyle.DIALOGUE -> CASUAL_ADDITION
            PodcastStyle.EXECUTIVE_SUMMARY, PodcastStyle.NEWS_BRIEFING -> FORMAL_ADDITION
            else -> ""
        }
        return if (styleSpecific.isNotEmpty()) "$CORE_GUIDELINES\n$styleSpecific" else CORE_GUIDELINES
    }

    override fun generate(request: TtsRequest): TtsResult {
        val modelId = request.ttsSettings["model"] ?: DEFAULT_MODEL
        val speed = request.ttsSettings["speed"]?.toDoubleOrNull()
        val temperature = request.ttsSettings["temperature"]?.toDoubleOrNull()
        val style = inferStyle(request)

        return if (style == PodcastStyle.DIALOGUE || style == PodcastStyle.INTERVIEW) {
            generateDialogue(request, modelId, speed, temperature)
        } else {
            generateMonologue(request, modelId, speed, temperature)
        }
    }

    private fun generateMonologue(request: TtsRequest, modelId: String, speed: Double?, temperature: Double?): TtsResult {
        val voiceId = request.ttsVoices["default"]
            ?: throw IllegalStateException("Inworld TTS requires a 'default' voice in ttsVoices")

        val chunks = TextChunker.chunk(request.script, maxChunkSize)
        log.info("Generating Inworld TTS audio for {} chunks (voice: {}, model: {}, speed: {}, temperature: {})", chunks.size, voiceId, modelId, speed, temperature)

        var totalCharacters = 0
        val audioChunks = chunks.mapIndexed { index, chunk ->
            log.info("Generating Inworld TTS chunk {}/{} ({} chars)", index + 1, chunks.size, chunk.length)
            val response = apiClient.synthesizeSpeech(request.userId, voiceId, chunk, modelId, speed, temperature)
            totalCharacters += response.processedCharactersCount
            Base64.getDecoder().decode(response.audioContent)
        }

        return TtsResult(
            audioChunks = audioChunks,
            totalCharacters = totalCharacters,
            requiresConcatenation = chunks.size > 1,
            model = modelId
        )
    }

    private fun generateDialogue(request: TtsRequest, modelId: String, speed: Double?, temperature: Double?): TtsResult {
        val turns = DialogueScriptParser.parse(request.script)
        if (turns.isEmpty()) {
            throw IllegalStateException("Dialogue script produced no speaker turns")
        }

        log.info("Generating Inworld dialogue: {} turns, model: {}, speed: {}, temperature: {}", turns.size, modelId, speed, temperature)

        var totalCharacters = 0
        val audioChunks = turns.flatMapIndexed { index, turn ->
            val voiceId = request.ttsVoices[turn.role]
                ?: throw IllegalStateException(
                    "No voice configured for role '${turn.role}'. Available roles: ${request.ttsVoices.keys.joinToString()}"
                )

            val turnChunks = TextChunker.chunk(turn.text, maxChunkSize)
            log.info("Generating Inworld turn {}/{} (role: {}, {} chunks, {} chars)", index + 1, turns.size, turn.role, turnChunks.size, turn.text.length)

            turnChunks.map { chunk ->
                val response = apiClient.synthesizeSpeech(request.userId, voiceId, chunk, modelId, speed, temperature)
                totalCharacters += response.processedCharactersCount
                Base64.getDecoder().decode(response.audioContent)
            }
        }

        return TtsResult(
            audioChunks = audioChunks,
            totalCharacters = totalCharacters,
            requiresConcatenation = audioChunks.size > 1,
            model = modelId
        )
    }

    private fun inferStyle(request: TtsRequest): PodcastStyle? {
        // If ttsVoices has roles other than "default", it's a dialogue/interview style
        val roles = request.ttsVoices.keys
        return when {
            roles.contains("interviewer") && roles.contains("expert") -> PodcastStyle.INTERVIEW
            roles.size > 1 && !roles.all { it == "default" } -> PodcastStyle.DIALOGUE
            else -> null
        }
    }
}
