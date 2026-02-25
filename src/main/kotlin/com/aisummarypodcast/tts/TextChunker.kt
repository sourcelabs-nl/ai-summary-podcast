package com.aisummarypodcast.tts

object TextChunker {

    private const val DEFAULT_MAX_CHUNK_SIZE = 4096
    private val SENTENCE_BOUNDARY = Regex("(?<=[.!?])\\s+")

    fun chunk(text: String, maxChunkSize: Int = DEFAULT_MAX_CHUNK_SIZE): List<String> {
        if (text.length <= maxChunkSize) return listOf(text)

        val sentences = text.split(SENTENCE_BOUNDARY)
        val chunks = mutableListOf<String>()
        val current = StringBuilder()

        for (sentence in sentences) {
            if (sentence.length > maxChunkSize) {
                // Flush current buffer
                if (current.isNotEmpty()) {
                    chunks.add(current.toString().trim())
                    current.clear()
                }
                // Split long sentence at whitespace boundaries
                chunks.addAll(splitLongSentence(sentence, maxChunkSize))
                continue
            }

            if (current.length + sentence.length + 1 > maxChunkSize) {
                chunks.add(current.toString().trim())
                current.clear()
            }

            if (current.isNotEmpty()) current.append(" ")
            current.append(sentence)
        }

        if (current.isNotEmpty()) {
            chunks.add(current.toString().trim())
        }

        return chunks
    }

    private fun splitLongSentence(sentence: String, maxChunkSize: Int): List<String> {
        val chunks = mutableListOf<String>()
        var remaining = sentence

        while (remaining.length > maxChunkSize) {
            val splitAt = remaining.lastIndexOf(' ', maxChunkSize)
            val breakPoint = if (splitAt > 0) splitAt else maxChunkSize
            chunks.add(remaining.substring(0, breakPoint).trim())
            remaining = remaining.substring(breakPoint).trim()
        }

        if (remaining.isNotEmpty()) {
            chunks.add(remaining)
        }

        return chunks
    }
}
