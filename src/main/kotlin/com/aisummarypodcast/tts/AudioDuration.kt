package com.aisummarypodcast.tts

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Path

@Component
class AudioDuration {

    private val log = LoggerFactory.getLogger(javaClass)

    fun calculate(filePath: Path): Int {
        val process = ProcessBuilder(
            "ffprobe", "-v", "error",
            "-show_entries", "format=duration",
            "-of", "default=noprint_wrappers=1:nokey=1",
            filePath.toAbsolutePath().toString()
        )
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText().trim()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            log.error("ffprobe failed for {}: {}", filePath, output)
            throw RuntimeException("ffprobe failed with exit code $exitCode")
        }

        val seconds = output.toDouble().toInt()
        log.info("Audio duration for {}: {} seconds", filePath.fileName, seconds)
        return seconds
    }
}
