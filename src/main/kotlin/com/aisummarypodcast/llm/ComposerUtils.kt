package com.aisummarypodcast.llm

import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.Podcast
import java.net.URI

fun resolveArticleContent(article: Article, useFullBody: Boolean): String =
    if (useFullBody) article.body else (article.summary ?: article.body)

fun shouldUseFullBody(articleCount: Int, podcast: Podcast, defaultThreshold: Int): Boolean =
    articleCount < (podcast.fullBodyThreshold ?: defaultThreshold)

fun extractDomain(url: String): String =
    try {
        URI(url).host?.removePrefix("www.") ?: url
    } catch (_: Exception) {
        url
    }

fun extractDomainAndPath(url: String): String =
    try {
        val uri = URI(url)
        val domain = uri.host?.removePrefix("www.") ?: return url
        val path = uri.path?.trimEnd('/') ?: ""
        if (path.isEmpty()) domain else "$domain$path"
    } catch (_: Exception) {
        url
    }
