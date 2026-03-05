package com.aisummarypodcast.podcast

import com.aisummarypodcast.llm.EpisodeRecapGenerator
import com.aisummarypodcast.llm.ModelResolver
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.EpisodeArticle
import com.aisummarypodcast.store.EpisodeArticleRepository
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.PodcastRepository
import com.aisummarypodcast.store.PostArticleRepository
import com.aisummarypodcast.tts.TtsPipeline
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.lang.reflect.Method

class ShowNotesTest {

    private val episodeRepository = mockk<EpisodeRepository>()
    private val podcastRepository = mockk<PodcastRepository>()
    private val ttsPipeline = mockk<TtsPipeline>()
    private val episodeArticleRepository = mockk<EpisodeArticleRepository>()
    private val articleRepository = mockk<ArticleRepository>()
    private val episodeRecapGenerator = mockk<EpisodeRecapGenerator>()
    private val modelResolver = mockk<ModelResolver>()
    private val postArticleRepository = mockk<PostArticleRepository>()

    private val service = EpisodeService(
        episodeRepository, podcastRepository, ttsPipeline,
        episodeArticleRepository, articleRepository,
        episodeRecapGenerator, modelResolver, postArticleRepository
    )

    private val buildShowNotes: Method = EpisodeService::class.java
        .getDeclaredMethod("buildShowNotes", String::class.java, List::class.java)
        .also { it.isAccessible = true }

    private fun invoke(recap: String?, articles: List<Article>): String? {
        return buildShowNotes.invoke(service, recap, articles) as String?
    }

    private fun article(id: Long, title: String, url: String) = Article(
        id = id, sourceId = "s1", title = title, body = "body",
        url = url, contentHash = "hash$id"
    )

    @Test
    fun `show notes with recap and articles`() {
        val result = invoke(
            "Today's recap summary.",
            listOf(
                article(1, "Article One", "https://example.com/1"),
                article(2, "Article Two", "https://example.com/2"),
                article(3, "Article Three", "https://example.com/3")
            )
        )

        val expected = """
            Today's recap summary.

            Sources:
            - Article One
              https://example.com/1
            - Article Two
              https://example.com/2
            - Article Three
              https://example.com/3
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `show notes without recap`() {
        val result = invoke(
            null,
            listOf(article(1, "Article One", "https://example.com/1"))
        )

        val expected = """
            Sources:
            - Article One
              https://example.com/1
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `show notes with no articles`() {
        val result = invoke("Today's recap.", emptyList())
        assertEquals("Today's recap.", result)
    }

    @Test
    fun `show notes with no recap and no articles returns null`() {
        val result = invoke(null, emptyList())
        assertNull(result)
    }
}
