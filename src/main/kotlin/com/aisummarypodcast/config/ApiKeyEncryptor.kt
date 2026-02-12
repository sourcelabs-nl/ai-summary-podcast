package com.aisummarypodcast.config

import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class ApiKeyEncryptor(private val appProperties: AppProperties) {

    private val algorithm = "AES/GCM/NoPadding"
    private val gcmTagLength = 128
    private val ivLength = 12

    private fun secretKey(): SecretKeySpec {
        val keyBytes = Base64.getDecoder().decode(appProperties.encryption.masterKey)
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(plainText: String): String {
        val iv = ByteArray(ivLength)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey(), GCMParameterSpec(gcmTagLength, iv))
        val cipherText = cipher.doFinal(plainText.toByteArray())

        val combined = iv + cipherText
        return Base64.getEncoder().encodeToString(combined)
    }

    fun decrypt(encrypted: String): String {
        val combined = Base64.getDecoder().decode(encrypted)
        val iv = combined.copyOfRange(0, ivLength)
        val cipherText = combined.copyOfRange(ivLength, combined.size)

        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(gcmTagLength, iv))
        return String(cipher.doFinal(cipherText))
    }
}
