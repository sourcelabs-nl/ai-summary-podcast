package com.aisummarypodcast.store

import com.fasterxml.jackson.annotation.JsonValue

enum class TtsProviderType(@JsonValue val value: String) {
    OPENAI("openai"),
    ELEVENLABS("elevenlabs");

    companion object {
        fun fromValue(value: String): TtsProviderType? = entries.firstOrNull { it.value == value }
    }
}
