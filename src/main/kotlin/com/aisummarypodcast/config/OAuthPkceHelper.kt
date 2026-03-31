package com.aisummarypodcast.config

import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class OAuthPkceHelper(
    private val appProperties: AppProperties
) {

    fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    fun createSignedState(userId: String): String {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(userId.toByteArray())
        val signature = hmacSign(payload)
        return "$payload.$signature"
    }

    fun verifySignedState(state: String): String? {
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
}
