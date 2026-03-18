package com.aisummarypodcast.podcast

import com.aisummarypodcast.llm.EpisodeRecapGenerator
import com.aisummarypodcast.llm.ModelResolver
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.EpisodeArticleRepository
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.PodcastRepository
import com.aisummarypodcast.store.PostArticleRepository
import com.aisummarypodcast.tts.TtsPipeline
import io.mockk.mockk
import org.springframework.context.ApplicationEventPublisher
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
    private val episodeSourcesGenerator = mockk<EpisodeSourcesGenerator>()
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)

    private val articleEligibilityService = mockk<com.aisummarypodcast.llm.ArticleEligibilityService>()

    private val service = EpisodeService(
        episodeRepository, podcastRepository, ttsPipeline,
        episodeArticleRepository, articleRepository,
        episodeRecapGenerator, modelResolver, postArticleRepository,
        episodeSourcesGenerator, articleEligibilityService, eventPublisher
    )

    private val buildShowNotes: Method = EpisodeService::class.java
        .getDeclaredMethod("buildShowNotes", String::class.java)
        .also { it.isAccessible = true }

    private fun invoke(recap: String?): String? {
        return buildShowNotes.invoke(service, recap) as String?
    }

    @Test
    fun `show notes returns recap text`() {
        val result = invoke("Today's recap summary.")
        assertEquals("Today's recap summary.", result)
    }

    @Test
    fun `show notes returns null when recap is null`() {
        val result = invoke(null)
        assertNull(result)
    }
}
