package com.aisummarypodcast.tts

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.llm.CostEstimator
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
        log.info("[TTS] Starting audio generation for podcast {}", podcast.id)
        val chunks = TextChunker.chunk(script)
        log.info("[TTS] Script split into {} chunks for podcast {}", chunks.size, podcast.id)

        val ttsResult = ttsService.generateAudio(chunks, podcast)
        val ttsCostCents = CostEstimator.estimateTtsCostCents(ttsResult.totalCharacters, appProperties.tts.costPerMillionChars)

        val (outputPath, duration) = generateAudioFile(ttsResult.audioChunks, podcast)

        val episode = episodeRepository.save(
            Episode(
                podcastId = podcast.id,
                generatedAt = Instant.now().toString(),
                scriptText = script,
                audioFilePath = outputPath.toString(),
                durationSeconds = duration,
                ttsCharacters = ttsResult.totalCharacters,
                ttsCostCents = ttsCostCents
            )
        )

        log.info("[TTS] Episode generated for podcast {}: {} ({} seconds)", podcast.id, outputPath.fileName, duration)
        return episode
    }

    fun generateForExistingEpisode(episode: Episode, podcast: Podcast): Episode {
        log.info("[TTS] Starting audio generation for episode {} (podcast {})", episode.id, podcast.id)
        val chunks = TextChunker.chunk(episode.scriptText)
        log.info("[TTS] Script split into {} chunks for episode {} (podcast {})", chunks.size, episode.id, podcast.id)

        val ttsResult = ttsService.generateAudio(chunks, podcast)
        val ttsCostCents = CostEstimator.estimateTtsCostCents(ttsResult.totalCharacters, appProperties.tts.costPerMillionChars)

        val (outputPath, duration) = generateAudioFile(ttsResult.audioChunks, podcast)

        val updated = episodeRepository.save(
            episode.copy(
                status = "GENERATED",
                audioFilePath = outputPath.toString(),
                durationSeconds = duration,
                ttsCharacters = ttsResult.totalCharacters,
                ttsCostCents = ttsCostCents
            )
        )

        log.info("[TTS] Episode {} audio generated for podcast {}: {} ({} seconds)", episode.id, podcast.id, outputPath.fileName, duration)
        return updated
    }

    private fun generateAudioFile(audioChunks: List<ByteArray>, podcast: Podcast): Pair<Path, Int> {
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())
        val fileName = "briefing-$timestamp.mp3"
        val podcastDir = Path.of(appProperties.episodes.directory, podcast.id)
        Files.createDirectories(podcastDir)
        val outputPath = podcastDir.resolve(fileName)

        audioConcatenator.concatenate(audioChunks, outputPath)

        val duration = audioDuration.calculate(outputPath)
        return Pair(outputPath, duration)
    }
}
