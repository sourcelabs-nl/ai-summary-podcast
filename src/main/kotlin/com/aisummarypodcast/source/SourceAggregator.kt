package com.aisummarypodcast.source

import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Post
import com.aisummarypodcast.store.PostArticle
import com.aisummarypodcast.store.PostArticleRepository
import com.aisummarypodcast.store.Source
import com.aisummarypodcast.store.SourceType
import com.aisummarypodcast.util.sha256
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.net.URI

@Component
class SourceAggregator(
    private val articleRepository: ArticleRepository,
    private val postArticleRepository: PostArticleRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun aggregateAndPersist(posts: List<Post>, source: Source): List<Article> {
        if (posts.isEmpty()) return emptyList()

        val articles = if (shouldAggregate(source) && posts.size > 1) {
            aggregatePosts(posts, source)
        } else {
            mapIndividualPosts(posts, source)
        }

        val savedArticles = articles.map { (article, threadPosts) ->
            val existing = articleRepository.findBySourceIdAndContentHash(source.id, article.contentHash)
            val saved = existing ?: articleRepository.save(article)
            saved to threadPosts
        }

        for ((article, threadPosts) in savedArticles) {
            for (post in threadPosts) {
                postArticleRepository.save(PostArticle(postId = post.id!!, articleId = article.id!!))
            }
        }

        val result = savedArticles.map { it.first }
        log.info("[Aggregator] Created {} articles from {} posts for source {}", result.size, posts.size, source.id)
        return result
    }

    internal fun shouldAggregate(source: Source): Boolean {
        if (source.aggregate != null) return source.aggregate
        return source.type == SourceType.TWITTER || source.url.contains("nitter.net")
    }

    internal fun groupPostsByThread(posts: List<Post>): List<List<Post>> {
        val sorted = posts.sortedBy { it.publishedAt ?: "" }
        val threads = mutableListOf<MutableList<Post>>()

        for (post in sorted) {
            if (isReply(post)) {
                if (threads.isNotEmpty()) {
                    threads.last().add(post)
                } else {
                    // Orphan reply: start a new thread
                    threads.add(mutableListOf(post))
                }
            } else {
                threads.add(mutableListOf(post))
            }
        }

        return threads
    }

    private fun isReply(post: Post): Boolean = post.title.startsWith("R to @")

    private fun aggregatePosts(posts: List<Post>, source: Source): List<Pair<Article, List<Post>>> {
        val threads = groupPostsByThread(posts)
        log.info("[Aggregator] Grouped {} posts into {} threads for source {}", posts.size, threads.size, source.id)

        return threads.map { threadPosts ->
            val parent = threadPosts.first()
            val body = threadPosts.joinToString("\n\n---\n\n") { post ->
                val timestamp = post.publishedAt ?: ""
                if (timestamp.isNotEmpty()) "$timestamp\n${post.body}" else post.body
            }

            val article = Article(
                sourceId = source.id,
                title = parent.title,
                body = body,
                url = rewriteNitterUrl(parent.url),
                publishedAt = parent.publishedAt,
                author = parent.author,
                contentHash = sha256(body)
            )

            article to threadPosts
        }
    }

    private fun mapIndividualPosts(posts: List<Post>, source: Source): List<Pair<Article, List<Post>>> {
        return posts.map { post ->
            val article = Article(
                sourceId = source.id,
                title = post.title,
                body = post.body,
                url = post.url,
                publishedAt = post.publishedAt,
                author = post.author,
                contentHash = sha256(post.body)
            )
            article to listOf(post)
        }
    }

    internal fun rewriteNitterUrl(url: String): String {
        return try {
            val uri = URI(url)
            if (uri.host?.equals("nitter.net", ignoreCase = true) == true) {
                URI(uri.scheme, uri.userInfo, "x.com", uri.port, uri.path, uri.query, uri.fragment).toString()
            } else {
                url
            }
        } catch (_: Exception) {
            url
        }
    }
}
