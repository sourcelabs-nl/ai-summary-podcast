package com.aisummarypodcast.source

import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.Source
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Component
class SourceAggregator {

    private val log = LoggerFactory.getLogger(javaClass)

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
        .withZone(ZoneOffset.UTC)

    fun aggregate(articles: List<Article>, source: Source): List<Article> {
        if (articles.size <= 1) return articles
        if (!shouldAggregate(source)) return articles

        log.info("[Aggregator] Aggregating {} articles from source {}", articles.size, source.id)
        return listOf(buildDigest(articles, source))
    }

    internal fun shouldAggregate(source: Source): Boolean {
        if (source.aggregate != null) return source.aggregate
        return source.type == "twitter" || source.url.contains("nitter.net")
    }

    private fun buildDigest(articles: List<Article>, source: Source): Article {
        val author = articles.firstNotNullOfOrNull { it.author }
        val titlePrefix = author ?: extractDomain(source.url)
        val mostRecentPublishedAt = articles.mapNotNull { it.publishedAt }.maxOrNull()
        val dateStr = mostRecentPublishedAt?.let {
            dateFormatter.format(Instant.parse(it))
        } ?: "Unknown date"

        val body = articles.joinToString("\n\n---\n\n") { article ->
            val timestamp = article.publishedAt ?: ""
            if (timestamp.isNotEmpty()) "$timestamp\n${article.body}" else article.body
        }

        return Article(
            sourceId = source.id,
            title = "Posts from $titlePrefix — $dateStr",
            body = body,
            url = source.url,
            publishedAt = mostRecentPublishedAt,
            author = author,
            contentHash = ""
        )
    }

    private fun extractDomain(url: String): String {
        return try {
            URI(url).host ?: url
        } catch (_: Exception) {
            url
        }
    }
}
