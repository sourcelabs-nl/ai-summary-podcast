package com.aisummarypodcast.tts

import org.slf4j.LoggerFactory

data class DialogueTurn(val role: String, val text: String)

object DialogueScriptParser {

    private val log = LoggerFactory.getLogger(javaClass)
    private val TAG_PATTERN = Regex("<(\\w+)>(.*?)</\\1>", RegexOption.DOT_MATCHES_ALL)

    fun parse(script: String): List<DialogueTurn> {
        if (script.isBlank()) return emptyList()

        val turns = mutableListOf<DialogueTurn>()
        var lastEnd = 0

        for (match in TAG_PATTERN.findAll(script)) {
            val outsideText = script.substring(lastEnd, match.range.first).trim()
            if (outsideText.isNotEmpty()) {
                log.warn("Text found outside speaker tags, ignoring: '{}'", outsideText.take(100))
            }

            val role = match.groupValues[1]
            val text = match.groupValues[2].trim()
            if (text.isNotEmpty()) {
                turns.add(DialogueTurn(role, text))
            }

            lastEnd = match.range.last + 1
        }

        val trailing = script.substring(lastEnd).trim()
        if (trailing.isNotEmpty()) {
            log.warn("Text found outside speaker tags, ignoring: '{}'", trailing.take(100))
        }

        return turns
    }
}
