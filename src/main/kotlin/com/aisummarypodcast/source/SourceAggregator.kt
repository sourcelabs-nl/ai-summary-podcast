package com.aisummarypodcast.source

import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Post
import com.aisummarypodcast.store.PostArticle
import com.aisummarypodcast.store.PostArticleRepository
import com.aisummarypodcast.store.Source
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Component
class SourceAggregator(
    private val articleRepository: ArticleRepository,
    private val postArticleRepository: PostArticleRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
        .withZone(ZoneOffset.UTC)

    fun aggregateAndPersist(posts: List<Post>, source: Source): List<Article> {
        if (posts.isEmpty()) return emptyList()

        val articles = if (shouldAggregate(source) && posts.size > 1) {
            aggregatePosts(posts, source)
        } else {
            mapIndividualPosts(posts, source)
        }

        val savedArticles = articles.map { article ->
            val existing = articleRepository.findBySourceIdAndContentHash(source.id, article.contentHash)
            existing ?: articleRepository.save(article)
        }

        // Link posts to articles
        if (shouldAggregate(source) && posts.size > 1) {
            // All posts linked to the single aggregated article
            val article = savedArticles.first()
            for (post in posts) {
                postArticleRepository.save(PostArticle(postId = post.id!!, articleId = article.id!!))
            }
        } else {
            // 1:1 mapping
            for ((index, article) in savedArticles.withIndex()) {
                val post = posts[index]
                postArticleRepository.save(PostArticle(postId = post.id!!, articleId = article.id!!))
            }
        }

        log.info("[Aggregator] Created {} articles from {} posts for source {}", savedArticles.size, posts.size, source.id)
        return savedArticles
    }

    internal fun shouldAggregate(source: Source): Boolean {
        if (source.aggregate != null) return source.aggregate
        return source.type == "twitter" || source.url.contains("nitter.net")
    }

    private fun aggregatePosts(posts: List<Post>, source: Source): List<Article> {
        log.info("[Aggregator] Aggregating {} posts from source {}", posts.size, source.id)

        val author = posts.firstNotNullOfOrNull { it.author }
        val titlePrefix = author ?: extractDomain(source.url)
        val mostRecentPublishedAt = posts.mapNotNull { it.publishedAt }.maxOrNull()
        val dateStr = mostRecentPublishedAt?.let {
            dateFormatter.format(Instant.parse(it))
        } ?: "Unknown date"

        val body = posts.joinToString("\n\n---\n\n") { post ->
            val timestamp = post.publishedAt ?: ""
            if (timestamp.isNotEmpty()) "$timestamp\n${post.body}" else post.body
        }

        val article = Article(
            sourceId = source.id,
            title = "Posts from $titlePrefix — $dateStr",
            body = body,
            url = source.url,
            publishedAt = mostRecentPublishedAt,
            author = author,
            contentHash = sha256(body)
        )

        return listOf(article)
    }

    private fun mapIndividualPosts(posts: List<Post>, source: Source): List<Article> {
        return posts.map { post ->
            Article(
                sourceId = source.id,
                title = post.title,
                body = post.body,
                url = post.url,
                publishedAt = post.publishedAt,
                author = post.author,
                contentHash = sha256(post.body)
            )
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            URI(url).host ?: url
        } catch (_: Exception) {
            url
        }
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
