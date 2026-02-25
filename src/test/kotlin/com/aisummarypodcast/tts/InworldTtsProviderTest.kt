package com.aisummarypodcast.tts

import com.aisummarypodcast.store.PodcastStyle
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Base64

class InworldTtsProviderTest {

    private val apiClient = mockk<InworldApiClient>()
    private val provider = InworldTtsProvider(apiClient)

    private val sampleAudio = Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3))

    @Test
    fun `maxChunkSize is 2000`() {
        assertEquals(2000, provider.maxChunkSize)
    }

    @Test
    fun `generates single-speaker audio with default voice`() {
        val request = TtsRequest(
            script = "Hello world",
            ttsVoices = mapOf("default" to "voice-1"),
            ttsSettings = emptyMap(),
            language = "en",
            userId = "u1"
        )
        every {
            apiClient.synthesizeSpeech("u1", "voice-1", "Hello world", "inworld-tts-1.5-max", null, null)
        } returns InworldSpeechResponse(sampleAudio, 11)

        val result = provider.generate(request)

        assertEquals(1, result.audioChunks.size)
        assertEquals(11, result.totalCharacters)
        assertFalse(result.requiresConcatenation)
        assertEquals("inworld-tts-1.5-max", result.model)
    }

    @Test
    fun `uses model from ttsSettings when specified`() {
        val request = TtsRequest(
            script = "Test",
            ttsVoices = mapOf("default" to "voice-1"),
            ttsSettings = mapOf("model" to "inworld-tts-1.5-mini"),
            language = "en",
            userId = "u1"
        )
        every {
            apiClient.synthesizeSpeech("u1", "voice-1", "Test", "inworld-tts-1.5-mini", null, null)
        } returns InworldSpeechResponse(sampleAudio, 4)

        val result = provider.generate(request)

        assertEquals("inworld-tts-1.5-mini", result.model)
    }

    @Test
    fun `throws when default voice is missing for monologue`() {
        val request = TtsRequest(
            script = "Test",
            ttsVoices = mapOf("host" to "v1", "cohost" to "v2"),
            ttsSettings = emptyMap(),
            language = "en",
            userId = "u1"
        )

        // This will be treated as dialogue because of multiple non-default roles
        // We need to set up the mocks for dialogue parsing
        // Actually with host/cohost it'll go dialogue path and parse tags
        // Let's test the monologue missing voice case
        val monoRequest = request.copy(ttsVoices = emptyMap())
        assertThrows<IllegalStateException> { provider.generate(monoRequest) }
    }

    @Test
    fun `generates dialogue with per-turn voice`() {
        val request = TtsRequest(
            script = "<host>Hello there!</host><cohost>Hey, how are you?</cohost>",
            ttsVoices = mapOf("host" to "voice-1", "cohost" to "voice-2"),
            ttsSettings = emptyMap(),
            language = "en",
            userId = "u1"
        )
        every {
            apiClient.synthesizeSpeech("u1", "voice-1", "Hello there!", "inworld-tts-1.5-max", null, null)
        } returns InworldSpeechResponse(sampleAudio, 12)
        every {
            apiClient.synthesizeSpeech("u1", "voice-2", "Hey, how are you?", "inworld-tts-1.5-max", null, null)
        } returns InworldSpeechResponse(sampleAudio, 17)

        val result = provider.generate(request)

        assertEquals(2, result.audioChunks.size)
        assertEquals(29, result.totalCharacters)
        assertTrue(result.requiresConcatenation)
    }

    @Test
    fun `throws when dialogue role has no configured voice`() {
        val request = TtsRequest(
            script = "<host>Hello!</host><guest>Hi!</guest>",
            ttsVoices = mapOf("host" to "voice-1"),
            ttsSettings = emptyMap(),
            language = "en",
            userId = "u1"
        )

        assertThrows<IllegalStateException> { provider.generate(request) }
    }

    @Test
    fun `generates interview style with interviewer and expert roles`() {
        val request = TtsRequest(
            script = "<interviewer>What happened?</interviewer><expert>A lot of things.</expert>",
            ttsVoices = mapOf("interviewer" to "voice-1", "expert" to "voice-2"),
            ttsSettings = emptyMap(),
            language = "en",
            userId = "u1"
        )
        every {
            apiClient.synthesizeSpeech("u1", "voice-1", "What happened?", "inworld-tts-1.5-max", null, null)
        } returns InworldSpeechResponse(sampleAudio, 14)
        every {
            apiClient.synthesizeSpeech("u1", "voice-2", "A lot of things.", "inworld-tts-1.5-max", null, null)
        } returns InworldSpeechResponse(sampleAudio, 16)

        val result = provider.generate(request)

        assertEquals(2, result.audioChunks.size)
        assertEquals(30, result.totalCharacters)
    }

    @Test
    fun `passes speed and temperature from ttsSettings`() {
        val request = TtsRequest(
            script = "Hello world",
            ttsVoices = mapOf("default" to "voice-1"),
            ttsSettings = mapOf("speed" to "1.2", "temperature" to "0.8"),
            language = "en",
            userId = "u1"
        )
        every {
            apiClient.synthesizeSpeech("u1", "voice-1", "Hello world", "inworld-tts-1.5-max", 1.2, 0.8)
        } returns InworldSpeechResponse(sampleAudio, 11)

        val result = provider.generate(request)

        assertEquals(1, result.audioChunks.size)
        assertEquals(11, result.totalCharacters)
    }

    // --- Script guidelines tests ---

    @Test
    fun `casual style guidelines include filler words`() {
        val guidelines = provider.scriptGuidelines(PodcastStyle.CASUAL)
        assertTrue(guidelines.contains("filler words"))
        assertTrue(guidelines.contains("[sigh]"))
        assertTrue(guidelines.contains("*word*"))
    }

    @Test
    fun `dialogue style guidelines include filler words`() {
        val guidelines = provider.scriptGuidelines(PodcastStyle.DIALOGUE)
        assertTrue(guidelines.contains("filler words"))
    }

    @Test
    fun `executive summary guidelines suppress filler words`() {
        val guidelines = provider.scriptGuidelines(PodcastStyle.EXECUTIVE_SUMMARY)
        assertTrue(guidelines.contains("Avoid filler words"))
        assertTrue(guidelines.contains("minimize non-verbal tags"))
    }

    @Test
    fun `news briefing guidelines suppress filler words`() {
        val guidelines = provider.scriptGuidelines(PodcastStyle.NEWS_BRIEFING)
        assertTrue(guidelines.contains("Avoid filler words"))
    }

    @Test
    fun `all styles include core markup instructions`() {
        for (style in PodcastStyle.entries) {
            val guidelines = provider.scriptGuidelines(style)
            assertTrue(guidelines.contains("[sigh]"), "Missing non-verbal tags for $style")
            assertTrue(guidelines.contains("*word*"), "Missing emphasis for $style")
            assertTrue(guidelines.contains("..."), "Missing pacing for $style")
            assertTrue(guidelines.contains("/phoneme/"), "Missing IPA for $style")
        }
    }
}
