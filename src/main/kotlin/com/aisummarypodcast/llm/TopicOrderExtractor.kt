package com.aisummarypodcast.llm

import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory

data class TopicOrderExtractionResult(
    val script: String,
    val topicOrder: List<String>
)

object TopicOrderExtractor {

    private val log = LoggerFactory.getLogger(javaClass)
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private const val START_DELIMITER = "|||TOPIC_ORDER|||"
    private const val END_DELIMITER = "|||END_TOPIC_ORDER|||"

    fun extract(rawResponse: String): TopicOrderExtractionResult {
        val startIndex = rawResponse.indexOf(START_DELIMITER)
        if (startIndex == -1) {
            return TopicOrderExtractionResult(rawResponse, emptyList())
        }

        val endIndex = rawResponse.indexOf(END_DELIMITER, startIndex)
        if (endIndex == -1) {
            return TopicOrderExtractionResult(rawResponse, emptyList())
        }

        val jsonContent = rawResponse.substring(startIndex + START_DELIMITER.length, endIndex).trim()
        val script = rawResponse.substring(0, startIndex).trimEnd()

        return try {
            val topicOrder: List<String> = objectMapper.readValue(jsonContent, objectMapper.typeFactory.constructCollectionType(List::class.java, String::class.java))
            TopicOrderExtractionResult(script, topicOrder)
        } catch (e: Exception) {
            log.warn("Failed to parse topic order JSON: {}", e.message)
            TopicOrderExtractionResult(script, emptyList())
        }
    }
}
