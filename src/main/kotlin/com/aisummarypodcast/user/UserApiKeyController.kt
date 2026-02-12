package com.aisummarypodcast.user

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class SetApiKeyRequest(val apiKey: String)
data class ApiKeyProviderResponse(val provider: String)

@RestController
@RequestMapping("/users/{userId}/api-keys")
class UserApiKeyController(
    private val userApiKeyService: UserApiKeyService,
    private val userService: UserService
) {

    @GetMapping
    fun list(@PathVariable userId: String): ResponseEntity<List<ApiKeyProviderResponse>> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val providers = userApiKeyService.listProviders(userId)
        return ResponseEntity.ok(providers.map { ApiKeyProviderResponse(it) })
    }

    @PutMapping("/{provider}")
    fun set(
        @PathVariable userId: String,
        @PathVariable provider: String,
        @RequestBody request: SetApiKeyRequest
    ): ResponseEntity<Void> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        if (request.apiKey.isBlank()) return ResponseEntity.badRequest().build()
        userApiKeyService.setKey(userId, provider, request.apiKey)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{provider}")
    fun delete(@PathVariable userId: String, @PathVariable provider: String): ResponseEntity<Void> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        if (!userApiKeyService.deleteKey(userId, provider)) return ResponseEntity.notFound().build()
        return ResponseEntity.noContent().build()
    }
}
