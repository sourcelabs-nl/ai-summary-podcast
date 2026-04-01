package com.aisummarypodcast.user

data class CreateUserRequest(val name: String)
data class UpdateUserRequest(val name: String)
data class UserResponse(val id: String, val name: String)

data class SetProviderConfigRequest(val provider: String, val apiKey: String? = null, val baseUrl: String? = null)
data class ProviderConfigResponse(val category: String, val provider: String, val baseUrl: String)
data class ErrorResponse(val error: String)
