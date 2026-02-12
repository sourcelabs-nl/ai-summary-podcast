package com.aisummarypodcast.user

import com.aisummarypodcast.store.ApiKeyCategory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class SetProviderConfigRequest(val provider: String, val apiKey: String? = null, val baseUrl: String? = null)
data class ProviderConfigResponse(val category: String, val provider: String, val baseUrl: String)
data class ErrorResponse(val error: String)

@RestController
@RequestMapping("/users/{userId}/api-keys")
class UserProviderConfigController(
    private val providerConfigService: UserProviderConfigService,
    private val userService: UserService
) {

    @GetMapping
    fun list(@PathVariable userId: String): ResponseEntity<List<ProviderConfigResponse>> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val configs = providerConfigService.listConfigs(userId)
        return ResponseEntity.ok(configs.map { config ->
            val resolvedBaseUrl = config.baseUrl ?: providerConfigService.resolveDefaultUrl(config.provider)
            ProviderConfigResponse(category = config.category.name, provider = config.provider, baseUrl = resolvedBaseUrl)
        })
    }

    @PutMapping("/{category}")
    fun set(
        @PathVariable userId: String,
        @PathVariable category: String,
        @RequestBody request: SetProviderConfigRequest
    ): ResponseEntity<*> {
        val parsedCategory = parseCategory(category) ?: return invalidCategoryResponse()
        userService.findById(userId) ?: return ResponseEntity.notFound().build<Void>()
        if (request.provider.isBlank()) return ResponseEntity.badRequest()
            .body(ErrorResponse("Provider is required"))
        if (request.baseUrl == null && !providerConfigService.hasDefaultUrl(request.provider)) {
            return ResponseEntity.badRequest()
                .body(ErrorResponse("Base URL is required for provider '${request.provider}'. Known providers with defaults: ${UserProviderConfigService.PROVIDER_DEFAULT_URLS.keys.joinToString(", ")}"))
        }
        providerConfigService.setConfig(userId, parsedCategory, request.provider, request.apiKey, request.baseUrl)
        return ResponseEntity.ok().build<Void>()
    }

    @DeleteMapping("/{category}")
    fun delete(@PathVariable userId: String, @PathVariable category: String): ResponseEntity<*> {
        val parsedCategory = parseCategory(category) ?: return invalidCategoryResponse()
        userService.findById(userId) ?: return ResponseEntity.notFound().build<Void>()
        if (!providerConfigService.deleteConfig(userId, parsedCategory)) return ResponseEntity.notFound().build<Void>()
        return ResponseEntity.noContent().build<Void>()
    }

    private fun invalidCategoryResponse(): ResponseEntity<ErrorResponse> {
        val valid = ApiKeyCategory.entries.joinToString(", ") { it.name }
        return ResponseEntity.badRequest().body(ErrorResponse("Invalid category. Must be one of: $valid"))
    }

    private fun parseCategory(value: String): ApiKeyCategory? =
        try { ApiKeyCategory.valueOf(value.uppercase()) } catch (_: IllegalArgumentException) { null }
}
