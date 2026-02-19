package com.aisummarypodcast.scheduler

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.Source
import org.springframework.stereotype.Component
import java.net.URI

@Component
class PollDelayResolver(private val appProperties: AppProperties) {

    fun resolveDelaySeconds(source: Source): Int {
        source.pollDelaySeconds?.let { return it }

        val host = extractHost(source.url)
        if (host != null) {
            appProperties.source.hostOverrides[host]?.let { return it.pollDelaySeconds }
        }

        appProperties.source.pollDelaySeconds[source.type.value]?.let { return it }

        return 0
    }

    internal fun extractHost(url: String): String? =
        try {
            URI(url).host
        } catch (_: Exception) {
            null
        }
}
