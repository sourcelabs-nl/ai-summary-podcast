package com.aisummarypodcast.util

import java.security.MessageDigest

fun sha256(text: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}
