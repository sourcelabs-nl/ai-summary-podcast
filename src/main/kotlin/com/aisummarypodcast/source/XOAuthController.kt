package com.aisummarypodcast.source

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.publishing.AuthorizeResponse
import com.aisummarypodcast.publishing.OAuthConnectionService
import com.aisummarypodcast.user.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@RestController
class XOAuthController(
    private val appProperties: AppProperties,
    private val userService: UserService,
    private val xClient: XClient,
    private val oauthConnectionService: OAuthConnectionService
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val pendingVerifiers = ConcurrentHashMap<String, PendingOAuth>()

    private data class PendingOAuth(
        val codeVerifier: String,
        val createdAt: Instant = Instant.now()
    )

    private fun isConfigured(): Boolean =
        !appProperties.x.clientId.isNullOrBlank() &&
            !appProperties.x.clientSecret.isNullOrBlank()

    private fun redirectUri(): String =
        "${appProperties.feed.baseUrl}/oauth/x/callback"

    @GetMapping("/users/{userId}/oauth/x/authorize")
    fun authorize(@PathVariable userId: String): ResponseEntity<Any> {
        if (!isConfigured()) {
            return ResponseEntity.status(503).body(mapOf("error" to "X integration is not configured"))
        }
        userService.findById(userId) ?: return ResponseEntity.notFound().build()

        cleanExpiredVerifiers()

        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)
        val state = createSignedState(userId)

        pendingVerifiers[state] = PendingOAuth(codeVerifier)

        val params = mapOf(
            "client_id" to appProperties.x.clientId!!,
            "redirect_uri" to redirectUri(),
            "response_type" to "code",
            "code_challenge" to codeChallenge,
            "code_challenge_method" to "S256",
            "state" to state,
            "scope" to "tweet.read users.read offline.access"
        )
        val queryString = params.entries.joinToString("&") { (k, v) ->
            "$k=${URLEncoder.encode(v, Charsets.UTF_8)}"
        }
        val authorizationUrl = "https://twitter.com/i/oauth2/authorize?$queryString"

        return ResponseEntity.ok(AuthorizeResponse(authorizationUrl))
    }

    @GetMapping("/oauth/x/callback")
    fun callback(
        @RequestParam code: String,
        @RequestParam state: String,
        @RequestParam(required = false) error: String?
    ): ResponseEntity<String> {
        if (error != null) {
            return htmlResponse("X authorization failed: $error")
        }

        val userId = verifySignedState(state)
        if (userId == null) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.TEXT_HTML)
                .body(htmlPage("Authorization Error", "Invalid or tampered authorization request."))
        }

        val pending = pendingVerifiers.remove(state)
        if (pending == null) {
            return ResponseEntity.status(404)
                .contentType(MediaType.TEXT_HTML)
                .body(htmlPage("Authorization Expired", "The authorization request has expired. Please try again."))
        }

        return try {
            val tokenResponse = xClient.exchangeCodeForTokens(
                code = code,
                clientId = appProperties.x.clientId!!,
                clientSecret = appProperties.x.clientSecret!!,
                redirectUri = redirectUri(),
                codeVerifier = pending.codeVerifier
            )

            val expiresAt = tokenResponse.expiresIn?.let { Instant.now().plusSeconds(it) }

            oauthConnectionService.storeConnection(
                userId = userId,
                provider = "x",
                accessToken = tokenResponse.accessToken,
                refreshToken = tokenResponse.refreshToken,
                expiresAt = expiresAt,
                scopes = tokenResponse.scope
            )

            log.info("X account connected for user {}", userId)
            htmlResponse("X account connected successfully! You can close this tab.")
        } catch (e: Exception) {
            log.error("Failed to exchange X authorization code for user {}: {}", userId, e.message, e)
            htmlResponse("Failed to connect X account. Please try again.")
        }
    }

    @GetMapping("/users/{userId}/oauth/x/status")
    fun status(@PathVariable userId: String): ResponseEntity<Any> {
        if (!isConfigured()) {
            return ResponseEntity.status(503).body(mapOf("error" to "X integration is not configured"))
        }
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(oauthConnectionService.getStatus(userId, "x"))
    }

    @DeleteMapping("/users/{userId}/oauth/x")
    fun disconnect(@PathVariable userId: String): ResponseEntity<Any> {
        if (!isConfigured()) {
            return ResponseEntity.status(503).body(mapOf("error" to "X integration is not configured"))
        }
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        return if (oauthConnectionService.deleteConnection(userId, "x")) {
            log.info("X account disconnected for user {}", userId)
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private fun createSignedState(userId: String): String {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(userId.toByteArray())
        val signature = hmacSign(payload)
        return "$payload.$signature"
    }

    private fun verifySignedState(state: String): String? {
        val parts = state.split(".", limit = 2)
        if (parts.size != 2) return null
        val (payload, signature) = parts
        val expectedSignature = hmacSign(payload)
        if (!MessageDigest.isEqual(signature.toByteArray(), expectedSignature.toByteArray())) return null
        return String(Base64.getUrlDecoder().decode(payload))
    }

    private fun hmacSign(data: String): String {
        val keyBytes = Base64.getDecoder().decode(appProperties.encryption.masterKey)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(keyBytes, "HmacSHA256"))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.toByteArray()))
    }

    private fun cleanExpiredVerifiers() {
        val cutoff = Instant.now().minusSeconds(600) // 10 minute TTL
        pendingVerifiers.entries.removeIf { it.value.createdAt.isBefore(cutoff) }
    }

    private fun htmlResponse(message: String): ResponseEntity<String> =
        ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(htmlPage("X", message))

    private fun htmlPage(title: String, message: String): String = """
        <!DOCTYPE html>
        <html><head><title>$title</title></head>
        <body style="font-family:system-ui;display:flex;justify-content:center;align-items:center;height:100vh;margin:0">
        <p style="font-size:1.2em">$message</p>
        </body></html>
    """.trimIndent()
}
