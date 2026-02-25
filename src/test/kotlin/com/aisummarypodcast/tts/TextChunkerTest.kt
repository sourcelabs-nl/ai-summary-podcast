package com.aisummarypodcast.tts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TextChunkerTest {

    @Test
    fun `short text returns single chunk with default max`() {
        val text = "Hello world."
        val chunks = TextChunker.chunk(text)
        assertEquals(1, chunks.size)
        assertEquals("Hello world.", chunks[0])
    }

    @Test
    fun `short text returns single chunk with custom max`() {
        val text = "Hello world."
        val chunks = TextChunker.chunk(text, 2000)
        assertEquals(1, chunks.size)
        assertEquals("Hello world.", chunks[0])
    }

    @Test
    fun `splits at sentence boundaries with default max`() {
        val sentence = "This is a sentence. "
        // Create text that exceeds 4096
        val text = sentence.repeat(250) // ~5000 chars
        val chunks = TextChunker.chunk(text)
        assertTrue(chunks.size > 1)
        chunks.forEach { assertTrue(it.length <= 4096) }
    }

    @Test
    fun `splits at sentence boundaries with custom max of 2000`() {
        val sentence = "This is a test sentence. "
        val text = sentence.repeat(150) // ~3750 chars
        val chunks = TextChunker.chunk(text, 2000)
        assertTrue(chunks.size > 1)
        chunks.forEach { assertTrue(it.length <= 2000, "Chunk exceeds max: ${it.length}") }
    }

    @Test
    fun `text exactly at max returns single chunk`() {
        val text = "a".repeat(2000)
        val chunks = TextChunker.chunk(text, 2000)
        assertEquals(1, chunks.size)
    }

    @Test
    fun `text one char over max splits`() {
        // Build text of sentences that total just over 100
        val text = "Short. " .repeat(20) // ~140 chars
        val chunks = TextChunker.chunk(text, 100)
        assertTrue(chunks.size > 1)
        chunks.forEach { assertTrue(it.length <= 100, "Chunk exceeds max: ${it.length}") }
    }

    @Test
    fun `long sentence exceeding max is split at whitespace`() {
        val text = "word ".repeat(500) // ~2500 chars, no sentence boundary
        val chunks = TextChunker.chunk(text, 2000)
        assertTrue(chunks.size > 1)
        chunks.forEach { assertTrue(it.length <= 2000, "Chunk exceeds max: ${it.length}") }
    }

    @Test
    fun `default max chunk size is 4096`() {
        // Text under 4096 → single chunk
        val shortText = "a".repeat(4096)
        assertEquals(1, TextChunker.chunk(shortText).size)

        // Text over 4096 → multiple chunks
        val longText = "This is a sentence. ".repeat(250) // ~5000 chars
        assertTrue(TextChunker.chunk(longText).size > 1)
    }

    @Test
    fun `preserves sentence boundaries when splitting`() {
        val text = "First sentence. Second sentence. Third sentence. Fourth sentence."
        val chunks = TextChunker.chunk(text, 40)
        // Each chunk should end at a sentence boundary (with period)
        chunks.dropLast(1).forEach { chunk ->
            assertTrue(chunk.endsWith("."), "Chunk should end at sentence boundary: '$chunk'")
        }
    }
}
