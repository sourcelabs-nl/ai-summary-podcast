package com.aisummarypodcast.tts

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.Podcast
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Component
class TtsPipeline(
    private val ttsService: TtsService,
    private val audioConcatenator: AudioConcatenator,
    private val audioDuration: AudioDuration,
    private val episodeRepository: EpisodeRepository,
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun generate(script: String, podcast: Podcast): Episode {
        val chunks = TextChunker.chunk(script)
        log.info("Script split into {} chunks for podcast {}", chunks.size, podcast.id)

        val audioChunks = ttsService.generateAudio(chunks, podcast)

        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())
        val fileName = "briefing-$timestamp.mp3"
        val podcastDir = Path.of(appProperties.episodes.directory, podcast.id)
        Files.createDirectories(podcastDir)
        val outputPath = podcastDir.resolve(fileName)

        audioConcatenator.concatenate(audioChunks, outputPath)

        val duration = audioDuration.calculate(outputPath)

        val episode = episodeRepository.save(
            Episode(
                podcastId = podcast.id,
                generatedAt = Instant.now().toString(),
                scriptText = script,
                audioFilePath = outputPath.toString(),
                durationSeconds = duration
            )
        )

        log.info("Episode generated for podcast {}: {} ({} seconds)", podcast.id, fileName, duration)
        return episode
    }
}
