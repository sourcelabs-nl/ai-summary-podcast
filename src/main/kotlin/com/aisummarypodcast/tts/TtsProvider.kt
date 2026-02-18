package com.aisummarypodcast.tts

data class TtsRequest(
    val script: String,
    val ttsVoices: Map<String, String>,
    val ttsSettings: Map<String, String>,
    val language: String,
    val userId: String
)

data class TtsResult(
    val audioChunks: List<ByteArray>,
    val totalCharacters: Int,
    val requiresConcatenation: Boolean
)

interface TtsProvider {
    fun generate(request: TtsRequest): TtsResult
}
