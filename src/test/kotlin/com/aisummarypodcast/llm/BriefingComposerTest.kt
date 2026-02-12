package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BriefingComposerTest {

    private val appProperties = mockk<AppProperties>()
    private val chatClientFactory = mockk<ChatClientFactory>()
    private val composer = BriefingComposer(appProperties, chatClientFactory)

    @Test
    fun `stripSectionHeaders removes Opening header line`() {
        val input = "[Opening]\nWelcome to today's briefing.\n"
        val result = composer.stripSectionHeaders(input)
        assertEquals("Welcome to today's briefing.\n", result)
    }

    @Test
    fun `stripSectionHeaders removes multiple header lines`() {
        val input = "[Opening]\nWelcome.\n[Transition]\nNext topic.\n[Closing]\nThat's all.\n"
        val result = composer.stripSectionHeaders(input)
        assertEquals("Welcome.\nNext topic.\nThat's all.\n", result)
    }

    @Test
    fun `stripSectionHeaders preserves inline bracketed text`() {
        val input = "The company [ACME Corp] announced earnings.\n"
        val result = composer.stripSectionHeaders(input)
        assertEquals("The company [ACME Corp] announced earnings.\n", result)
    }

    @Test
    fun `stripSectionHeaders preserves brackets within sentences`() {
        val input = "Results were mixed [see chart] for the quarter.\nOverall performance improved.\n"
        val result = composer.stripSectionHeaders(input)
        assertEquals("Results were mixed [see chart] for the quarter.\nOverall performance improved.\n", result)
    }

    @Test
    fun `stripSectionHeaders handles script with no headers`() {
        val input = "Welcome to today's briefing.\nHere are the top stories.\n"
        val result = composer.stripSectionHeaders(input)
        assertEquals(input, result)
    }
}
