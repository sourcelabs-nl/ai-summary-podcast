package com.aisummarypodcast.store

import com.fasterxml.jackson.annotation.JsonValue

enum class PodcastStyle(@JsonValue val value: String) {
    NEWS_BRIEFING("news-briefing"),
    CASUAL("casual"),
    DEEP_DIVE("deep-dive"),
    EXECUTIVE_SUMMARY("executive-summary"),
    DIALOGUE("dialogue"),
    INTERVIEW("interview");

    companion object {
        fun fromValue(value: String): PodcastStyle? = entries.firstOrNull { it.value == value }
    }
}
