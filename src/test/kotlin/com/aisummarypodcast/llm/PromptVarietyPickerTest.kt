package com.aisummarypodcast.llm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PromptVarietyPickerTest {

    private val picker = PromptVarietyPicker()

    @Test
    fun `same (podcastId, episodeDate) yields identical selection across all six axes`() {
        val key1 = picker.pick("podcast-abc", LocalDate.of(2026, 5, 12))
        val key2 = picker.pick("podcast-abc", LocalDate.of(2026, 5, 12))
        assertEquals(key1, key2)
    }

    @Test
    fun `consecutive days produce at least 3 distinct opening-style and 3 distinct sign-off-shape selections over 5 days`() {
        val podcastId = "podcast-abc"
        val baseDate = LocalDate.of(2026, 5, 12)
        val selections = (0 until 5).map { picker.pick(podcastId, baseDate.plusDays(it.toLong())) }

        val distinctOpenings = selections.map { it.openingStyle }.toSet()
        val distinctSignOffs = selections.map { it.signOffShape }.toSet()

        assertTrue(distinctOpenings.size >= 3, "expected ≥3 distinct opening styles over 5 days, got $distinctOpenings")
        assertTrue(distinctSignOffs.size >= 3, "expected ≥3 distinct sign-off shapes over 5 days, got $distinctSignOffs")
    }

    @Test
    fun `two different podcasts on the same date differ on at least one axis`() {
        val date = LocalDate.of(2026, 5, 12)
        val a = picker.pick("podcast-a", date)
        val b = picker.pick("podcast-b", date)
        assertNotEquals(a, b)
    }
}
