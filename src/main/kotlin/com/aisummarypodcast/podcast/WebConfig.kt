package com.aisummarypodcast.podcast

import com.aisummarypodcast.config.AppProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(private val appProperties: AppProperties) : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/episodes/**")
            .addResourceLocations("file:${appProperties.episodes.directory}/")
    }
}
