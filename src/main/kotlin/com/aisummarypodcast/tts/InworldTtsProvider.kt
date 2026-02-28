package com.aisummarypodcast.tts

import com.aisummarypodcast.store.PodcastStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger

@Component
class InworldTtsProvider(
    private val apiClient: InworldApiClient
) : TtsProvider {

    private val log = LoggerFactory.getLogger(javaClass)

    override val maxChunkSize: Int = 2000

    companion object {
        const val DEFAULT_MODEL = "inworld-tts-1.5-max"
        private const val DEFAULT_TEMPERATURE = 0.8
        private const val MAX_RETRY_ATTEMPTS = 3
        private val RETRY_DELAYS_MS = longArrayOf(1000, 2000, 4000)

        private val CORE_GUIDELINES = """
            |The TTS engine supports rich expressiveness markup:
            |- Non-verbal tags: [sigh], [laugh], [breathe], [cough], [clear_throat], [yawn] — use sparingly for natural effect
            |- Emphasis: use *word* (single asterisks) for stressed words. NEVER use **double asterisks** — the TTS engine will read the asterisk characters aloud
            |- Pacing: use ellipsis (...) for trailing pauses, exclamation marks for excitement
            |- IPA phonemes: use /phoneme/ for precise pronunciation of proper nouns
            |Text formatting rules:
            |- Write all numbers, dates, currencies, and symbols in fully spoken form (e.g. "twenty twenty-six" not "2026", "five thousand dollars" not "${'$'}5,000", "ten percent" not "10%")
            |- NEVER use markdown formatting (headers, bold, bullet points, links) — write everything as natural spoken sentences
            |- Use natural contractions throughout (don't, we're, it's, they've) for spoken naturalness
            |- Always end sentences with proper punctuation (period, question mark, or exclamation mark) — the TTS engine uses these for pacing""".trimMargin()

        private val CASUAL_ADDITION = """
            |- Use natural filler words (uh, um, well, you know) to sound conversational and human""".trimMargin()

        private val FORMAL_ADDITION = """
            |- Avoid filler words (uh, um, well, you know) and minimize non-verbal tags — keep delivery clean and professional""".trimMargin()
    }

    override fun scriptGuidelines(style: PodcastStyle, pronunciations: Map<String, String>): String {
        val styleSpecific = when (style) {
            PodcastStyle.CASUAL, PodcastStyle.DIALOGUE -> CASUAL_ADDITION
            PodcastStyle.EXECUTIVE_SUMMARY, PodcastStyle.NEWS_BRIEFING -> FORMAL_ADDITION
            else -> ""
        }
        val base = if (styleSpecific.isNotEmpty()) "$CORE_GUIDELINES\n$styleSpecific" else CORE_GUIDELINES
        if (pronunciations.isEmpty()) return base
        val pronunciationGuide = buildString {
            appendLine()
            appendLine("Pronunciation Guide (use IPA notation on first occurrence of each term):")
            for ((term, ipa) in pronunciations) {
                appendLine("- $term: $ipa")
            }
        }.trimEnd()
        return "$base\n$pronunciationGuide"
    }

    override fun generate(request: TtsRequest): TtsResult {
        val modelId = request.ttsSettings["model"] ?: DEFAULT_MODEL
        val speed = request.ttsSettings["speed"]?.toDoubleOrNull()
        val temperature = request.ttsSettings["temperature"]?.toDoubleOrNull() ?: DEFAULT_TEMPERATURE
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

        val processedScript = InworldScriptPostProcessor.process(request.script)
        val chunks = TextChunker.chunk(processedScript, maxChunkSize)
        log.info("Generating Inworld TTS audio for {} chunks in parallel (voice: {}, model: {}, speed: {}, temperature: {})", chunks.size, voiceId, modelId, speed, temperature)

        val totalCharacters = AtomicInteger(0)
        val audioChunks = runBlocking(Dispatchers.IO) {
            chunks.mapIndexed { index, chunk ->
                async {
                    log.info("Generating Inworld TTS chunk {}/{} ({} chars)", index + 1, chunks.size, chunk.length)
                    val response = synthesizeWithRetry(request.userId, voiceId, chunk, modelId, speed, temperature)
                    totalCharacters.addAndGet(response.processedCharactersCount)
                    Base64.getDecoder().decode(response.audioContent)
                }
            }.awaitAll()
        }

        return TtsResult(
            audioChunks = audioChunks,
            totalCharacters = totalCharacters.get(),
            requiresConcatenation = chunks.size > 1,
            model = modelId
        )
    }

    private fun generateDialogue(request: TtsRequest, modelId: String, speed: Double?, temperature: Double?): TtsResult {
        val turns = DialogueScriptParser.parse(request.script)
        if (turns.isEmpty()) {
            throw IllegalStateException("Dialogue script produced no speaker turns")
        }

        // Flatten all turn chunks into a single indexed list for full parallel generation
        data class ChunkWork(val voiceId: String, val text: String)

        val allChunks = turns.flatMapIndexed { index, turn ->
            val voiceId = request.ttsVoices[turn.role]
                ?: throw IllegalStateException(
                    "No voice configured for role '${turn.role}'. Available roles: ${request.ttsVoices.keys.joinToString()}"
                )
            val processedText = InworldScriptPostProcessor.process(turn.text)
            val turnChunks = TextChunker.chunk(processedText, maxChunkSize)
            log.info("Inworld dialogue turn {}/{} (role: {}, {} chunks, {} chars)", index + 1, turns.size, turn.role, turnChunks.size, turn.text.length)
            turnChunks.map { chunk -> ChunkWork(voiceId, chunk) }
        }

        log.info("Generating Inworld dialogue: {} total chunks in parallel, model: {}", allChunks.size, modelId)

        val totalCharacters = AtomicInteger(0)
        val audioChunks = runBlocking(Dispatchers.IO) {
            allChunks.mapIndexed { index, work ->
                async {
                    log.info("Generating Inworld dialogue chunk {}/{} ({} chars)", index + 1, allChunks.size, work.text.length)
                    val response = synthesizeWithRetry(request.userId, work.voiceId, work.text, modelId, speed, temperature)
                    totalCharacters.addAndGet(response.processedCharactersCount)
                    Base64.getDecoder().decode(response.audioContent)
                }
            }.awaitAll()
        }

        return TtsResult(
            audioChunks = audioChunks,
            totalCharacters = totalCharacters.get(),
            requiresConcatenation = audioChunks.size > 1,
            model = modelId
        )
    }

    private suspend fun synthesizeWithRetry(
        userId: String, voiceId: String, text: String, modelId: String, speed: Double?, temperature: Double?
    ): InworldSpeechResponse {
        for (attempt in 0 until MAX_RETRY_ATTEMPTS) {
            try {
                return apiClient.synthesizeSpeech(userId, voiceId, text, modelId, speed, temperature)
            } catch (e: InworldRateLimitException) {
                if (attempt == MAX_RETRY_ATTEMPTS - 1) throw e
                val delayMs = RETRY_DELAYS_MS[attempt]
                log.warn("Inworld rate limited (attempt {}/{}), retrying in {}ms", attempt + 1, MAX_RETRY_ATTEMPTS, delayMs)
                delay(delayMs)
            }
        }
        throw IllegalStateException("Unreachable")
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
