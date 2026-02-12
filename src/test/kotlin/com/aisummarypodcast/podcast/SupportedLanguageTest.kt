package com.aisummarypodcast.podcast

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Locale

class SupportedLanguageTest {

    @Test
    fun `fromCode returns correct language for valid code`() {
        val result = SupportedLanguage.fromCode("nl")
        assertNotNull(result)
        assertEquals(SupportedLanguage.DUTCH, result)
        assertEquals("Dutch", result!!.displayName)
    }

    @Test
    fun `fromCode returns null for invalid code`() {
        assertNull(SupportedLanguage.fromCode("xx"))
    }

    @Test
    fun `isSupported returns true for valid code`() {
        assertTrue(SupportedLanguage.isSupported("en"))
        assertTrue(SupportedLanguage.isSupported("fr"))
        assertTrue(SupportedLanguage.isSupported("ja"))
    }

    @Test
    fun `isSupported returns false for invalid code`() {
        assertFalse(SupportedLanguage.isSupported("xx"))
        assertFalse(SupportedLanguage.isSupported(""))
        assertFalse(SupportedLanguage.isSupported("english"))
    }

    @Test
    fun `toLocale returns correct locale for common languages`() {
        assertEquals(Locale.of("nl"), SupportedLanguage.DUTCH.toLocale())
        assertEquals(Locale.of("fr"), SupportedLanguage.FRENCH.toLocale())
        assertEquals(Locale.of("de"), SupportedLanguage.GERMAN.toLocale())
        assertEquals(Locale.of("en"), SupportedLanguage.ENGLISH.toLocale())
    }

    @Test
    fun `toLocale resolves all languages to a valid locale`() {
        // Every supported language should resolve to either its own locale or English fallback
        for (lang in SupportedLanguage.entries) {
            val locale = lang.toLocale()
            assertTrue(
                locale == Locale.of(lang.code) || locale == Locale.ENGLISH,
                "Expected ${lang.code} to resolve to its own locale or English, got: $locale"
            )
        }
    }

    @Test
    fun `all 57 languages are defined`() {
        assertEquals(57, SupportedLanguage.entries.size)
    }

    @Test
    fun `all codes are unique`() {
        val codes = SupportedLanguage.entries.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
    }
}
