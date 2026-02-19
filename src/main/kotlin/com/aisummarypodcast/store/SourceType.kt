package com.aisummarypodcast.store

import com.fasterxml.jackson.annotation.JsonValue

enum class SourceType(@JsonValue val value: String) {
    RSS("rss"),
    WEBSITE("website"),
    TWITTER("twitter"),
    YOUTUBE("youtube");

    companion object {
        fun fromValue(value: String): SourceType? = entries.firstOrNull { it.value == value }
    }
}
