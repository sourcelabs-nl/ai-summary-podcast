package com.aisummarypodcast.tts

object InworldScriptPostProcessor {

    private val SUPPORTED_TAGS = setOf("sigh", "laugh", "breathe", "cough", "clear_throat", "yawn")

    private val DOUBLE_ASTERISKS = Regex("\\*\\*(.+?)\\*\\*")
    private val MARKDOWN_HEADERS = Regex("(?m)^#{1,6}\\s+.*$")
    private val MARKDOWN_BULLETS = Regex("(?m)^[-*]\\s+")
    private val MARKDOWN_LINKS = Regex("\\[([^]]+)]\\([^)]+\\)")
    private val EMOJIS = Regex("[\\x{1F600}-\\x{1F64F}\\x{1F300}-\\x{1F5FF}\\x{1F680}-\\x{1F6FF}\\x{1F1E0}-\\x{1F1FF}\\x{2600}-\\x{27BF}\\x{2300}-\\x{23FF}\\x{2B50}\\x{2B55}\\x{FE0F}\\x{200D}\\x{20E3}\\x{E0020}-\\x{E007F}\\x{1F900}-\\x{1F9FF}\\x{1FA00}-\\x{1FA6F}\\x{1FA70}-\\x{1FAFF}]+")
    private val BRACKETED_TAGS = Regex("\\[(\\w+)]")

    fun process(script: String): String {
        var result = script

        // 1. Convert **word** → *word*
        result = DOUBLE_ASTERISKS.replace(result, "*$1*")

        // 2. Strip markdown headers
        result = MARKDOWN_HEADERS.replace(result, "")

        // 3. Strip markdown bullet prefixes (keep the text)
        result = MARKDOWN_BULLETS.replace(result, "")

        // 4. Convert markdown links [text](url) → text
        result = MARKDOWN_LINKS.replace(result, "$1")

        // 5. Strip emojis
        result = EMOJIS.replace(result, "")

        // 6. Whitelist non-verbal tags
        result = BRACKETED_TAGS.replace(result) { match ->
            val tag = match.groupValues[1]
            if (tag in SUPPORTED_TAGS) match.value else ""
        }

        // Clean up any resulting double spaces or blank lines
        result = result.replace(Regex(" {2,}"), " ")
        result = result.replace(Regex("\\n{3,}"), "\n\n")
        result = result.trim()

        return result
    }
}
