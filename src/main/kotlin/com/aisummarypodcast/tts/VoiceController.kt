package com.aisummarypodcast.tts

import com.aisummarypodcast.store.TtsProviderType
import com.aisummarypodcast.user.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class VoiceResponse(val voiceId: String, val name: String, val category: String, val previewUrl: String?)

@RestController
@RequestMapping("/users/{userId}/voices")
class VoiceController(
    private val userService: UserService,
    private val elevenLabsApiClient: ElevenLabsApiClient,
    private val inworldApiClient: InworldApiClient
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun listVoices(
        @PathVariable userId: String,
        @RequestParam provider: String
    ): ResponseEntity<Any> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()

        return when (provider) {
            TtsProviderType.ELEVENLABS.value -> fetchVoices("ElevenLabs") {
                elevenLabsApiClient.listVoices(userId)
            }
            TtsProviderType.INWORLD.value -> fetchVoices("Inworld") {
                inworldApiClient.listVoices(userId)
            }
            else -> ResponseEntity.badRequest().body(mapOf("error" to "Voice discovery is not supported for the '$provider' provider"))
        }
    }

    private fun fetchVoices(providerName: String, fetch: () -> List<VoiceInfo>): ResponseEntity<Any> {
        return try {
            val voices = fetch()
            val response = voices.map { VoiceResponse(it.voiceId, it.name, it.category, it.previewUrl) }
            ResponseEntity.ok(response)
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: Exception) {
            log.error("Failed to fetch voices from {}: {}", providerName, e.message, e)
            ResponseEntity.status(502).body(mapOf("error" to "Failed to fetch voices from $providerName: ${e.message}"))
        }
    }
}
