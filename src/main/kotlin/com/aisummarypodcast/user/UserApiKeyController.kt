package com.aisummarypodcast.user

import com.aisummarypodcast.store.ApiKeyCategory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class SetApiKeyRequest(val apiKey: String, val provider: String)
data class ApiKeyResponse(val category: String, val provider: String)
data class ErrorResponse(val error: String)

@RestController
@RequestMapping("/users/{userId}/api-keys")
class UserApiKeyController(
    private val userApiKeyService: UserApiKeyService,
    private val userService: UserService
) {

    @GetMapping
    fun list(@PathVariable userId: String): ResponseEntity<List<ApiKeyResponse>> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val keys = userApiKeyService.listKeys(userId)
        return ResponseEntity.ok(keys.map { ApiKeyResponse(category = it.category.name, provider = it.provider) })
    }

    @PutMapping("/{category}")
    fun set(
        @PathVariable userId: String,
        @PathVariable category: String,
        @RequestBody request: SetApiKeyRequest
    ): ResponseEntity<*> {
        val parsedCategory = parseCategory(category) ?: return invalidCategoryResponse()
        userService.findById(userId) ?: return ResponseEntity.notFound().build<Void>()
        if (request.apiKey.isBlank()) return ResponseEntity.badRequest().build<Void>()
        if (request.provider.isBlank()) return ResponseEntity.badRequest().build<Void>()
        userApiKeyService.setKey(userId, parsedCategory, request.provider, request.apiKey)
        return ResponseEntity.ok().build<Void>()
    }

    @DeleteMapping("/{category}")
    fun delete(@PathVariable userId: String, @PathVariable category: String): ResponseEntity<*> {
        val parsedCategory = parseCategory(category) ?: return invalidCategoryResponse()
        userService.findById(userId) ?: return ResponseEntity.notFound().build<Void>()
        if (!userApiKeyService.deleteKey(userId, parsedCategory)) return ResponseEntity.notFound().build<Void>()
        return ResponseEntity.noContent().build<Void>()
    }

    private fun invalidCategoryResponse(): ResponseEntity<ErrorResponse> {
        val valid = ApiKeyCategory.entries.joinToString(", ") { it.name }
        return ResponseEntity.badRequest().body(ErrorResponse("Invalid category. Must be one of: $valid"))
    }

    private fun parseCategory(value: String): ApiKeyCategory? =
        try { ApiKeyCategory.valueOf(value.uppercase()) } catch (_: IllegalArgumentException) { null }
}
