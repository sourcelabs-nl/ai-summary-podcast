package com.aisummarypodcast.source

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component

@Component
class ContentExtractor {

    fun extract(document: Document): String {
        // Try <article> tag first
        val article = document.selectFirst("article")
        if (article != null) {
            return cleanText(article)
        }

        // Fall back to the largest text-containing block
        val candidates = document.select("div, section, main")
        val best = candidates.maxByOrNull { it.text().length }

        return if (best != null) cleanText(best) else document.body()?.text().orEmpty()
    }

    private fun cleanText(element: Element): String {
        // Remove navigation, headers, footers, sidebars, scripts, styles
        element.select("nav, header, footer, aside, script, style, .nav, .menu, .sidebar, .footer, .header").remove()
        return element.text().trim()
    }
}
