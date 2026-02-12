package com.aisummarypodcast.tts

object TextChunker {

    private const val MAX_CHUNK_SIZE = 4096
    private val SENTENCE_BOUNDARY = Regex("(?<=[.!?])\\s+")

    fun chunk(text: String): List<String> {
        if (text.length <= MAX_CHUNK_SIZE) return listOf(text)

        val sentences = text.split(SENTENCE_BOUNDARY)
        val chunks = mutableListOf<String>()
        val current = StringBuilder()

        for (sentence in sentences) {
            if (sentence.length > MAX_CHUNK_SIZE) {
                // Flush current buffer
                if (current.isNotEmpty()) {
                    chunks.add(current.toString().trim())
                    current.clear()
                }
                // Split long sentence at whitespace boundaries
                chunks.addAll(splitLongSentence(sentence))
                continue
            }

            if (current.length + sentence.length + 1 > MAX_CHUNK_SIZE) {
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

    private fun splitLongSentence(sentence: String): List<String> {
        val chunks = mutableListOf<String>()
        var remaining = sentence

        while (remaining.length > MAX_CHUNK_SIZE) {
            val splitAt = remaining.lastIndexOf(' ', MAX_CHUNK_SIZE)
            val breakPoint = if (splitAt > 0) splitAt else MAX_CHUNK_SIZE
            chunks.add(remaining.substring(0, breakPoint).trim())
            remaining = remaining.substring(breakPoint).trim()
        }

        if (remaining.isNotEmpty()) {
            chunks.add(remaining)
        }

        return chunks
    }
}
