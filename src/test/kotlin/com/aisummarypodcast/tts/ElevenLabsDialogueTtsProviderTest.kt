package com.aisummarypodcast.tts

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ElevenLabsDialogueTtsProviderTest {

    private val apiClient = mockk<ElevenLabsApiClient>()
    private val provider = ElevenLabsDialogueTtsProvider(apiClient)

    @Test
    fun `generates dialogue audio from tagged script`() {
        val request = TtsRequest(
            script = "<host>Hello!</host><cohost>Hi there!</cohost>",
            ttsVoices = mapOf("host" to "v1", "cohost" to "v2"),
            ttsSettings = emptyMap(),
            language = "en",
            userId = "u1"
        )

        every { apiClient.textToDialogue("u1", any(), null) } returns byteArrayOf(1, 2, 3)

        val result = provider.generate(request)

        assertEquals(1, result.audioChunks.size)
        assertFalse(result.requiresConcatenation)
        assertEquals(15, result.totalCharacters)

        verify {
            apiClient.textToDialogue("u1", match { inputs ->
                inputs.size == 2 &&
                inputs[0] == DialogueInput("Hello!", "v1") &&
                inputs[1] == DialogueInput("Hi there!", "v2")
            }, null)
        }
    }

    @Test
    fun `throws when voice not configured for role`() {
        val request = TtsRequest(
            script = "<host>Hello!</host><narrator>Narration</narrator>",
            ttsVoices = mapOf("host" to "v1"),
            ttsSettings = emptyMap(),
            language = "en",
            userId = "u1"
        )

        assertThrows<IllegalStateException> { provider.generate(request) }
    }

    @Test
    fun `throws for empty dialogue`() {
        val request = TtsRequest(
            script = "",
            ttsVoices = mapOf("host" to "v1"),
            ttsSettings = emptyMap(),
            language = "en",
            userId = "u1"
        )

        assertThrows<IllegalStateException> { provider.generate(request) }
    }

    @Test
    fun `passes settings to API`() {
        val request = TtsRequest(
            script = "<host>Hello!</host>",
            ttsVoices = mapOf("host" to "v1"),
            ttsSettings = mapOf("stability" to "0.7"),
            language = "en",
            userId = "u1"
        )

        every { apiClient.textToDialogue("u1", any(), mapOf("stability" to "0.7")) } returns byteArrayOf(1)

        provider.generate(request)

        verify { apiClient.textToDialogue("u1", any(), mapOf("stability" to "0.7")) }
    }
}
